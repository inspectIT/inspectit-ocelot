package rocks.inspectit.ocelot.file.accessor;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileTestBase;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(value = Parameterized.class)
public abstract class AbstractFileAccessorTest<T extends AbstractFileAccessor> extends FileTestBase {

    protected T accessor;

    protected abstract T createInstance() throws Exception;

    @BeforeEach
    public void setUp() throws Exception {
        accessor = createInstance();
    }

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }

    @Nested
    class ListFiles {

        @Test
        public void listFiles() {
            createTestFiles("one.yml", "files/second.yml", "files/sub/third.yml", "files/sub/deep/four.yml", "files/sub/five.yml", "files/six.yml");

            List<FileInfo> result = accessor.listConfigurationFiles(".");

            assertThat(result).isNotEmpty();

            assertThat(result).hasSize(3);
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("second.yml");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(fileInfo.getChildren()).isNull();
            });
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("six.yml");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(fileInfo.getChildren()).isNull();
            });
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("sub");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                assertThat(fileInfo.getChildren()).hasSize(3);

                List<FileInfo> subChildren = fileInfo.getChildren();

                assertThat(subChildren).anySatisfy(subFileInfo -> {
                    assertThat(subFileInfo.getName()).isEqualTo("third.yml");
                    assertThat(subFileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                    assertThat(subFileInfo.getChildren()).isNull();
                });
                assertThat(subChildren).anySatisfy(subFileInfo -> {
                    assertThat(subFileInfo.getName()).isEqualTo("five.yml");
                    assertThat(subFileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                    assertThat(subFileInfo.getChildren()).isNull();
                });
                assertThat(subChildren).anySatisfy(subFileInfo -> {
                    assertThat(subFileInfo.getName()).isEqualTo("deep");
                    assertThat(subFileInfo.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                    assertThat(subFileInfo.getChildren()).hasSize(1);

                    FileInfo child = subFileInfo.getChildren().get(0);

                    assertThat(child.getName()).isEqualTo("four.yml");
                    assertThat(child.getType()).isEqualTo(FileInfo.Type.FILE);
                    assertThat(child.getChildren()).isNull();
                });
            });
        }

        @Test
        public void listNestedFiles() {
            createTestFiles("one.yml", "files/second.yml", "files/sub/third.yml");

            List<FileInfo> result = accessor.listConfigurationFiles("sub");

            assertThat(result).isNotEmpty();

            assertThat(result).hasSize(1);
            assertThat(result).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("third.yml");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(fileInfo.getChildren()).isNull();
            });
        }

        @Test
        public void directoryDoesNotExist() {
            createTestFiles("one.yml", "files/second.yml");

            List<FileInfo> result = accessor.listConfigurationFiles("not-existing");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ReadConfigurationFile {

        @Test
        public void fileNotExisting() {
            Optional<String> result = accessor.readConfigurationFile("test.yml");

            assertThat(result).isEmpty();
        }

        @Test
        public void emptyFile() {
            createTestFiles("files/test.yml");

            Optional<String> result = accessor.readConfigurationFile("test.yml");

            assertThat(result).hasValue("");
        }

        @Test
        public void readFile() {
            createTestFiles("files/test.yml=content");

            Optional<String> result = accessor.readConfigurationFile("./test.yml");

            assertThat(result).hasValue("content");
        }

        @Test
        public void readNestedFile() {
            createTestFiles("files/sub/test.yml=content");

            Optional<String> result = accessor.readConfigurationFile("sub/test.yml");

            assertThat(result).hasValue("content");
        }

        @Test
        public void validTraversal() {
            createTestFiles("files/test.yml");

            Optional<String> result = accessor.readConfigurationFile("sub/../test.yml");

            assertThat(result).hasValue("");
        }

        @Test
        public void illegalPath() {
            createTestFiles("files/test.yml");

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> accessor.readConfigurationFile("../test.yml"))
                    .withMessageStartingWith("User path escapes the base path:");
        }

        @Test
        public void absolutePath() {
            createTestFiles("files/test.yml");

            String path;
            if (System.getProperty("os.name").contains("Windows")) {
                path = "c:/file";
            } else {
                path = "/absolute/file";
            }

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> accessor.readConfigurationFile(path))
                    .withMessageStartingWith("Path must be relative:");
        }
    }

    @Nested
    class ReadAgentMappings {

        @Test
        public void agentMappingsDoesNotExist() {
            Optional<String> result = accessor.readAgentMappings();

            assertThat(result).isEmpty();
        }

        @Test
        public void readEmptyAgentMappings() {
            createTestFiles("agent_mappings.yaml");

            Optional<String> result = accessor.readAgentMappings();

            assertThat(result).hasValue("");
        }

        @Test
        public void readAgentMappings() {
            createTestFiles("agent_mappings.yaml=content");

            Optional<String> result = accessor.readAgentMappings();

            assertThat(result).hasValue("content");
        }
    }

    @Nested
    class ConfigurationFileExists {

        @Test
        public void doesNotExist() {
            boolean result = accessor.configurationFileExists("file");

            assertThat(result).isFalse();
        }

        @Test
        public void fileExist() {
            createTestFiles("files/sub/file");

            boolean result = accessor.configurationFileExists("sub/file");

            assertThat(result).isTrue();
        }

        @Test
        public void directoryExist() {
            createTestFiles("files/sub/file");

            boolean result = accessor.configurationFileExists("sub");

            assertThat(result).isTrue();
        }
    }

    @Nested
    class ConfigurationFileIsDirectory {

        @Test
        public void doesNotExist() {
            boolean result = accessor.configurationFileIsDirectory("target");

            assertThat(result).isFalse();
        }

        @Test
        public void isDirectory() {
            createTestFiles("files/target/file");

            boolean result = accessor.configurationFileIsDirectory("target");

            assertThat(result).isTrue();
        }

        @Test
        public void isNotDirectory() {
            createTestFiles("files/target/file");

            boolean result = accessor.configurationFileIsDirectory("target/file");

            assertThat(result).isFalse();
        }
    }
}