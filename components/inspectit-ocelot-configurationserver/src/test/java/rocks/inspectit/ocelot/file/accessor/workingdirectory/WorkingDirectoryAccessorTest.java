package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileTestBase;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class WorkingDirectoryAccessorTest extends FileTestBase {

    private AbstractWorkingDirectoryAccessor accessor;

    @BeforeEach
    public void beforeEach() throws IOException {
        tempDirectory = Files.createTempDirectory("ocelot");

        Lock readLock = mock(Lock.class);
        Lock writeLock = mock(Lock.class);

        accessor = new WorkingDirectoryAccessor(readLock, writeLock, tempDirectory);
    }

    @AfterEach
    public void afterEach() throws IOException {
        FileUtils.deleteDirectory(tempDirectory.toFile());
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
    class WriteAgentMappings {

        @Test
        public void writeAgentMappings() throws IOException {
            accessor.writeAgentMappings("new content");

            Optional<String> result = accessor.readAgentMappings();

            assertThat(result).hasValue("new content");
        }

        @Test
        public void overwriteAgentMappings() throws IOException {
            createTestFiles("agent_mappings.yaml=old content");

            Optional<String> before = accessor.readAgentMappings();

            accessor.writeAgentMappings("new content");

            Optional<String> after = accessor.readAgentMappings();

            assertThat(before).hasValue("old content");
            assertThat(after).hasValue("new content");
        }
    }

    @Nested
    class DeleteConfiguration {

        @Test
        public void deleteNonExistingFile() {
            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> accessor.deleteConfiguration("first.yml"))
                    .withMessageStartingWith("Path cannot be deleted because it does not exist: ");
        }

        @Test
        public void deleteRoot() {
            createTestFiles("files/file.yml");

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> accessor.deleteConfiguration("."))
                    .withMessageStartingWith("Cannot delete base directory: .");
        }

        @Test
        public void deleteRootTraversal() {
            createTestFiles("files/file.yml");

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> accessor.deleteConfiguration("./dummy/.."))
                    .withMessageStartingWith("Cannot delete base directory: .");
        }

        @Test
        public void deleteFile() throws IOException {
            createTestFiles("files/first.yml");

            List<FileInfo> before = accessor.listConfigurationFiles("");

            accessor.deleteConfiguration("first.yml");

            List<FileInfo> after = accessor.listConfigurationFiles("");

            assertThat(before).hasSize(1);
            assertThat(after).isEmpty();
        }

        @Test
        public void deleteDirectory() throws IOException {
            createTestFiles("files/sub/first.yml");

            List<FileInfo> before = accessor.listConfigurationFiles("sub");

            accessor.deleteConfiguration("sub");

            List<FileInfo> after = accessor.listConfigurationFiles("");

            assertThat(before).hasSize(1);
            assertThat(after).isEmpty();
        }
    }

    @Nested
    class WriteConfigurationFile {

        @Test
        public void writeFile() throws IOException {
            List<FileInfo> before = accessor.listConfigurationFiles("");

            accessor.writeConfigurationFile("first.yml", "new content");

            List<FileInfo> after = accessor.listConfigurationFiles("");
            Optional<String> fileContent = accessor.readConfigurationFile("first.yml");

            assertThat(before).isEmpty();
            assertThat(after).isNotEmpty()
                    .anySatisfy(fileInfo -> {
                        assertThat(fileInfo.getName()).isEqualTo("first.yml");
                        assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(fileInfo.getChildren()).isNull();
                    });

            assertThat(fileContent).hasValue("new content");
        }

        @Test
        public void overwriteFile() throws IOException {
            createTestFiles("files/first.yml=old content");

            Optional<String> before = accessor.readConfigurationFile("first.yml");

            accessor.writeConfigurationFile("first.yml", "new content");

            Optional<String> after = accessor.readConfigurationFile("first.yml");

            assertThat(before).hasValue("old content");
            assertThat(after).hasValue("new content");
        }

        @Test
        public void fileIsDirectory() {
            createTestFiles("files/sub/first.yml");

            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> accessor.writeConfigurationFile("sub", "new content"))
                    .withMessageStartingWith("Cannot write file because target is already a directory: ");
        }
    }

    @Nested
    class MoveConfiguration {

        @Test
        public void moveFile() throws IOException {
            createTestFiles("files/first.yml=my file");

            Optional<String> beforeA = accessor.readConfigurationFile("first.yml");
            Optional<String> beforeB = accessor.readConfigurationFile("sub/moved.yml");

            accessor.moveConfiguration("first.yml", "sub/moved.yml");

            Optional<String> afterA = accessor.readConfigurationFile("first.yml");
            Optional<String> afterB = accessor.readConfigurationFile("sub/moved.yml");

            assertThat(beforeA).hasValue("my file");
            assertThat(beforeB).isEmpty();
            assertThat(afterA).isEmpty();
            assertThat(afterB).hasValue("my file");
        }

        @Test
        public void moveDirectory() throws IOException {
            createTestFiles("files/sub_a/file.yml=my file");

            Optional<String> beforeA = accessor.readConfigurationFile("sub_a/file.yml");
            Optional<String> beforeB = accessor.readConfigurationFile("sub_b/file.yml");

            accessor.moveConfiguration("sub_a", "sub_b");

            Optional<String> afterA = accessor.readConfigurationFile("sub_a/file.yml");
            Optional<String> afterB = accessor.readConfigurationFile("sub_b/file.yml");

            assertThat(beforeA).hasValue("my file");
            assertThat(beforeB).isEmpty();
            assertThat(afterA).isEmpty();
            assertThat(afterB).hasValue("my file");
        }
    }

    @Nested
    class CreateConfigurationDirectory {

        @Test
        public void createDirectory() throws IOException {
            accessor.createConfigurationDirectory("test");

            Path targetDirectory = tempDirectory.resolve("files/test");
            assertThat(targetDirectory).exists();
        }

        @Test
        public void alreadyExists() {
            createTestFiles("files/test/file");

            assertThatExceptionOfType(FileAlreadyExistsException.class)
                    .isThrownBy(() -> accessor.createConfigurationDirectory("test"))
                    .withMessageStartingWith("Directory already exists:");
        }

        @Test
        public void fileWithSameName() {
            createTestFiles("files/test");

            assertThatExceptionOfType(FileAlreadyExistsException.class)
                    .isThrownBy(() -> accessor.createConfigurationDirectory("test"))
                    .withMessageStartingWith("Directory already exists:");
        }

        @Test
        public void invalidLocation() {
            createTestFiles("test/file");

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> accessor.createConfigurationDirectory("../test"))
                    .withMessageStartingWith("User path escapes the base path:");
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