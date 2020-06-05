package rocks.inspectit.ocelot.file.versioning;

import com.google.common.collect.Iterables;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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

@Slf4j
public class VersioningManager {

    private static final PersonIdent GIT_SYSTEM_AUTHOR = new PersonIdent("System", "info@inspectit.rocks");

    private Path workingDirectory;

    private Git git;

    private Supplier<Authentication> authenticationSupplier;

    @Setter
    private long amendTimeout = Duration.ofMinutes(10).toMillis();

    public VersioningManager(Path workingDirectory, Supplier<Authentication> authenticationSupplier) {
        this.workingDirectory = workingDirectory;
        this.authenticationSupplier = authenticationSupplier;
    }

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

    public void destroy() {
        git.close();
    }

    public synchronized void commitAsExternalChange() throws GitAPIException {
        if (isClean()) {
            return;
        }
        log.info("Staging and committing of external changes to the configuration files or agent mappings");
        commit(GIT_SYSTEM_AUTHOR, "Staging and committing of external changes", false);
    }

    public synchronized void commit(String message) throws GitAPIException {
        PersonIdent author = getAuthor();

        commit(author, message, true);
    }

    private synchronized void commit(PersonIdent author, String message, boolean allowAmend) throws GitAPIException {
        if (isClean()) {
            return;
        }

        stageFiles();
        commitFiles(author, message, allowAmend);
    }

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

    private void stageFiles() throws GitAPIException {
        log.info("Staging all configuration files and agent mappings.");

        git.add()
                .addFilepattern(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER)
                .addFilepattern(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME)
                .call();
    }

    public boolean isClean() throws GitAPIException {
        Status status = git.status()
                .addPath(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER)
                .addPath(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME)
                .call();

        return status.isClean();
    }

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

    private PersonIdent getAuthor() {
        Authentication authentication = authenticationSupplier.get();
        String username = authentication.getName();

        return new PersonIdent(username, "info@inspectit.rocks");
    }

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
