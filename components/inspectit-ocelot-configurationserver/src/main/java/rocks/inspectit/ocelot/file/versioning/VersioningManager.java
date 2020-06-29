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
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.events.ConfigurationPromotionEvent;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.versioning.model.ConfigurationPromotion;
import rocks.inspectit.ocelot.file.versioning.model.SimpleDiffEntry;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This manager handles the interaction with the versioned (Git) representation of the working directory.
 */
@Slf4j
public class VersioningManager {

    /**
     * Git user used for system commits.
     */
    private static final PersonIdent GIT_SYSTEM_AUTHOR = new PersonIdent("System", "info@inspectit.rocks");

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
     */
    public VersioningManager(Path workingDirectory, Supplier<Authentication> authenticationSupplier, ApplicationEventPublisher eventPublisher) {
        this.workingDirectory = workingDirectory;
        this.authenticationSupplier = authenticationSupplier;
        this.eventPublisher = eventPublisher;
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

    /**
     * Committing all currently modified files using the given commit message. The author of the commit is provided
     * by the {@link #authenticationSupplier}. Commits will be amended in case the previous one is made by the same
     * user and is newer than {@link #amendTimeout} milliseconds.
     *
     * @param message the commit message to use
     */
    public synchronized void commitAllChanges(String message) throws GitAPIException {
        PersonIdent author = getCurrentAuthor();

        stageFiles();

        commitFiles(author, message, true);
    }

//    /**
//     * Committing all currently modified files using the given commit message and the given author.
//     *
//     * @param author     the author to use
//     * @param message    the commit message to use
//     * @param allowAmend whether commits should be amended if possible (same user and below timeout)
//     */
//    private void commit(PersonIdent author, String message, boolean allowAmend) throws GitAPIException {
//        if (isClean()) {
//            return;
//        }
//
//        commitFiles(author, message, allowAmend);
//    }

    /**
     * Commits the staged files using the given author and message. Consecutive of the same user within {@link #amendTimeout}
     * milliseconds will be amended if specified. When used, ensure to lock the working directory, otherwise it may be
     * that you' re committing to the wrong branch.
     *
     * @param author     the author to use
     * @param message    the commit message to use
     * @param allowAmend whether commits should be amended if possible (same user and below timeout)
     */
    private void commitFiles(PersonIdent author, String message, boolean allowAmend) throws GitAPIException {
        if (isClean()) {
            log.info("Repository is in clean state, thus, committing will be skipped.");
            return;
        } else {
            log.info("Committing staged changes.");
        }

        CommitCommand commitCommand = git.commit()
                .setAll(true) // in order to remove deleted files from index
                .setMessage(message)
                .setAuthor(author);

        Optional<RevCommit> latestCommit = getLatestCommit();
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
    }

    /**
     * Stage all modified/added/removed configuration files and the agent mapping file.
     */
    private void stageFiles() throws GitAPIException {
        log.info("Staging all configuration files and agent mappings.");

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
    private PersonIdent getCurrentAuthor() {
        Authentication authentication = authenticationSupplier.get();
        String username = authentication.getName();

        return new PersonIdent(username, "info@inspectit.rocks");
    }

    /**
     * @return Returns the latest commit of the workspace branch.
     */
    public Optional<RevCommit> getLatestCommit() {
        return getLatestCommit(Branch.WORKSPACE);
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
     * @return A {@link RevisionAccess} instance to access the current live branch.
     */
    public RevisionAccess getLiveRevision() {
        Optional<RevCommit> latestCommit = getLatestCommit(Branch.LIVE);
        return latestCommit.map(revCommit -> new RevisionAccess(git.getRepository(), revCommit)).orElse(null);
    }

    /**
     * @return @return A {@link RevisionAccess} instance to access the current workspace branch.
     */
    public RevisionAccess getWorkspaceRevision() {
        Optional<RevCommit> latestCommit = getLatestCommit(Branch.WORKSPACE);
        return latestCommit.map(revCommit -> new RevisionAccess(git.getRepository(), revCommit)).orElse(null);
    }

    /**
     * @return @return A {@link RevisionAccess} instance to access the commit with the specified Id.
     */
    public RevisionAccess getRevisionById(ObjectId commitId) {
        RevCommit commit = getCommit(commitId);
        if (commit != null) {
            return new RevisionAccess(git.getRepository(), commit);
        }
        return null;
    }

    /**
     * @return Returns the diff between the current live branch and the current workspace branch. The actual file
     * difference (old and new content) will not be returned.
     */
    public WorkspaceDiff getWorkspaceDiff() throws IOException, GitAPIException {
        return getWorkspaceDiff(false);
    }

    /**
     * Returns the diff between the current live branch and the current workspace branch.
     *
     * @param includeFileContent whether the file difference (old and new content) is included
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
                    String shortenFile = entry.getFile().substring(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER.length());
                    entry.setFile(shortenFile);
                })
                .collect(Collectors.toList());

        // the diff entries will get their file difference if specified
        if (includeFileContent) {
            RevisionAccess liveRevision = getRevisionById(oldCommit);
            RevisionAccess workspaceRevision = getRevisionById(newCommit);

            simpleDiffEntries.forEach(entry -> fillFileContent(entry, liveRevision, workspaceRevision));
        }

        return WorkspaceDiff.builder()
                .diffEntries(simpleDiffEntries)
                .liveCommitId(oldCommit.name())
                .workspaceCommitId(newCommit.name())
                .build();
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
    public void promoteConfiguration(ConfigurationPromotion promotion) throws GitAPIException {
        if (promotion == null || CollectionUtils.isEmpty(promotion.getFiles())) {
            throw new IllegalArgumentException("ConfigurationPromotion must not be null and has to promote at least one file!");
        }

        try {
            ObjectId liveCommitId = ObjectId.fromString(promotion.getLiveCommitId());
            ObjectId workspaceCommitId = ObjectId.fromString(promotion.getWorkspaceCommitId());

            ObjectId currentLiveBranchId = git.getRepository().exactRef("refs/heads/" + Branch.LIVE.getBranchName()).getObjectId();

            if (!liveCommitId.equals(currentLiveBranchId)) {
                throw new ConcurrentModificationException("Live branch has been modified. The provided promotion definition is out of sync.");
            }

            // get modified files between the specified diff - we only consider files which exists in the diff
            WorkspaceDiff diff = getWorkspaceDiff(false, liveCommitId, workspaceCommitId);
            Map<String, DiffEntry.ChangeType> changeIndex = diff.getDiffEntries().stream()
                    .collect(Collectors.toMap(SimpleDiffEntry::getFile, SimpleDiffEntry::getType));

            List<String> removeFiles = promotion.getFiles().stream()
                    .filter(file -> changeIndex.get(file) == DiffEntry.ChangeType.DELETE)
                    .map(this::prefixRelativeFile)
                    .collect(Collectors.toList());

            List<String> checkoutFiles = promotion.getFiles().stream()
                    .filter(file -> changeIndex.get(file) != DiffEntry.ChangeType.DELETE)
                    .map(this::prefixRelativeFile)
                    .collect(Collectors.toList());

            // checkout live branch
            git.checkout().setName(Branch.LIVE.getBranchName()).call();

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
            commitFiles(getCurrentAuthor(), "Promoting configuration files", false);

        } catch (IOException | GitAPIException ex) {
            throw new PromotionFailedException("Configuration promotion has failed.", ex);
        } finally {
            // checkout workspace branch
            git.checkout().setName(Branch.WORKSPACE.getBranchName()).call();

            eventPublisher.publishEvent(new ConfigurationPromotionEvent(this));

            //TODO should we hard reset the repository in case an error occurs, thus we're in a clean state?
        }
    }

    /**
     * Prefixes the given file path using the configuration's file subfolder.
     * <p>
     * Example, the input `/my_file.yml` will result in `files/my_file.yml` if `files` is the files subfolder.
     *
     * @param file the relative file path
     * @return the file path including the files directory
     */
    private String prefixRelativeFile(String file) {
        if (file.startsWith("/")) {
            return AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + file;
        } else {
            return AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/" + file;
        }
    }
}
