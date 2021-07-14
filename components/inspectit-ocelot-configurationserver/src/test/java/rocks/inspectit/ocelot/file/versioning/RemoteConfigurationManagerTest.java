package rocks.inspectit.ocelot.file.versioning;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.RemoteConfigurationsSettings;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteConfigurationManagerTest {

    private RemoteConfigurationManager manager;

    private Git gitLocal;

    private Git gitRemote;

    private RemoteConfigurationsSettings remoteSettings;

    private InspectitServerSettings settings;

    private Path tempDirectoryLocal;

    private Path tempDirectoryRemote;

    @BeforeEach
    public void beforeEach() throws IOException, GitAPIException, URISyntaxException {
        tempDirectoryLocal = Files.createTempDirectory("git-test-local");
        tempDirectoryRemote = Files.createTempDirectory("git-test-remote");

        gitLocal = Git.init().setDirectory(tempDirectoryLocal.toFile()).call();
        gitRemote = Git.init().setDirectory(tempDirectoryRemote.toFile()).call();

        remoteSettings = RemoteConfigurationsSettings.builder()
                .enabled(true)
                .remoteName("test-remote")
                .targetBranch("target-branch")
                .gitRepositoryUri(new URIish(tempDirectoryRemote.toString() + "/.git"))
                .build();

        settings = InspectitServerSettings.builder().remoteConfigurations(remoteSettings).build();
    }

    @AfterEach
    public void afterEach() throws IOException {
        FileUtils.deleteDirectory(tempDirectoryLocal.toFile());
        FileUtils.deleteDirectory(tempDirectoryRemote.toFile());
    }

    @Nested
    public class UpdateRemoteRefs {

        @Test
        public void initRef() throws GitAPIException {
            manager = new RemoteConfigurationManager(settings, gitLocal);

            assertThat(gitLocal.remoteList().call()).isEmpty();

            manager.updateRemoteRefs();

            assertThat(gitLocal.remoteList().call()).extracting(RemoteConfig::getName)
                    .containsExactly(remoteSettings.getRemoteName());
        }

        @Test
        public void updateRef() throws GitAPIException, URISyntaxException {
            gitLocal.remoteAdd()
                    .setName(remoteSettings.getRemoteName())
                    .setUri(new URIish("https://example.org"))
                    .call();

            manager = new RemoteConfigurationManager(settings, gitLocal);

            manager.updateRemoteRefs();

            List<RemoteConfig> remotes = gitLocal.remoteList().call();
            assertThat(remotes).hasSize(1);
            RemoteConfig remote = remotes.get(0);
            assertThat(remote.getName()).isEqualTo(remoteSettings.getRemoteName());
            assertThat(remote.getURIs()).element(0)
                    .extracting(URIish::toString)
                    .isEqualTo(new URIish(tempDirectoryRemote.toString() + "/.git").toString());
        }
    }

    @Nested
    public class PushBranch {

        @Test
        public void pushBranch() throws GitAPIException, IOException {
            File testFile = new File(tempDirectoryLocal.toFile(), "test.file");
            testFile.createNewFile();

            gitLocal.add().addFilepattern(".").call();
            gitLocal.commit().setMessage("push files").call();
            gitLocal.branchCreate().setName(Branch.WORKSPACE.getBranchName()).call();

            manager = new RemoteConfigurationManager(settings, gitLocal);
            manager.updateRemoteRefs();

            manager.pushBranch(Branch.WORKSPACE, "different-branch");

            List<Ref> refs = gitRemote.branchList().call();
            assertThat(refs).extracting(Ref::getName).containsExactly("refs/heads/different-branch");
        }
    }
}