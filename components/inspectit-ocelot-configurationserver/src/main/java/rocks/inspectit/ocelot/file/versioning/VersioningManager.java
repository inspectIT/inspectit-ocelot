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

            stageAndCommit();
        }
    }

    public void destroy() {
        git.close();
    }

    public synchronized void stageAndCommit() throws GitAPIException {
        if (!isClean()) {
            stageAll();
            commit();
        }
    }

    public synchronized void resetConfigurationFiles() throws GitAPIException {
        //TODO has to be discussed whether it is meaningful this way. This ensures that the Git repository
        //TODO is not affected by changes which are done by editing files in the working directory manually.
        //TODO Imo it is valid to assume, that all changes HAVE TO BE done via the config-server.
        if (!isClean()) {
            try {
                Path filePath = workingDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER);
                FileUtils.cleanDirectory(filePath.toFile());
            } catch (IOException e) {
                log.warn("Working directory could not be cleaned.", e);
            }

            git.checkout()
                    .addPath(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER)
                    .call();
        }
    }

    private void commit() throws GitAPIException {
        log.info("Committing all staged changes.");

        PersonIdent author = getAuthor();

        CommitCommand commitCommand = git.commit()
                .setAll(true)
                .setMessage("Commit configuration file and agent mapping changes")
                .setAuthor(author);

        if (getCommitCount() > 0) {
            PersonIdent latestAuthor = getLatestCommitAuthor();
            boolean sameAuthor = latestAuthor != null && author.getName().equals(latestAuthor.getName());

            long latestCommitTime = getLatestCommitTime() * 1000L;
            boolean expired = latestCommitTime + amendTimeout < System.currentTimeMillis();

            if (sameAuthor && !expired) {
                log.debug("|-Executing an amend commit.");
                commitCommand.setAmend(true);
            }
        }

        commitCommand.call();
    }

    private void stageAll() throws GitAPIException {
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

    private PersonIdent getLatestCommitAuthor() {
        return getLatestCommit().map(RevCommit::getAuthorIdent).orElse(null);
    }

    private int getLatestCommitTime() {
        return getLatestCommit().map(RevCommit::getCommitTime).orElse(-1);
    }

    private Optional<RevCommit> getLatestCommit() {
        try {
            ObjectId commitId = git.getRepository().resolve(Constants.HEAD);

            if (commitId == null) {
                return Optional.empty();
            }

            try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                RevCommit commit = revWalk.parseCommit(commitId);
                return Optional.ofNullable(commit);
            }
        } catch (IOException e) {
            //TODO error message
            log.error("error", e);
            return Optional.empty();
        }
    }
}
