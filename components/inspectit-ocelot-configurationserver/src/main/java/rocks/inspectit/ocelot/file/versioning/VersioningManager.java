package rocks.inspectit.ocelot.file.versioning;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.security.core.Authentication;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

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
     * Timeout for amend commits. Consecutive commits of the same user within this time will be amended.
     */
    @Setter
    private long amendTimeout = Duration.ofMinutes(10).toMillis();

    /**
     * Constructor.
     *
     * @param workingDirectory       the working directory to use
     * @param authenticationSupplier the supplier to user for accessing the current user
     */
    public VersioningManager(Path workingDirectory, Supplier<Authentication> authenticationSupplier) {
        this.workingDirectory = workingDirectory;
        this.authenticationSupplier = authenticationSupplier;
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
            commit(GIT_SYSTEM_AUTHOR, "Initializing Git repository using existing working directory", false);
        } else if (!isClean()) {
            log.info("Changes in the configuration or agent mapping files have been detected and will be committed to the repository.");
            commit(GIT_SYSTEM_AUTHOR, "Staging and committing of external changes during startup", false);
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
        commit(GIT_SYSTEM_AUTHOR, "Staging and committing of external changes", false);
    }

    /**
     * Committing all currently modified files using the given commit message. The author of the commit is provided
     * by the {@link #authenticationSupplier}. Commits will be amended in case the previous one is made by the same
     * user and is newer than {@link #amendTimeout} milliseconds.
     *
     * @param message the commit message to use
     */
    public synchronized void commit(String message) throws GitAPIException {
        PersonIdent author = getAuthor();

        commit(author, message, true);
    }

    /**
     * Committing all currently modified files using the given commit message and the given author.
     *
     * @param author     the author to use
     * @param message    the commit message to use
     * @param allowAmend whether commits should be amended if possible (same user and below timeout)
     */
    private synchronized void commit(PersonIdent author, String message, boolean allowAmend) throws GitAPIException {
        if (isClean()) {
            return;
        }

        stageFiles();
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
    private PersonIdent getAuthor() {
        Authentication authentication = authenticationSupplier.get();
        String username = authentication.getName();

        return new PersonIdent(username, "info@inspectit.rocks");
    }

    /**
     * @return Returns the latest commit of the current branch.
     */
    private Optional<RevCommit> getLatestCommit() {
        try {
            ObjectId commitId = git.getRepository().resolve(Constants.HEAD);

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
}
