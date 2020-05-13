package rocks.inspectit.ocelot.file.versioning;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.ocelot.file.FileTestBase;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VersioningManagerTest extends FileTestBase {

    /**
     * For convenience: if this field is not null, it will used as working directory during tests.
     * Note: the specified directory is CLEANED before each run, thus, if you have files there, they will be gone ;)
     */
    public static final String TEST_DIRECTORY = null;

    private VersioningManager versioningManager;

    private Authentication authentication;

    @BeforeEach
    public void beforeEach() throws IOException {
        if (TEST_DIRECTORY == null) {
            tempDirectory = Files.createTempDirectory("ocelot");
        } else {
            tempDirectory = Paths.get(TEST_DIRECTORY);
            FileUtils.cleanDirectory(tempDirectory.toFile());
        }

        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");

        versioningManager = new VersioningManager(tempDirectory, () -> authentication);

        System.out.println("Test data in: " + tempDirectory.toString());
    }

    @AfterEach
    public void afterEach() throws IOException {
        if (TEST_DIRECTORY == null) {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    @Nested
    class Init {

        @Test
        public void initAndStageFiles() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml", "untracked-file");

            boolean before = Files.exists(tempDirectory.resolve(".git"));

            versioningManager.initialize();

            boolean after = Files.exists(tempDirectory.resolve(".git"));

            boolean clean = versioningManager.isClean();
            assertThat(clean).isTrue();
            assertThat(before).isFalse();
            assertThat(after).isTrue();
        }

        @Test
        public void multipleCalls() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml", "untracked-file");

            boolean initFirst = Files.exists(tempDirectory.resolve(".git"));
            int firstCount = versioningManager.getCommitCount();
            assertThat(initFirst).isFalse();
            assertThat(firstCount).isZero();

            versioningManager.initialize();

            boolean cleanFirst = versioningManager.isClean();
            boolean initSecond = Files.exists(tempDirectory.resolve(".git"));
            int secondCount = versioningManager.getCommitCount();
            assertThat(initSecond).isTrue();
            assertThat(cleanFirst).isTrue();
            assertThat(secondCount).isOne();

            versioningManager.initialize();

            boolean cleanSecond = versioningManager.isClean();
            int thirdCount = versioningManager.getCommitCount();
            assertThat(cleanSecond).isTrue();
            assertThat(thirdCount).isOne();
        }
    }

    @Nested
    class IsClean {

        @Test
        public void cleanRepository() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml", "untracked-file");
            versioningManager.initialize();

            boolean result = versioningManager.isClean();

            assertThat(result).isTrue();
        }

        @Test
        public void modificationChanges() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            versioningManager.initialize();

            boolean before = versioningManager.isClean();

            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME + "=content");

            boolean after = versioningManager.isClean();

            assertThat(before).isTrue();
            assertThat(after).isFalse();
        }

        @Test
        public void untrackedChanges() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            versioningManager.initialize();

            boolean before = versioningManager.isClean();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            boolean after = versioningManager.isClean();

            assertThat(before).isTrue();
            assertThat(after).isFalse();
        }

        @Test
        public void ignoredFile() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, "ignored-file");
            versioningManager.initialize();

            boolean result = versioningManager.isClean();

            assertThat(result).isTrue();
        }
    }

    @Nested
    class Destroy {

        @Test
        public void callDestroy() {
            Git gitMock = mock(Git.class);
            ReflectionTestUtils.setField(versioningManager, "git", gitMock);

            versioningManager.destroy();

            verify(gitMock).close();
            verifyNoMoreInteractions(gitMock);
        }
    }


    @Nested
    class ResetConfigurationFiles {

        @Test
        public void test() throws GitAPIException {
            versioningManager.initialize();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.stageAndCommit();

            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=asdasdasd");


            versioningManager.stageAndCommit();
            versioningManager.resetConfigurationFiles();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=asdasdasd");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/another.yml");

            versioningManager.resetConfigurationFiles();
        }
    }

    @Nested
    class StageAndCommit {

        @Test
        public void amend() throws GitAPIException {
            versioningManager.initialize();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.stageAndCommit();

            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=new_content");
            versioningManager.stageAndCommit();

            assertThat(versioningManager.getCommitCount()).isOne();
        }

        @Test
        public void amendTimeout() throws GitAPIException {
            versioningManager.initialize();
            versioningManager.setAmendTimeout(-2000);

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.stageAndCommit();

            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=new_content");
            versioningManager.stageAndCommit();

            assertThat(versioningManager.getCommitCount()).isEqualTo(2);
        }
    }
}