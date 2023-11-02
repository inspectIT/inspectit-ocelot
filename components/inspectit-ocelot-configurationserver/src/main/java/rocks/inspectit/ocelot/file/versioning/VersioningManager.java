package rocks.inspectit.ocelot.file.versioning;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.RemoteConfigurationsSettings;
import rocks.inspectit.ocelot.config.model.RemoteRepositorySettings;
import rocks.inspectit.ocelot.error.exceptions.SelfPromotionNotAllowedException;
import rocks.inspectit.ocelot.events.PromotionEvent;
import rocks.inspectit.ocelot.events.WorkspaceChangedEvent;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.git.CachingRevisionAccess;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.versioning.model.Promotion;
import rocks.inspectit.ocelot.file.versioning.model.SimpleDiffEntry;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceVersion;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This manager handles the interaction with the versioned (Git) representation of the working directory.
 * When using this class, ensure to lock the working directory or resource accordingly to prevent any racing conditions.
 */
@Slf4j
public class VersioningManager {

    /**
     * Exception message in case a expected parent does not exist.
     */
    private static final String EXPECTED_PARENT_EXIST = "Expected parent to exist";

    /**
     * The tag name used for marking which commit has been used for the last remote sync.
     */
    private static final String SOURCE_SYNC_TAG_NAME = "ocelot-sync-base";

    /**
     * Git user used for system commits.
     */
    @VisibleForTesting
    static final PersonIdent GIT_SYSTEM_AUTHOR = new PersonIdent("System", "info@inspectit.rocks");

    /**
     * The server's settings.
     */
    private InspectitServerSettings settings;

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
     * Remote configuration manager for interacting with the remote repository for configuration files.
     */
    private RemoteConfigurationManager remoteConfigurationManager;

    /**
     * Constructor.
     *
     * @param workingDirectory       the working directory to use
     * @param authenticationSupplier the supplier to user for accessing the current user
     * @param eventPublisher         the event publisher to use
     * @param settings               the server's settings
     */
    public VersioningManager(Path workingDirectory, Supplier<Authentication> authenticationSupplier, ApplicationEventPublisher eventPublisher, InspectitServerSettings settings) {
        this.workingDirectory = workingDirectory;
        this.authenticationSupplier = authenticationSupplier;
        this.eventPublisher = eventPublisher;
        this.settings = settings;
    }

    /**
     * Initializes the versioning manager. This method open the Git repository of the working directory. In case the
     * Git directory does not exist, it will be created. Modified files will be automatically commited to the
     * workspace branch.
     */
    public synchronized void initialize() throws GitAPIException, IOException {
        boolean hasGit = isGitRepository();

        git = Git.init().setDirectory(workingDirectory.toFile()).call();

        RemoteConfigurationsSettings remoteSettings = settings.getRemoteConfigurations();
        boolean usingRemoteConfiguration = remoteSettings != null && remoteSettings.isEnabled();

        if (usingRemoteConfiguration) {
            initRemoteConfigurationManager();
        }

        if (!hasGit) {
            log.info("Working directory is not managed by Git. Initializing Git repository and staging and committing all existing file.");

            if (usingRemoteConfiguration && remoteSettings.getPushRepository() != null && remoteConfigurationManager.sourceBranchExistsOnRemote(remoteSettings
                    .getPushRepository())) {
                // we will need to push new commits to target branch in this case; thus, make sure it can be done without force
                setCurrentBranchToTarget();
            }

            stageFiles();
            commitAllFiles(GIT_SYSTEM_AUTHOR, "Initializing Git repository using existing working directory", false);

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
            commitAllFiles(GIT_SYSTEM_AUTHOR, "Staging and committing of external changes during startup", false);
        }

        if (usingRemoteConfiguration) {
            // push the current state during startup
            if (remoteSettings.isPushAtStartup() && remoteSettings.getPushRepository() != null) {
                remoteConfigurationManager.pushBranch(Branch.LIVE, remoteSettings.getPushRepository());
            }

            // fetch and merge the remote source into the local workspace
            if (remoteSettings.isPullAtStartup() && remoteSettings.getPullRepository() != null) {
                remoteConfigurationManager.fetchSourceBranch(settings.getRemoteConfigurations().getPullRepository());
                mergeSourceBranch();
            }
        }
    }

    /**
     * Inits RemoteConfigurationManager and updates remote refs in case they are configured.
     */
    private void initRemoteConfigurationManager() throws GitAPIException {
        if (remoteConfigurationManager == null) {
            remoteConfigurationManager = new RemoteConfigurationManager(settings, git);
            remoteConfigurationManager.updateRemoteRefs();
        }
    }

    /**
     * Sets the currently checked-out branch on top of the remote target branch using {@code git reset}.
     * There are two cases:
     * In case it can be foreseen that the desired state after initialization is that workspace, live, and both remote
     * branches (source/target) are all equal, it does a hard reset. This is the case if there are no local files before
     * the git initialization, initial configuration synchronization and auto promotion are active, and source and target
     * remote branches have the same latest commit. After calling this method, the local branch will then be equal to
     * the target/source branches.
     * <p>
     * Otherwise, it uses soft reset, resulting in a commit history including the target branch's history and another
     * commit that resets everything to the state of the local files. Merging of the files from the source branch has to
     * be done afterwards.
     * <p>
     * Expects the target branch to be present (in configuration and remote push repository).
     */
    private void setCurrentBranchToTarget() throws GitAPIException, IOException {
        log.info("Synchronizing local live branch with target branch of remote push repository.");

        RemoteConfigurationsSettings remoteSettings = settings.getRemoteConfigurations();
        RemoteRepositorySettings sourceRepository = settings.getRemoteConfigurations().getPullRepository();
        RemoteRepositorySettings targetRepository = settings.getRemoteConfigurations().getPushRepository();

        // fetch pull and push repo, as we will need both to compare and synchronize local/pull/push repos
        // push repo is expected to exist
        remoteConfigurationManager.fetchSourceBranch(targetRepository);
        if (sourceRepository != null) {
            remoteConfigurationManager.fetchSourceBranch(sourceRepository);
        }

        // this is true for a fresh start of a config server instance connected to a remote git for backup
        // --> prevent unnecessary commits at startup that don't change any files (particularly when frequently restarting in, e.g., Kubernetes)
        // otherwise, we need to properly merge files from two remotes
        boolean hardReset = remoteSettings.isInitialConfigurationSync() && remoteSettings.isAutoPromotion() && areRemotesEqual(sourceRepository, targetRepository);

        log.info("{}-resetting current branch to '{}'.", (hardReset ? "Hard" : "Soft"), targetRepository.getBranchName());
        git.reset()
                .setRef("refs/heads/" + targetRepository.getBranchName())
                .setMode(hardReset ? ResetCommand.ResetType.HARD : ResetCommand.ResetType.SOFT)
                .call();

        log.info("Local changes can now be pushed to the remote target branch without force.");
    }

    private boolean areRemotesEqual(RemoteRepositorySettings sourceRepository, RemoteRepositorySettings targetRepository) throws IOException {
        if (sourceRepository == null || targetRepository == null) {
            return sourceRepository == targetRepository; // true iff both are null
        }

        Repository repository = git.getRepository();

        ObjectId sourceId = repository.exactRef("refs/heads/" + sourceRepository.getBranchName()).getObjectId();
        ObjectId targetId = repository.exactRef("refs/heads/" + targetRepository.getBranchName()).getObjectId();

        return ObjectId.isEqual(sourceId, targetId);
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
        commitAllFiles(GIT_SYSTEM_AUTHOR, "Staging and committing of external changes", false);
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

        if (commitAllFiles(author, message, author != GIT_SYSTEM_AUTHOR)) {
            eventPublisher.publishEvent(new WorkspaceChangedEvent(this, getWorkspaceRevision()));
        }
    }

    /**
     * Commits ALL files using the given author and message. Consecutive of the same user within {@link #amendTimeout}
     * milliseconds will be amended if specified.
     *
     * @param author     the author to use
     * @param message    the commit message to use
     * @param allowAmend whether commits should be amended if possible (same user and below timeout)
     *
     * @return true, if a commit was created. False, if there was no change to commit.
     */
    private boolean commitAllFiles(PersonIdent author, String message, boolean allowAmend) throws GitAPIException {
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
     * Stage all modified/added/removed configuration files and, if specified, the agent mapping file.
     */
    private void stageFiles() throws GitAPIException {
        log.debug("Staging all configuration files and agent mappings.");

        AddCommand addCommand = git.add().addFilepattern(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER);
        addCommand.addFilepattern(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);

        addCommand.call();
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
                mail = username + settings.getMailSuffix();
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
    @VisibleForTesting
    RevCommit getCommit(ObjectId commitId) {
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
        return getWorkspaceDiff(includeFileContent, oldCommit, newCommit, null);
    }

    /**
     * See {@link #getWorkspaceDiff(boolean, ObjectId, ObjectId, PersonIdent)}.
     *
     * @param includeFileContent whether the file difference (old and new content) is included
     * @param oldCommit          the commit id of the base (old) commit
     * @param newCommit          the commit id of the target (new) commit
     * @param deletingAuthor     the author to be set for all file deletions, instead of detected authors (required if {@code oldCommit} and {@code newCommit} don't have a common ancestor)
     *
     * @return the diff between the specified branches
     */
    private WorkspaceDiff getWorkspaceDiff(boolean includeFileContent, ObjectId oldCommit, ObjectId newCommit, PersonIdent deletingAuthor) throws IOException, GitAPIException {
        // the diff works on TreeIterators, we prepare two for the two branches
        AbstractTreeIterator oldTree = prepareTreeParser(oldCommit);
        AbstractTreeIterator newTree = prepareTreeParser(newCommit);

        // then the procelain diff-command returns a list of diff entries
        List<DiffEntry> diffEntries = git.diff().setOldTree(oldTree).setNewTree(newTree).call();

        // the diff entries are converted into custom ones
        List<SimpleDiffEntry> simpleDiffEntries = diffEntries.stream()
                .map(SimpleDiffEntry::of)
                .map(SimpleDiffEntry::shortenName)
                .collect(Collectors.toList());

        // the diff entries will get their file difference if specified
        if (includeFileContent) {
            RevisionAccess liveRevision = getRevisionById(oldCommit);
            RevisionAccess workspaceRevision = getRevisionById(newCommit);

            simpleDiffEntries.forEach(entry -> fillFileContent(entry, liveRevision, workspaceRevision));
        }

        // fill in the file's authors who did a modification to it
        simpleDiffEntries.forEach(entry -> {
            List<String> authors;
            if (deletingAuthor != null && entry.getType() == DiffEntry.ChangeType.DELETE) {
                authors = Collections.singletonList(deletingAuthor.getName());
            } else {
                authors = getModifyingAuthors(entry, oldCommit, newCommit);
            }
            entry.setAuthors(authors);
        });

        return WorkspaceDiff.builder()
                .entries(simpleDiffEntries)
                .liveCommitId(oldCommit.name())
                .workspaceCommitId(newCommit.name())
                .build();
    }

    @VisibleForTesting
    List<String> getModifyingAuthors(SimpleDiffEntry entry, ObjectId baseCommitId, ObjectId newCommitId) {
        RevCommit baseCommit = getCommit(baseCommitId);
        RevCommit newCommit = getCommit(newCommitId);
        switch (entry.getType()) {
            case ADD:
                return new ArrayList<>(findAuthorsSinceAddition(entry.getFile(), newCommit));
            case MODIFY:
                return new ArrayList<>(findModifyingAuthors(entry.getFile(), baseCommit, newCommit));
            case DELETE:
                return Collections.singletonList(findDeletingAuthor(entry.getFile(), baseCommit, newCommit));
            default:
                log.warn("Unsupported change type for author lookup encountered: {}", entry.getType());
                return Collections.emptyList();
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

        if(file.equals(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME))
            return findModifyingAuthorsForAgentMappings(file, newRevision, baseRevision);
        else
            return findModifyingAuthorsForConfiguration(file, newRevision, baseRevision);
    }

    private Collection<String> findModifyingAuthorsForConfiguration(String file, RevisionAccess newRevision, RevisionAccess baseRevision) {
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
                    .orElseThrow(() -> new IllegalStateException(EXPECTED_PARENT_EXIST));
            if (newRevision.configurationFileExists(file) && newRevision.readConfigurationFile(file)
                    .get()
                    .equals(baseContent)) {
                break; // we have reached a revision where the content is in the original state, no need to look further
            }
        }
        return authors;
    }

    private Collection<String> findModifyingAuthorsForAgentMappings(String file, RevisionAccess newRevision, RevisionAccess baseRevision) {
        Set<String> authors = new HashSet<>();
        String baseContent = baseRevision.readAgentMappings().get();

        //Find all persons who added or modified the file since the last promotion.
        RevisionAccess commonAncestor = newRevision.getCommonAncestor(baseRevision);
        while (!newRevision.getRevisionId().equals(commonAncestor.getRevisionId())) {
            if (newRevision.isAgentMappingsModified()) {
                authors.add(newRevision.getAuthorName());
            } else if (newRevision.isAgentMappingsAdded()) {
                authors.add(newRevision.getAuthorName());
                break; //THe file has been added, no need to take previous changes into account
            }
            newRevision = newRevision.getPreviousRevision()
                    .orElseThrow(() -> new IllegalStateException(EXPECTED_PARENT_EXIST));
            if (newRevision.agentMappingsExist() && newRevision.readAgentMappings()
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

        if(file.equals(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME))
            return findAuthorsSinceAdditionForAgentMappings(newRevision);
        else
            return findAuthorsSinceAdditionForConfiguration(file, newRevision);
    }

    private Collection<String> findAuthorsSinceAdditionForConfiguration(String file, RevisionAccess newRevision) {
        Set<String> authors = new HashSet<>();
        //Find all persons who edited the file since it was added
        while (!newRevision.isConfigurationFileAdded(file)) {
            if (newRevision.isConfigurationFileModified(file)) {
                authors.add(newRevision.getAuthorName());
            }
            newRevision = newRevision.getPreviousRevision()
                    .orElseThrow(() -> new IllegalStateException(EXPECTED_PARENT_EXIST));
        }
        authors.add(newRevision.getAuthorName()); //Also add the name of the person who added the file
        return authors;
    }

    private Collection<String> findAuthorsSinceAdditionForAgentMappings(RevisionAccess newRevision) {
        Set<String> authors = new HashSet<>();
        //Find all persons who edited the file since it was added
        while (!newRevision.isAgentMappingsAdded()) {
            if (newRevision.isAgentMappingsModified()) {
                authors.add(newRevision.getAuthorName());
            }
            newRevision = newRevision.getPreviousRevision()
                    .orElseThrow(() -> new IllegalStateException(EXPECTED_PARENT_EXIST));
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
     * @param baseCommit the commit to compare against, usually the live branch
     * @param newCommit  the commit in which the provided file does not exist anymore, usually on the workspace
     *
     * @return the author of the revision which is responsible for the deletion.
     */
    private String findDeletingAuthor(String file, RevCommit baseCommit, RevCommit newCommit) {
        if(file.equals(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME))
            throw new IllegalStateException(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME + " must not be deleted!");

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
                    .orElseThrow(() -> new IllegalStateException(EXPECTED_PARENT_EXIST));
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
        if(file.equals(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME))
            return findLastChangingRevisionForAgentMappings(baseRevision);
        else
            return findLastChangingRevisionForConfiguration(file, baseRevision);
    }

    private RevisionAccess findLastChangingRevisionForConfiguration(String file, RevisionAccess baseRevision) {
        while (!baseRevision.isConfigurationFileAdded(file) && !baseRevision.isConfigurationFileModified(file)) {
            baseRevision = baseRevision.getPreviousRevision()
                    .orElseThrow(() -> new IllegalStateException(EXPECTED_PARENT_EXIST));
        }
        return baseRevision;
    }

    private RevisionAccess findLastChangingRevisionForAgentMappings(RevisionAccess baseRevision) {
        while (!baseRevision.isAgentMappingsAdded() && !baseRevision.isAgentMappingsModified()) {
            baseRevision = baseRevision.getPreviousRevision()
                    .orElseThrow(() -> new IllegalStateException(EXPECTED_PARENT_EXIST));
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

        if(file.equals(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME))
            fillAgentMappingsFileContent(entry, oldRevision, newRevision);
        else
            fillConfigurationFileContent(entry, oldRevision, newRevision);
    }

    private void fillConfigurationFileContent(SimpleDiffEntry entry, RevisionAccess oldRevision, RevisionAccess newRevision) {
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

    private void fillAgentMappingsFileContent(SimpleDiffEntry entry, RevisionAccess oldRevision, RevisionAccess newRevision) {
        String oldContent = null;
        String newContent = null;

        if (entry.getType() == DiffEntry.ChangeType.ADD) {
            newContent = newRevision.readAgentMappings().orElse(null);
        } else if (entry.getType() == DiffEntry.ChangeType.MODIFY) {
            oldContent = oldRevision.readAgentMappings().orElse(null);
            newContent = newRevision.readAgentMappings().orElse(null);
        } else if (entry.getType() == DiffEntry.ChangeType.DELETE) {
            oldContent = oldRevision.readAgentMappings().orElse(null);
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
     * Promoting the files according to the specified {@link Promotion} definition.
     *
     * @param promotion          the promotion definition
     * @param allowSelfPromotion whether users can promote their own files
     *
     * @return Additional information of the promotion in case the promotion was successful. This might contain additional
     * information about warning or errors which did not affect the promotion itself.
     *
     * @throws SelfPromotionNotAllowedException in case the user tries to promote its own files, but it is prohibited
     * @throws ConcurrentModificationException  in case there was a commit on the live branch in the meantime
     * @throws PromotionFailedException         in case the promotion has been failed
     */
    public PromotionResult promote(Promotion promotion, boolean allowSelfPromotion) throws GitAPIException {
        return promote(promotion, allowSelfPromotion, getCurrentAuthor());
    }

    /**
     * Promoting the files according to the specified {@link Promotion} definition.
     *
     * @param promotion          the promotion definition
     * @param allowSelfPromotion whether users can promote their own files
     * @param author             the author used for the resulting promotion commit
     */
    public synchronized PromotionResult promote(Promotion promotion, boolean allowSelfPromotion, PersonIdent author) throws GitAPIException {
        if (promotion == null || CollectionUtils.isEmpty(promotion.getFiles())) {
            throw new IllegalArgumentException("Promotion must not be null and has to promote at least one file!");
        }

        log.info("User '{}' promotes {} files.", author.getName(), promotion.getFiles().size());

        PromotionResult result = PromotionResult.OK;
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

            // merge target commit into current branch
            mergeFiles(workspaceCommitId, checkoutFiles, removeFiles, promotion.getCommitMessage(), author);

            // optionally: push to remote
            RemoteConfigurationsSettings remoteSettings = settings.getRemoteConfigurations();
            if (remoteConfigurationManager != null && remoteSettings.getPushRepository() != null) {
                RemoteRefUpdate.Status status = remoteConfigurationManager.pushBranch(Branch.LIVE, remoteSettings.getPushRepository());
                if (status != RemoteRefUpdate.Status.OK && status != RemoteRefUpdate.Status.UP_TO_DATE) {
                    result = PromotionResult.SYNCHRONIZATION_FAILED;
                }

                if (result != PromotionResult.SYNCHRONIZATION_FAILED && hasOneRemoteForPushAndPull()) {
                    // as we know we just pushed to the branch we also pull from, update the synchronization tag
                    // to prevent unnecessary merge commits when restarting the configuration server
                    remoteConfigurationManager.fetchSourceBranch(settings.getRemoteConfigurations()
                            .getPullRepository()); // sets local copy of source branch to remote state (equal to live)
                    updateSynchronizationTag(getLatestCommit(Branch.LIVE).get());
                }
            }
        } catch (IOException | GitAPIException ex) {
            throw new PromotionFailedException("Promotion has failed.", ex);
        } finally {
            log.info("Promotion was successful.");

            // checkout workspace branch
            git.checkout().setName(Branch.WORKSPACE.getBranchName()).call();

            eventPublisher.publishEvent(new PromotionEvent(this, getLiveRevision()));
        }

        return result;
    }

    private boolean containsSelfPromotion(Promotion promotion, WorkspaceDiff diff) {
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
        if(file.equals(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME))
            return file;
        else if (file.startsWith("/"))
            return AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + file;
        else
            return AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/" + file;
    }

    /**
     * Returns whether remote repos for pulling and pushing are specified and are equal in terms of Git URI and branch name.
     *
     * @return {@code true} = has the same repo for push and pull, {@code false} = has two different or no remote repos
     */
    private boolean hasOneRemoteForPushAndPull() {
        RemoteConfigurationsSettings remoteSettings = settings.getRemoteConfigurations();
        RemoteRepositorySettings pullRepository = remoteSettings.getPullRepository();
        RemoteRepositorySettings pushRepository = remoteSettings.getPushRepository();

        return pullRepository != null && pushRepository != null && Objects.equals(pullRepository.getGitRepositoryUri(), pushRepository
                .getGitRepositoryUri()) && Objects.equals(pullRepository.getBranchName(), pushRepository.getBranchName());
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

    /**
     * Synchronizes the local workspace branch with the configured remote configuration source. The synchronization
     * is only done in case it is configured and enabled. In this case, the configured remote will be fetched and its
     * branch merged into the local workspace. Optionally, the modifications are promoted into the live branch.
     *
     * @return true in case the synchronization has been done.
     */
    public synchronized boolean pullSourceBranch() throws GitAPIException, IOException {
        RemoteConfigurationsSettings remoteSettings = settings.getRemoteConfigurations();

        if (remoteSettings == null || !remoteSettings.isEnabled() || remoteSettings.getPullRepository() == null) {
            log.info("Remote configuration source will not be pulled because it is not specified or disabled.");
            return false;
        }

        // fetch and merge the remote source into the local workspace
        remoteConfigurationManager.fetchSourceBranch(remoteSettings.getPullRepository());
        mergeSourceBranch();

        return true;
    }

    /**
     * Merges the configured configurations remote source branch into the local workspace branch.
     */
    @VisibleForTesting
    synchronized void mergeSourceBranch() throws GitAPIException, IOException {
        log.info("Merging remote configurations into the workspace.");

        RemoteConfigurationsSettings remoteSettings = settings.getRemoteConfigurations();
        if (remoteSettings == null) {
            throw new IllegalStateException("The remote configuration settings must not be null.");
        }
        RemoteRepositorySettings sourceRepository = remoteSettings.getPullRepository();
        if (sourceRepository == null) {
            throw new IllegalStateException("Source repository settings must not be null.");
        }

        // get ref of synchronization tag
        List<Ref> tagRefs = git.tagList().call();
        Optional<Ref> syncTagRef = tagRefs.stream()
                .filter(tagRef -> tagRef.getName().equals("refs/tags/" + SOURCE_SYNC_TAG_NAME))
                .findFirst();

        // get ref of the configuration source branch
        Repository repository = git.getRepository();
        ObjectId diffTarget = repository.exactRef("refs/heads/" + sourceRepository.getBranchName()).getObjectId();

        if (syncTagRef.isPresent()) {
            // merge the diff between the tag and the source branch head
            ObjectId diffBase = getCommit(syncTagRef.get().getObjectId());
            mergeBranch(diffBase, diffTarget, true, "Merging remote configuration source branch");
        } else if (remoteSettings.isInitialConfigurationSync()) {
            // in case this options is set, we merge the diff between the workspace and the source branch head.
            // we don't remove files in this case!
            log.info("Synchronization marker has not been found. Executing an initial configuration synchronization.");
            Optional<RevCommit> workspaceCommit = getLatestCommit(Branch.WORKSPACE);
            if (workspaceCommit.isPresent()) {
                mergeBranch(workspaceCommit.get(), diffTarget, false, "Initial configuration synchronization");
            } else {
                log.error("Cannot merge configuration source into local workspace because the workspace branch has no commits.");
            }
        } else {
            // otherwise, nothing will be merged
            log.info("Synchronization marker has not been found, thus adding it to the latest commit on the configuration remote.");
        }

        // updating synchronization tag and setting it
        RevCommit commit = getCommit(diffTarget);
        if (commit != null && (!syncTagRef.isPresent() || syncTagRef.get().getObjectId() != commit)) {
            updateSynchronizationTag(commit);
        }
    }

    /**
     * Merges the diff between the {@code baseObject} and {@code targetObject} into the currently checked out branch.
     *
     * @param baseObject    the object (commit, ref, ...) which is used as basis for the diff calculation
     * @param targetObject  the object (commit, ref, ...) which is used as target (the desired state) for the diff calculation
     * @param removeFiles   whether files which are removed in the diff should actually be removed
     * @param commitMessage the message used for the resulting commit
     */
    private void mergeBranch(ObjectId baseObject, ObjectId targetObject, boolean removeFiles, String commitMessage) throws IOException, GitAPIException {
        // getting the diff of the base and target commit
        WorkspaceDiff diff;
        if (removeFiles) {
            diff = getWorkspaceDiff(false, baseObject, targetObject);
        } else {
            diff = getWorkspaceDiff(false, baseObject, targetObject, GIT_SYSTEM_AUTHOR);
        }

        // collect diff files
        List<String> fileToRemove = Collections.emptyList();
        if (removeFiles) {
            fileToRemove = diff.getEntries()
                    .stream()
                    .filter(entry -> entry.getType() == DiffEntry.ChangeType.DELETE)
                    .map(SimpleDiffEntry::getFile)
                    .map(this::prefixRelativeFile)
                    .collect(Collectors.toList());
        }

        List<String> checkoutFiles = diff.getEntries()
                .stream()
                .filter(entry -> entry.getType() != DiffEntry.ChangeType.DELETE)
                .map(SimpleDiffEntry::getFile)
                .map(this::prefixRelativeFile)
                .collect(Collectors.toList());

        if (fileToRemove.isEmpty() && checkoutFiles.isEmpty()) {
            log.info("There is nothing to merge from the source configuration branch into the current workspace branch.");
            return;
        }

        // merge target commit into current branch
        mergeFiles(targetObject, checkoutFiles, fileToRemove, commitMessage, GIT_SYSTEM_AUTHOR);

        // promote if enabled
        if (settings.getRemoteConfigurations().isAutoPromotion()) {
            log.info("Auto-promotion of synchronized configuration files.");
            List<String> diffFiles = diff.getEntries()
                    .stream()
                    .map(SimpleDiffEntry::getFile)
                    .collect(Collectors.toList());

            Promotion promotion = Promotion.builder()
                    .commitMessage("Auto-promotion due to workspace remote synchronization.")
                    .workspaceCommitId(getLatestCommit(Branch.WORKSPACE).get().getId().getName())
                    .liveCommitId(getLatestCommit(Branch.LIVE).get().getId().getName())
                    .files(diffFiles)
                    .build();

            promote(promotion, true, GIT_SYSTEM_AUTHOR);
        }

    }

    /**
     * Merges the {@code checkoutFiles} files from the {@code targetCommit} into the currently checked out branch. Files
     * contained in the {@code removeFiles} will be removed. The {@code targetCommit} will be set as a parent of the
     * resulting commit.
     *
     * @param targetCommit  the commit containing the desired files
     * @param checkoutFiles the files which should be taken from the {@code targetCommit}
     * @param removeFiles   the files which will be removed
     * @param commitMessage the message for the resulting commit
     * @param commitAuthor  the author of the resulting commit
     */
    private void mergeFiles(ObjectId targetCommit, List<String> checkoutFiles, List<String> removeFiles, String commitMessage, PersonIdent commitAuthor) throws GitAPIException, IOException {
        // create (start) an empty merge-commit
        git.merge()
                .include(targetCommit)
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
            git.checkout().setStartPoint(targetCommit.name()).addPaths(checkoutFiles).call();
        }

        // adding changed files
        AddCommand addCommand = git.add();
        Stream.concat(removeFiles.stream(), checkoutFiles.stream()).forEach(addCommand::addFilepattern);
        addCommand.call();

        // commit changes
        git.commit().setMessage(commitMessage).setAuthor(commitAuthor).call();
    }

    /**
     * Sets the synchronization tag ({@link #SOURCE_SYNC_TAG_NAME}) to the specified commit. In case the tag already
     * exists, it will be updated.
     *
     * @param commit the commit to set the tag to
     */
    private void updateSynchronizationTag(RevCommit commit) throws GitAPIException {
        if (commit == null) {
            throw new IllegalArgumentException("Target commit must not be null");
        }
        log.debug("Adding synchronization tag to commit {}.", commit.getName());
        git.tag().setName(SOURCE_SYNC_TAG_NAME).setObjectId(commit).setForceUpdate(true).call();
    }
}
