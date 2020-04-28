package rocks.inspectit.ocelot.file.versioning;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.VersioningSettings;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class VersioningManager {

    private VersioningSettings versioningSettings;

    private Path workingDirectory;

    private Git git;

    @Autowired
    public VersioningManager(InspectitServerSettings settings) {
        this.versioningSettings = settings.getVersioning();
        this.workingDirectory = Paths.get(settings.getWorkingDirectory()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws GitAPIException {
        boolean hasGit = isGitRepository();

        git = Git.init().setDirectory(workingDirectory.toFile()).call();

        if (!hasGit) {
            log.info("Working directory is not managed by Git. Initializing Git repository and staging and committing all existing file.");

            stageAndCommit();
        }
    }

    @PreDestroy
    @VisibleForTesting
    void destroy() {
        git.close();
    }

    public void stageAndCommit() throws GitAPIException {
        if (!isClean()) {
            stageAll();
            commit();
        }
    }

    private void commit() throws GitAPIException {
        log.info("Committing all staged changes.");

        git.commit()
                .setAll(true)
                .setMessage("Commit configuration file and agent mapping changes")
                .setAuthor(versioningSettings.getGitUsername(), versioningSettings.getGitMail())
                .call();
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
        } catch (GitAPIException e) {
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
}
