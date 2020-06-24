package rocks.inspectit.ocelot.file.versioning;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import rocks.inspectit.ocelot.events.ConfigurationPromotionEvent;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.versioning.model.Diff;
import rocks.inspectit.ocelot.file.versioning.model.SimpleDiffEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
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

    private ApplicationEventPublisher eventPublisher;

    private boolean setupBranches = false;

    /**
     * Timeout for amend commits. Consecutive commits of the same user within this time will be amended.
     */
    @Setter
    private long amendTimeout = Duration.ofMinutes(10).toMillis();

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
            commit(GIT_SYSTEM_AUTHOR, "Initializing Git repository using existing working directory", true, false);

            if (getCommitCount() > 0) {
                setupBranches();
            } else {
                setupBranches = true;
            }
        } else if (!isClean()) {
            log.info("Changes in the configuration or agent mapping files have been detected and will be committed to the repository.");
            commit(GIT_SYSTEM_AUTHOR, "Staging and committing of external changes during startup", true, false);
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
        commit(GIT_SYSTEM_AUTHOR, "Staging and committing of external changes", true, false);
    }

    /**
     * Committing all currently modified files using the given commit message. The author of the commit is provided
     * by the {@link #authenticationSupplier}. Commits will be amended in case the previous one is made by the same
     * user and is newer than {@link #amendTimeout} milliseconds.
     *
     * @param message the commit message to use
     */
    public synchronized void commit(String message) throws GitAPIException {
        PersonIdent author = getCurrentAuthor();

        commit(author, message, true, true);
    }

    /**
     * Committing all currently modified files using the given commit message and the given author.
     *
     * @param author     the author to use
     * @param message    the commit message to use
     * @param allowAmend whether commits should be amended if possible (same user and below timeout)
     */
    private synchronized void commit(PersonIdent author, String message, boolean stageFiles, boolean allowAmend) throws GitAPIException {
        if (isClean()) {
            return;
        }

        if (stageFiles) {
            stageFiles();
        }

        commitFiles(author, message, allowAmend);
    }

    /**
     * Commits the staged files using the given author and message. Consecutive of the same user within {@link #amendTimeout}
     * milliseconds will be amended if specified.
     *
     * @param author     the author to use
     * @param message    the commit message to use
     * @param allowAmend whether commits should be amended if possible (same user and below timeout)
     */
    private void commitFiles(PersonIdent author, String message, boolean allowAmend) throws GitAPIException {
        log.info("Committing staged changes.");

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

        if (setupBranches) {
            setupBranches = false;
            setupBranches();
        }
    }

    private void setupBranches() throws GitAPIException {
        git.branchRename().setNewName(Branch.WORKSPACE.getBranchName()).call();

        git.branchCreate().setName(Branch.LIVE.getBranchName()).call();
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
     * @return Returns the latest commit of the current branch.
     */
    public Optional<RevCommit> getLatestCommit() {
        return getLatestCommit(Branch.WORKSPACE);
    }

    public Optional<RevCommit> getLatestCommit(Branch targetBranch) {
        try {
            ObjectId commitId = git.getRepository().resolve("refs/heads/" + targetBranch.getBranchName());

            if (commitId == null) {
                return Optional.empty();
            }

            try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                RevCommit commit = revWalk.parseCommit(commitId);
                return Optional.of(commit);
            }
        } catch (IOException e) {
            log.error("An exception occurred while trying to load the latest commit.", e);
            return Optional.empty();
        }
    }

    public RevisionAccess getLiveRevision() {
        Optional<RevCommit> latestCommit = getLatestCommit(Branch.LIVE);
        return latestCommit.map(revCommit -> new RevisionAccess(git.getRepository(), revCommit)).orElse(null);
    }

    /**
     * See: https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowBranchDiff.java
     *
     * @throws IOException
     * @throws GitAPIException
     */
    public Diff getDiff() throws IOException, GitAPIException {
        Repository repository = git.getRepository();

        // the diff works on TreeIterators, we prepare two for the two branches
        Pair<AbstractTreeIterator, String> oldTree = prepareTreeParser(repository, "refs/heads/" + Branch.LIVE.getBranchName());
        Pair<AbstractTreeIterator, String> newTree = prepareTreeParser(repository, "refs/heads/" + Branch.WORKSPACE.getBranchName());

        // then the procelain diff-command returns a list of diff entries
        List<DiffEntry> diffEntries = git.diff().setOldTree(oldTree.getLeft()).setNewTree(newTree.getLeft()).call();

        List<SimpleDiffEntry> simpleDiffEntries = diffEntries.stream()
                .map(SimpleDiffEntry::of)
                .collect(Collectors.toList());

        Diff diff = new Diff();
        diff.setDiffEntries(simpleDiffEntries);
        diff.setLiveCommitId(oldTree.getRight());
        diff.setWorkspaceCommitId(newTree.getRight());

        return diff;
    }

    private static Pair<AbstractTreeIterator, String> prepareTreeParser(Repository repository, String ref) throws IOException {

        // from the commit we can build the tree which allows us to construct the TreeParser
        Ref head = repository.exactRef(ref);
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(head.getObjectId());
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return Pair.of(treeParser, commit.getId().name());
        }
    }

    //TODO synchronize working directory
    public void promoteConfiguration(ConfigurationPromotion promotion) throws GitAPIException, IOException {
        try {
            git.checkout().setName(Branch.LIVE.getBranchName()).call();

            Optional<RevCommit> liveCommitOptional = getLatestCommit(Branch.LIVE);
            if (liveCommitOptional.isPresent()) {
                String liveCommitId = liveCommitOptional.get().getId().name();

                if (!liveCommitId.equals(promotion.getLiveCommitId())) {
                    throw new RuntimeException("change in between");
                }
            } else {
                throw new RuntimeException("should not happen");
            }

            Diff diff = getDiff();
            Map<String, DiffEntry.ChangeType> changeIndex = diff.getDiffEntries().stream()
                    .collect(Collectors.toMap(SimpleDiffEntry::getFile, SimpleDiffEntry::getType));

            boolean callRemove = false;
            boolean callCheckout = false;
            RmCommand rmCommand = git.rm();
            CheckoutCommand checkoutCommand = git.checkout().setStartPoint(promotion.getWorkspaceCommitId());

            for (String file : promotion.getFiles()) {
                DiffEntry.ChangeType changeType = changeIndex.get(file);
                if (changeType != null) {
                    if (changeType == DiffEntry.ChangeType.DELETE) {
                        rmCommand.addFilepattern(file);
                        callRemove = true;
                    } else {
                        checkoutCommand.addPath(file);
                        callCheckout = true;
                    }
                }
            }

            if (callRemove) {
                rmCommand.call();
            }
            if (callCheckout) {
                checkoutCommand.call();
            }

            commit(getCurrentAuthor(), "Promoting configuration", false, false);

        } finally {
            eventPublisher.publishEvent(new ConfigurationPromotionEvent(this));

            git.checkout().setName(Branch.WORKSPACE.getBranchName()).call();
            //TODO hard reset in case of error?
        }
    }
}
