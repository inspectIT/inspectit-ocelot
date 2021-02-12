package rocks.inspectit.ocelot.file.accessor.git;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileTestBase;
import rocks.inspectit.ocelot.file.versioning.VersioningManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RevisionAccessIntTest extends FileTestBase {

    /**
     * For convenience: if this field is not null, it will used as working directory during tests.
     * Note: the specified directory is CLEANED before each run, thus, if you have files there, they will be gone ;)
     */
    public static final String TEST_DIRECTORY = null;

    private RevisionAccess revision;

    private VersioningManager versioningManager;

    @BeforeEach
    public void beforeEach() throws IOException, GitAPIException {
        if (TEST_DIRECTORY == null) {
            tempDirectory = Files.createTempDirectory("ocelot");
        } else {
            tempDirectory = Paths.get(TEST_DIRECTORY);
            FileUtils.cleanDirectory(tempDirectory.toFile());
        }

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        versioningManager = new VersioningManager(tempDirectory, () -> authentication, eventPublisher, "@test.com");

        setupRepository();

        revision = versioningManager.getLiveRevision();
    }

    @AfterEach
    public void afterEach() throws IOException {
        if (TEST_DIRECTORY == null) {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    public void setupRepository() throws GitAPIException, IOException {
        versioningManager.setAmendTimeout(-1);

        // initial files - will be included in the live branch
        createTestFiles("files/file_a.yml=a1", "files/sub/file_z.yml=z1", "untracked.yml");
        versioningManager.initialize();

        Files.delete(tempDirectory.resolve("files").resolve("sub").resolve("file_z.yml"));
        versioningManager.commitAllChanges("first");

        createTestFiles("files/file_a.yml=a2", "files/file_b.yml=b1");
        versioningManager.commitAllChanges("second");
    }

    @Nested
    class ListConfigurationFiles {

        @Test
        public void listFiles() {
            List<FileInfo> result = revision.listConfigurationFiles("");

            assertThat(result).hasSize(2);
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("file_a.yml");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(fileInfo.getChildren()).isNull();
            });
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("sub");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.DIRECTORY);

                List<FileInfo> subChildren = fileInfo.getChildren();

                assertThat(subChildren).hasOnlyOneElementSatisfying(subFileInfo -> {
                    assertThat(subFileInfo.getName()).isEqualTo("file_z.yml");
                    assertThat(subFileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                    assertThat(subFileInfo.getChildren()).isNull();
                });
            });
        }

        @Test
        public void listFiles_Deep() throws GitAPIException {
            createTestFiles("files/sub_a/deep/file_x.yml=x", "files/z_file_z.yml");
            versioningManager.commitAllChanges("third");

            List<FileInfo> result = versioningManager.getWorkspaceRevision().listConfigurationFiles("");

            assertThat(result).hasSize(4);
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("file_a.yml");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(fileInfo.getChildren()).isNull();
            });
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("file_b.yml");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(fileInfo.getChildren()).isNull();
            });
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("sub_a");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.DIRECTORY);

                List<FileInfo> subChildren = fileInfo.getChildren();

                assertThat(subChildren).hasOnlyOneElementSatisfying(subFileInfo -> {
                    assertThat(subFileInfo.getName()).isEqualTo("deep");
                    assertThat(subFileInfo.getType()).isEqualTo(FileInfo.Type.DIRECTORY);

                    List<FileInfo> deepSubChildren = subFileInfo.getChildren();

                    assertThat(deepSubChildren).hasOnlyOneElementSatisfying(deepSubFileInfo -> {
                        assertThat(deepSubFileInfo.getName()).isEqualTo("file_x.yml");
                        assertThat(deepSubFileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(deepSubFileInfo.getChildren()).isNull();
                    });
                });
            });
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("z_file_z.yml");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(fileInfo.getChildren()).isNull();
            });
        }
    }

    @Nested
    class ListFiles {

        @Test
        public void listFiles() {
            List<FileInfo> result = revision.listFiles("files");

            assertThat(result).hasSize(2);
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("file_a.yml");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(fileInfo.getChildren()).isNull();
            });
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("sub");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.DIRECTORY);

                List<FileInfo> subChildren = fileInfo.getChildren();

                assertThat(subChildren).hasOnlyOneElementSatisfying(subFileInfo -> {
                    assertThat(subFileInfo.getName()).isEqualTo("file_z.yml");
                    assertThat(subFileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                    assertThat(subFileInfo.getChildren()).isNull();
                });
            });
        }

        @Test
        public void listFilesNullPath() {
            List<FileInfo> result = revision.listFiles(null);

            assertThat(result).hasSize(1);
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("files");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.DIRECTORY);

                List<FileInfo> children = fileInfo.getChildren();
                assertThat(children).hasSize(2);
                assertThat(children).anySatisfy(subFileInfo -> {
                    assertThat(subFileInfo.getName()).isEqualTo("file_a.yml");
                    assertThat(subFileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                    assertThat(subFileInfo.getChildren()).isNull();
                });
                assertThat(children).anySatisfy(subFileInfo -> {
                    assertThat(subFileInfo.getName()).isEqualTo("sub");
                    assertThat(subFileInfo.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                    assertThat(subFileInfo.getChildren()).hasSize(1);
                });
            });
        }
    }

    @Nested
    class ReadConfigurationFile {

        @Test
        public void readFile() {
            Optional<String> result = revision.readConfigurationFile("file_a.yml");

            assertThat(result).hasValue("a1");
        }

        @Test
        public void traversalRead() {
            Optional<String> result = revision.readConfigurationFile("dir/../file_a.yml");

            assertThat(result).hasValue("a1");
        }

        @Test
        public void readNestedFile() {
            Optional<String> result = revision.readConfigurationFile("sub/file_z.yml");

            assertThat(result).hasValue("z1");
        }

        @Test
        public void readDirectory() {
            Optional<String> result = revision.readConfigurationFile("sub");

            assertThat(result).isEmpty();
        }

        @Test
        public void readMissingFile() {
            Optional<String> result = revision.readConfigurationFile("not_existing.yml");

            assertThat(result).isEmpty();
        }

        @Test
        public void illegalPath() {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> revision.readConfigurationFile("../untracked.yml"));
        }
    }

    @Nested
    class ConfigurationFileExists {

        @Test
        public void checkFile() {
            boolean result = revision.configurationFileExists("file_a.yml");

            assertThat(result).isTrue();
        }

        @Test
        public void checkNestedFile() {
            boolean result = revision.configurationFileExists("sub/file_z.yml");

            assertThat(result).isTrue();
        }

        @Test
        public void checkDirectory() {
            boolean result = revision.configurationFileExists("sub");

            assertThat(result).isTrue();
        }

        @Test
        public void fileNotExisting() {
            boolean result = revision.configurationFileExists("not_existing.yml");

            assertThat(result).isFalse();
        }

        @Test
        public void illegalPath() {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> revision.configurationFileExists("../untracked.yml"));
        }
    }

    @Nested
    class ConfigurationFileIsDirectory {

        @Test
        public void checkFile() {
            boolean result = revision.configurationFileIsDirectory("file_a.yml");

            assertThat(result).isFalse();
        }

        @Test
        public void checkDirectory() {
            boolean result = revision.configurationFileIsDirectory("sub");

            assertThat(result).isTrue();
        }

        @Test
        public void fileNotExisting() {
            boolean result = revision.configurationFileIsDirectory("not_existing");

            assertThat(result).isFalse();
        }

        @Test
        public void illegalPath() {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> revision.configurationFileExists(".."));
        }
    }
}