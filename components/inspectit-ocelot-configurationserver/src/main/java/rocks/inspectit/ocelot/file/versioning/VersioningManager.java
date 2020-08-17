package rocks.inspectit.ocelot.file.versioning;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.error.exceptions.SelfPromotionNotAllowedException;
import rocks.inspectit.ocelot.events.ConfigurationPromotionEvent;
import rocks.inspectit.ocelot.events.WorkspaceChangedEvent;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.git.CachingRevisionAccess;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.versioning.model.ConfigurationPromotion;
import rocks.inspectit.ocelot.file.versioning.model.SimpleDiffEntry;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceVersion;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This manager handles the interaction with the versioned (Git) representation of the working directory.
 * When using this class, ensure to lock the working directory or resource accordingly to prevent any racing conditions.
 */
@Slf4j
public class VersioningManager {

    /**
     * Git user used for system commits.
     */
    @VisibleForTesting
    static final PersonIdent GIT_SYSTEM_AUTHOR = new PersonIdent("System", "info@inspectit.rocks");

    /**
     * The mail suffix used to generate mail addresses for internal users.
     */
    private String mailSuffix;

    /**
     * Path of the current working directory.
     */
    private Path workingDirectory;

    /**
     * Git repository of the working directory.
     */
    private Git git;

    /**
     * Supplier for accessing the currently logged in user.
     */
    private Supplier<Authentication> authenticationSupplier;

    /**
     * Used for sending application events.
     */
    private ApplicationEventPublisher eventPublisher;

    /**
     * Timeout for amend commits. Consecutive commits of the same user within this time will be amended.
     */
    @Setter
    private long amendTimeout = Duration.ofMinutes(10).toMillis();

    /**
     * Constructor.
     *
     * @param workingDirectory       the working directory to use
     * @param authenticationSupplier the supplier to user for accessing the current user
     * @param eventPublisher         the event publisher to use
     * @param mailSuffix             The mail suffix used to generate mail addresses for internal users.
     */
    public VersioningManager(Path workingDirectory, Supplier<Authentication> authenticationSupplier, ApplicationEventPublisher eventPublisher, String mailSuffix) {
        this.workingDirectory = workingDirectory;
        this.authenticationSupplier = authenticationSupplier;
        this.eventPublisher = eventPublisher;
        this.mailSuffix = mailSuffix;
    }

    /**
     * Initializes the versioning manager. This method open the Git repository of the working directory. In case the
     * Git directory does not exist, it will be created. Modified files will be automatically commited to the
     * workspace branch.
     */
    public synchronized void initialize() throws GitAPIException {
        boolean hasGit = isGitRepository();

        git = Git.init().setDirectory(workingDirectory.toFile()).call();

        if (!hasGit) {
            log.info("Working directory is not managed by Git. Initializing Git repository and staging and committing all existing file.");

            stageFiles();
            commitFiles(GIT_SYSTEM_AUTHOR, "Initializing Git repository using existing working directory", false);

            if (getCommitCount() <= 0) {
                // creating an empty commit
                git.commit()
                        .setAllowEmpty(true)
                        .setAuthor(GIT_SYSTEM_AUTHOR)
                        .setMessage("Initializing Git repository")
                        .call();
            }

            // create the branches which will be used
            git.branchRename().setNewName(Branch.WORKSPACE.getBranchName()).call();
            git.branchCreate().setName(Branch.LIVE.getBranchName()).call();
        } else if (!isClean()) {
            log.info("Changes in the configuration or agent mapping files have been detected and will be committed to the repository.");

            stageFiles();
            commitFiles(GIT_SYSTEM_AUTHOR, "Staging and committing of external changes during startup", false);
        }
    }

    /**
     * Closes the {@link #git} instance of this manager.
     */
    @VisibleForTesting
    void destroy() {
        git.close();
    }

    /**
     * Commits all currently modified files and labels the commit as an external change. This method should be used to
     * show that files were not processed by the configuration server, but from external sources.
     */
    public synchronized void commitAsExternalChange() throws GitAPIException {
        if (isClean()) {
            return;
        }
        log.info("Staging and committing of external changes to the configuration files or agent mappings");

        stageFiles();
        commitFiles(GIT_SYSTEM_AUTHOR, "Staging and committing of external changes", false);
    }

    private boolean isWorkspaceBranch() {
        try {
            String fullBranch = git.getRepository().getFullBranch();
            String workspaceRef = "refs/heads/" + Branch.WORKSPACE.getBranchName();
            return workspaceRef.equals(fullBranch);
        } catch (IOException e) {
            throw new RuntimeException("Exception while accessing Git workspace repository.", e);
        }
    }

    /**
     * Committing all currently modified files using the given commit message. The author of the commit is provided
     * by the {@link #authenticationSupplier}. Commits will be amended in case the previous one is made by the same
     * user and is newer than {@link #amendTimeout} milliseconds.
     * The commit will be against the workspace branch!
     *
     * @param message the commit message to use
     *
     * @throws IllegalStateException in case the workspace branch is not the currently checked out branch
     */
    public void commitAllChanges(String message) throws GitAPIException {
        if (!isWorkspaceBranch()) {
            throw new IllegalStateException("The workspace branch is currently not checked out. Ensure your working directory is in a correct state!");
        }

        PersonIdent author = getCurrentAuthor();

        stageFiles();

        if (commitFiles(author, message, true)) {
            eventPublisher.publishEvent(new WorkspaceChangedEvent(this, getWorkspaceRevision()));
        }
    }

    /**
     * Commits the staged files using the given author and message. Consecutive of the same user within {@link #amendTimeout}
     * milliseconds will be amended if specified.
     *
     * @param author     the author to use
     * @param message    the commit message to use
     * @param allowAmend whether commits should be amended if possible (same user and below timeout)
     *
     * @return true, if a commit was created. False, if there was no change to commit.
     */
    private boolean commitFiles(PersonIdent author, String message, boolean allowAmend) throws GitAPIException {
        if (isClean()) {
            log.debug("Repository is in clean state, thus, committing will be skipped.");
            return false;
        } else {
            log.debug("Committing staged changes.");
        }

        CommitCommand commitCommand = git.commit().setAll(true) // in order to remove deleted files from index
                .setMessage(message).setAuthor(author);

        Optional<RevCommit> latestCommit = getLatestCommit(Branch.WORKSPACE);
        if (allowAmend && latestCommit.isPresent()) {
            PersonIdent latestAuthor = latestCommit.get().getAuthorIdent();
            boolean sameAuthor = latestAuthor != null && author.getName().equals(latestAuthor.getName());

            long latestCommitTime = latestCommit.get().getCommitTime() * 1000L;
            boolean expired = latestCommitTime + amendTimeout < System.currentTimeMillis();

            if (sameAuthor && !expired) {
                log.debug("|-Executing an amend commit.");
                commitCommand.setAmend(true);
            }
        }

        commitCommand.call();
        return true;
    }

    /**
     * Stage all modified/added/removed configuration files and the agent mapping file.
     */
    private void stageFiles() throws GitAPIException {
        log.debug("Staging all configuration files and agent mappings.");

        git.add()
                .addFilepattern(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER)
                .addFilepattern(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME)
                .call();
    }

    /**
     * @return Returns whether the working directory is in a clean stage. A clean state means that no configuration file
     * nor the agent mappings have been modified.
     */
    public boolean isClean() throws GitAPIException {
        Status status = git.status()
                .addPath(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER)
                .addPath(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME)
                .call();

        return status.isClean();
    }

    /**
     * @return Returns the amount of commits of the currently checked out branch.
     */
    public int getCommitCount() {
        try {
            Iterable<RevCommit> commits = git.log().call();
            return Iterables.size(commits);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * @return Returns <code>true</code> if the working directory is managed by Git.
     */
    private boolean isGitRepository() {
        try (Git ignored = Git.open(workingDirectory.toFile())) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * @return Returns the Git author of the currently logged in user.
     */
    @VisibleForTesting
    PersonIdent getCurrentAuthor() {
        Authentication authentication = authenticationSupplier.get();
        if (authentication != null) {
            String username = authentication.getName();
            String mail;
            if (authentication.getPrincipal() instanceof InetOrgPerson) {
                mail = ((InetOrgPerson) authentication.getPrincipal()).getMail();
            } else {
                mail = username + mailSuffix;
            }
            return new PersonIdent(username, mail);
        } else {
            return GIT_SYSTEM_AUTHOR;
        }
    }

    /**
     * @return Returns the latest commit of the specified branch.
     */
    public Optional<RevCommit> getLatestCommit(Branch targetBranch) {
        try {
            String ref = "refs/heads/" + targetBranch.getBranchName();
            ObjectId commitId = git.getRepository().resolve(ref);
            RevCommit commit = getCommit(commitId);
            return Optional.ofNullable(commit);
        } catch (IOException e) {
            log.error("An exception occurred while trying to load the latest commit.", e);
            return Optional.empty();
        }
    }

    /**
     * Returns the commit with the given id. In case the commit does not exist, the id is wrong or any other kind
     * of error, <code>null</code> will be returned.
     *
     * @param commitId the id of the desired commit
     *
     * @return the commit object or null if the commit could not be loaded
     */
    private RevCommit getCommit(ObjectId commitId) {
        try {
            if (commitId == null) {
                return null;
            }

            try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                return revWalk.parseCommit(commitId);
            }
        } catch (IOException e) {
            log.error("An exception occurred while trying to load the latest commit.", e);
            return null;
        }
    }

    /**
     * @return A {@link CachingRevisionAccess} instance to access the current live branch.
     */
    public CachingRevisionAccess getLiveRevision() {
        Optional<RevCommit> latestCommit = getLatestCommit(Branch.LIVE);
        return latestCommit.map(revCommit -> new CachingRevisionAccess(git.getRepository(), revCommit)).orElse(null);
    }

    /**
     * @return @return A {@link CachingRevisionAccess} instance to access the current workspace branch.
     */
    public CachingRevisionAccess getWorkspaceRevision() {
        Optional<RevCommit> latestCommit = getLatestCommit(Branch.WORKSPACE);
        return latestCommit.map(revCommit -> new CachingRevisionAccess(git.getRepository(), revCommit)).orElse(null);
    }

    /**
     * @return @return A {@link CachingRevisionAccess} instance to access the commit with the specified Id.
     */
    public CachingRevisionAccess getRevisionById(ObjectId commitId) {
        RevCommit commit = getCommit(commitId);
        if (commit != null) {
            return new CachingRevisionAccess(git.getRepository(), commit);
        }
        return null;
    }

    /**
     * @return Returns the diff between the current live branch and the current workspace branch. The actual file
     * difference (old and new content) will not be returned.
     */
    public WorkspaceDiff getWorkspaceDiffWithoutContent() throws IOException, GitAPIException {
        return getWorkspaceDiff(false);
    }

    /**
     * Returns the diff between the current live branch and the current workspace branch.
     *
     * @param includeFileContent whether the file difference (old and new content) is included
     *
     * @return the diff between the live and workspace branch
     */
    public WorkspaceDiff getWorkspaceDiff(boolean includeFileContent) throws IOException, GitAPIException {
        Repository repository = git.getRepository();
        ObjectId oldCommitId = repository.exactRef("refs/heads/" + Branch.LIVE.getBranchName()).getObjectId();
        ObjectId newCommitId = repository.exactRef("refs/heads/" + Branch.WORKSPACE.getBranchName()).getObjectId();

        return getWorkspaceDiff(includeFileContent, oldCommitId, newCommitId);
    }

    /**
     * Returns the diff between the specified commits. In case the `includeFileContent` argument is true, the actual
     * file difference (old and new content) will be included in the resulting object.
     * <p>
     * This method is based on the following implementation: https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowBranchDiff.java
     *
     * @param includeFileContent whether the file difference (old and new content) is included
     * @param oldCommit          the commit id of the base (old) commit
     * @param newCommit          the commit id of the target (new) commit
     *
     * @return the diff between the specified branches
     */
    @VisibleForTesting
    WorkspaceDiff getWorkspaceDiff(boolean includeFileContent, ObjectId oldCommit, ObjectId newCommit) throws IOException, GitAPIException {
        // the diff works on TreeIterators, we prepare two for the two branches
        AbstractTreeIterator oldTree = prepareTreeParser(oldCommit);
        AbstractTreeIterator newTree = prepareTreeParser(newCommit);

        // then the procelain diff-command returns a list of diff entries
        List<DiffEntry> diffEntries = git.diff().setOldTree(oldTree).setNewTree(newTree).call();

        // the diff entries are converted into custom ones
        List<SimpleDiffEntry> simpleDiffEntries = diffEntries.stream()
                .map(SimpleDiffEntry::of)
                .filter(entry -> entry.getFile().startsWith(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER))
                .peek(entry -> {
                    String shortenFile = entry.getFile()
                            .substring(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER.length());
                    entry.setFile(shortenFile);
                })
                .collect(Collectors.toList());

        // the diff entries will get their file difference if specified
        if (includeFileContent) {
            RevisionAccess liveRevision = getRevisionById(oldCommit);
            RevisionAccess workspaceRevision = getRevisionById(newCommit);

            simpleDiffEntries.forEach(entry -> fillFileContent(entry, liveRevision, workspaceRevision));
        }
        simpleDiffEntries.forEach(entry -> fillInAuthors(entry, oldCommit, newCommit));

        return WorkspaceDiff.builder()
                .entries(simpleDiffEntries)
                .liveCommitId(oldCommit.name())
                .workspaceCommitId(newCommit.name())
                .build();
    }

    @VisibleForTesting
    void fillInAuthors(SimpleDiffEntry entry, ObjectId baseCommitId, ObjectId newCommitId) {
        RevCommit baseCommit = getCommit(baseCommitId);
        RevCommit newCommit = getCommit(newCommitId);
        switch (entry.getType()) {
            case ADD:
                entry.setAuthors(new ArrayList<>(findAuthorsSinceAddition(entry.getFile(), newCommit)));
                break;
            case MODIFY:
                entry.setAuthors(new ArrayList<>(findModifyingAuthors(entry.getFile(), baseCommit, newCommit)));
                break;
            case DELETE:
                entry.setAuthors(Collections.singletonList(findDeletingAuthor(entry.getFile(), baseCommit, newCommit)));
                break;
            default:
                log.warn("Unsupported change type for author lookup encountered: {}", entry.getType());
                break;
        }
    }

    /**
     * Finds all authors who have modified a file since a certain base revision.
     *
     * @param file       the name of the file to check.
     * @param baseCommit A commit on the live branch onto which the newCommit will be merged
     * @param newCommit  A commit on the workspace branch containing file modifications
     *
     * @return A list of authors who have modified the file.
     */
    private Collection<String> findModifyingAuthors(String file, RevCommit baseCommit, RevCommit newCommit) {
        RevisionAccess newRevision = new RevisionAccess(git.getRepository(), newCommit);
        RevisionAccess baseRevision = new RevisionAccess(git.getRepository(), baseCommit);
        //move "baseRevision" to the last commit where this file was touched (potentially the root commit).
        baseRevision = findLastChangingRevision(file, baseRevision);

        Set<String> authors = new HashSet<>();
        String baseContent = baseRevision.readConfigurationFile(file).get();
        //Find all persons who added or modified the file since the last promotion.
        RevisionAccess commonAncestor = newRevision.getCommonAncestor(baseRevision);
        while (!newRevision.getRevisionId().equals(commonAncestor.getRevisionId())) {
            if (newRevision.isConfigurationFileModified(file)) {
                authors.add(newRevision.getAuthorName());
            } else if (newRevision.isConfigurationFileAdded(file)) {
                authors.add(newRevision.getAuthorName());
                break; //THe file has been added, no need to take previous changes into account
            }
            newRevision = newRevision.getPreviousRevision()
                    .orElseThrow(() -> new IllegalStateException("Expected parent to exist"));
            if (newRevision.configurationFileExists(file) && newRevision.readConfigurationFile(file)
                    .get()
                    .equals(baseContent)) {
                break; // we have reached a revision where the content is in the original state, no need to look further
            }
        }
        return authors;
    }

    /**
     * Walks back in history to the point where the given file was added.
     * On the way, all authors which have modifies the file are remembered.
     *
     * @param file      the file to check
     * @param newCommit the commit to start looking from, usually on the workspace
     *
     * @return the list of authors who have modified the file since it's addition including the author adding the file
     */
    private Collection<String> findAuthorsSinceAddition(String file, RevCommit newCommit) {
        RevisionAccess newRevision = new RevisionAccess(git.getRepository(), newCommit);
        Set<String> authors = new HashSet<>();
        //Find all persons who edited the file since it was added
        while (!newRevision.isConfigurationFileAdded(file)) {
            if (newRevision.isConfigurationFileModified(file)) {
                authors.add(newRevision.getAuthorName());
            }
            newRevision = newRevision.getPreviousRevision()
                    .orElseThrow(() -> new IllegalStateException("Expected parent to exist"));
        }
        authors.add(newRevision.getAuthorName()); //Also add the name of the person who added the file
        return authors;
    }

    /**
     * Finds the most recent revision originating from "newCommit" in which the given file was deleted.
     * Does not walk past the common ancestor of "newCommit" and "baseCommit".
     * <p>
     * Returns the author of this revision.
     *
     * @param file       the file to check
     * @param baseCommit the commit to comapre agains, usually the live branch
     * @param newCommit  the commit in which the provided file does not exist anymore, usually on the workspace
     *
     * @return the author of the revision which is responsible for the deletion.
     */
    private String findDeletingAuthor(String file, RevCommit baseCommit, RevCommit newCommit) {
        RevisionAccess newRevision = new RevisionAccess(git.getRepository(), newCommit);
        RevisionAccess baseRevision = new RevisionAccess(git.getRepository(), baseCommit);
        //move "baseRevision" to the last commit where this file was touched (potentially the root commit).
        baseRevision = findLastChangingRevision(file, baseRevision);

        RevisionAccess commonAncestor = baseRevision.getCommonAncestor(newRevision);
        RevisionAccess previous = commonAncestor;
        //find the person who deleted the file most recently
        while (!newRevision.getRevisionId().equals(commonAncestor.getRevisionId())) {
            if (newRevision.isConfigurationFileDeleted(file)) {
                return newRevision.getAuthorName();
            }
            previous = newRevision;
            newRevision = newRevision.getPreviousRevision()
                    .orElseThrow(() -> new IllegalStateException("Expected parent to exist"));
        }
        return previous.getAuthorName(); //in case an amend happened, this will be the correct user
    }

    /**
     * Walks backwards in history starting at the given revision.
     * Stops at the first revision which either adds or modifies the given file.
     * <p>
     * If the provided revision already modified/adds the given file, it is returned unchanged.
     *
     * @param file         the file to look for
     * @param baseRevision the starting revision to walk backwards from
     *
     * @return a revision which either modifies or adds the given file.
     */
    private RevisionAccess findLastChangingRevision(String file, RevisionAccess baseRevision) {
        while (!baseRevision.isConfigurationFileAdded(file) && !baseRevision.isConfigurationFileModified(file)) {
            baseRevision = baseRevision.getPreviousRevision()
                    .orElseThrow(() -> new IllegalStateException("Expected parent to exist"));
        }
        return baseRevision;
    }

    /**
     * Sets the old and new file content of the specified diff entry using the given revision access instances.
     *
     * @param entry       the entry to fill its content
     * @param oldRevision the revision access representing the old state of the file
     * @param newRevision the revision access representing the new state of the file
     */
    private void fillFileContent(SimpleDiffEntry entry, RevisionAccess oldRevision, RevisionAccess newRevision) {
        String file = entry.getFile();
        String oldContent = null;
        String newContent = null;

        if (entry.getType() == DiffEntry.ChangeType.ADD) {
            newContent = newRevision.readConfigurationFile(file).orElse(null);
        } else if (entry.getType() == DiffEntry.ChangeType.MODIFY) {
            oldContent = oldRevision.readConfigurationFile(file).orElse(null);
            newContent = newRevision.readConfigurationFile(file).orElse(null);
        } else if (entry.getType() == DiffEntry.ChangeType.DELETE) {
            oldContent = oldRevision.readConfigurationFile(file).orElse(null);
        }

        entry.setOldContent(oldContent);
        entry.setNewContent(newContent);
    }

    /**
     * Creates an {@link AbstractTreeIterator} for the specified commit.
     *
     * @param commitId the commit which is used as basis for the tree iterator
     *
     * @return the created {@link AbstractTreeIterator}
     */
    private AbstractTreeIterator prepareTreeParser(ObjectId commitId) throws IOException {
        Repository repository = git.getRepository();
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(commitId);
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }

    /**
     * Promoting the configuration files according to the specified {@link ConfigurationPromotion} definition.
     *
     * @param promotion the promotion definition
     */
    public void promoteConfiguration(ConfigurationPromotion promotion, boolean allowSelfPromotion) throws GitAPIException {
        if (promotion == null || CollectionUtils.isEmpty(promotion.getFiles())) {
            throw new IllegalArgumentException("ConfigurationPromotion must not be null and has to promote at least one file!");
        }

        try {
            ObjectId liveCommitId = ObjectId.fromString(promotion.getLiveCommitId());
            ObjectId workspaceCommitId = ObjectId.fromString(promotion.getWorkspaceCommitId());

            ObjectId currentLiveBranchId = git.getRepository()
                    .exactRef("refs/heads/" + Branch.LIVE.getBranchName())
                    .getObjectId();

            if (!liveCommitId.equals(currentLiveBranchId)) {
                throw new ConcurrentModificationException("Live branch has been modified. The provided promotion definition is out of sync.");
            }

            // get modified files between the specified diff - we only consider files which exists in the diff
            WorkspaceDiff diff = getWorkspaceDiff(false, liveCommitId, workspaceCommitId);

            if (!allowSelfPromotion && containsSelfPromotion(promotion, diff)) {
                throw new SelfPromotionNotAllowedException("The promotion request contains a file which was edited by the same user");
            }

            Map<String, DiffEntry.ChangeType> changeIndex = diff.getEntries()
                    .stream()
                    .collect(Collectors.toMap(SimpleDiffEntry::getFile, SimpleDiffEntry::getType));

            List<String> removeFiles = promotion.getFiles()
                    .stream()
                    .filter(file -> changeIndex.get(file) == DiffEntry.ChangeType.DELETE)
                    .map(this::prefixRelativeFile)
                    .collect(Collectors.toList());

            List<String> checkoutFiles = promotion.getFiles()
                    .stream()
                    .filter(file -> changeIndex.get(file) != DiffEntry.ChangeType.DELETE)
                    .map(this::prefixRelativeFile)
                    .collect(Collectors.toList());

            // checkout live branch
            git.checkout().setName(Branch.LIVE.getBranchName()).call();

            // create an empty merge-commit
            git.merge()
                    .include(workspaceCommitId)
                    .setCommit(false)
                    .setFastForward(MergeCommand.FastForwardMode.NO_FF)
                    .setStrategy(MergeStrategy.OURS)
                    .call();

            // remove all deleted files
            if (!removeFiles.isEmpty()) {
                RmCommand rmCommand = git.rm();
                removeFiles.forEach(rmCommand::addFilepattern);
                rmCommand.call();
            }
            // checkout added and modified files
            if (!checkoutFiles.isEmpty()) {
                CheckoutCommand checkoutCommand = git.checkout().setStartPoint(promotion.getWorkspaceCommitId());
                checkoutFiles.forEach(checkoutCommand::addPath);
                checkoutCommand.call();
            }

            // commit changes
            commitFiles(getCurrentAuthor(), promotion.getCommitMessage(), false);

        } catch (IOException | GitAPIException ex) {
            throw new PromotionFailedException("Configuration promotion has failed.", ex);
        } finally {
            // checkout workspace branch
            git.checkout().setName(Branch.WORKSPACE.getBranchName()).call();

            eventPublisher.publishEvent(new ConfigurationPromotionEvent(this, getLiveRevision()));
        }
    }

    private boolean containsSelfPromotion(ConfigurationPromotion promotion, WorkspaceDiff diff) {
        PersonIdent currentAuthor = getCurrentAuthor();
        if (currentAuthor == GIT_SYSTEM_AUTHOR) {
            return false;
        }
        Set<String> promotedFiles = promotion.getFiles()
                .stream()
                .map(this::prefixRelativeFile) //use prefixRelativeFile to normalize the file names
                .collect(Collectors.toSet());
        return diff.getEntries()
                .stream()
                .filter(entry -> promotedFiles.contains(prefixRelativeFile(entry.getFile())))
                .anyMatch(entry -> entry.getAuthors().contains(currentAuthor.getName()));
    }

    /**
     * Prefixes the given file path using the configuration's file subfolder.
     * <p>
     * Example, the input `/my_file.yml` will result in `files/my_file.yml` if `files` is the files subfolder.
     *
     * @param file the relative file path
     *
     * @return the file path including the files directory
     */
    private String prefixRelativeFile(String file) {
        if (file.startsWith("/")) {
            return AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + file;
        } else {
            return AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/" + file;
        }
    }

    /**
     * @return returning a list of {@link WorkspaceVersion} existing in the workspace branch.
     */
    public List<WorkspaceVersion> listWorkspaceVersions() throws IOException, GitAPIException {
        ObjectId branch = git.getRepository().resolve("refs/heads/" + Branch.WORKSPACE.getBranchName());

        Iterable<RevCommit> workspaceCommits = git.log().add(branch).call();

        return StreamSupport.stream(workspaceCommits.spliterator(), false)
                .map(WorkspaceVersion::of)
                .collect(Collectors.toList());
    }
}
