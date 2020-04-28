package rocks.inspectit.ocelot.file.versioning;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.VersioningSettings;
import rocks.inspectit.ocelot.file.FileTestBase;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class VersioningManagerIntTest extends FileTestBase {

    /**
     * For convenience: if this field is not null, it will used as working directory during tests.
     * Note: the specified directory is CLEANED before each run, thus, if you have files there, they will be gone ;)
     */
    public static final String TEST_DIRECTORY = null;

    private VersioningManager versioningManager;

    @BeforeEach
    public void beforeEach() throws IOException {
        if (TEST_DIRECTORY == null) {
            tempDirectory = Files.createTempDirectory("ocelot");
        } else {
            tempDirectory = Paths.get(TEST_DIRECTORY);
            FileUtils.cleanDirectory(tempDirectory.toFile());
        }

        VersioningSettings versioningSettings = new VersioningSettings();
        versioningSettings.setGitUsername("ocelot");
        versioningSettings.setGitMail("ocelot@inspectit.rocks");
        InspectitServerSettings settings = new InspectitServerSettings();
        settings.setWorkingDirectory(tempDirectory.toString());
        settings.setVersioning(versioningSettings);

        versioningManager = new VersioningManager(settings);

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

            versioningManager.init();

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

            versioningManager.init();

            boolean cleanFirst = versioningManager.isClean();
            boolean initSecond = Files.exists(tempDirectory.resolve(".git"));

            versioningManager.init();

            boolean cleanSecond = versioningManager.isClean();
            boolean initThird = Files.exists(tempDirectory.resolve(".git"));

            assertThat(initFirst).isFalse();
            assertThat(initSecond).isTrue();
            assertThat(initThird).isTrue();
            assertThat(cleanFirst).isTrue();
            assertThat(cleanSecond).isTrue();
        }
    }

    @Nested
    class IsClean {

        @Test
        public void cleanRepository() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml", "untracked-file");
            versioningManager.init();

            boolean result = versioningManager.isClean();

            assertThat(result).isTrue();
        }

        @Test
        public void modificationChanges() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            versioningManager.init();

            boolean before = versioningManager.isClean();

            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME + "=content");

            boolean after = versioningManager.isClean();

            assertThat(before).isTrue();
            assertThat(after).isFalse();
        }

        @Test
        public void untrackedChanges() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            versioningManager.init();

            boolean before = versioningManager.isClean();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            boolean after = versioningManager.isClean();

            assertThat(before).isTrue();
            assertThat(after).isFalse();
        }

        @Test
        public void ignoredFile() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, "ignored-file");
            versioningManager.init();

            boolean result = versioningManager.isClean();

            assertThat(result).isTrue();
        }
    }
}