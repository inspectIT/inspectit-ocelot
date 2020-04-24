package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileChangedEvent;
import rocks.inspectit.ocelot.file.FileInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkingDirectoryAccessorTest {

    private AbstractWorkingDirectoryAccessor accessor;

    private InspectitServerSettings settings;

    private ApplicationEventPublisher eventPublisher;

    private Path tempDirectory;

    @BeforeEach
    public void beforeEach() throws IOException {
        tempDirectory = Files.createTempDirectory("ocelot");

        settings = new InspectitServerSettings();
        settings.setWorkingDirectory(tempDirectory.toString());

        eventPublisher = mock(ApplicationEventPublisher.class);

        accessor = new WorkingDirectoryAccessor(settings, eventPublisher);
    }

    @AfterEach
    public void afterEach() throws IOException {
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }

    private void createTestFiles(String... files) {
        try {
            for (String file : files) {
                String path;
                String content;
                if (file.contains("=")) {
                    String[] splitted = file.split("=");
                    path = splitted[0];
                    content = splitted.length == 2 ? splitted[1] : "";
                } else {
                    path = file;
                    content = "";
                }

                Path targetFile = tempDirectory.resolve(path);
                Files.createDirectories(targetFile.getParent());
                Files.write(targetFile, content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                    .withMessage("User path escapes the base path: ..\\test.yml");
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
            createTestFiles("one.yml", "files/second.yml", "files/sub/third.yml");

            Optional<List<FileInfo>> result = accessor.listConfigurationFiles(".");

            assertThat(result).isNotEmpty();

            List<FileInfo> files = result.get();
            assertThat(files).hasSize(2);
            assertThat(files).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("second.yml");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(fileInfo.getChildren()).isNull();
            });
            assertThat(files).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("sub");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                assertThat(fileInfo.getChildren()).hasSize(1);

                FileInfo child = fileInfo.getChildren().get(0);

                assertThat(child.getName()).isEqualTo("third.yml");
                assertThat(child.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(child.getChildren()).isNull();
            });
        }

        @Test
        public void listNestedFiles() {
            createTestFiles("one.yml", "files/second.yml", "files/sub/third.yml");

            Optional<List<FileInfo>> result = accessor.listConfigurationFiles("sub");

            assertThat(result).isNotEmpty();

            List<FileInfo> files = result.get();
            assertThat(files).hasSize(1);
            assertThat(files).anySatisfy(fileInfo -> {
                assertThat(fileInfo.getName()).isEqualTo("third.yml");
                assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                assertThat(fileInfo.getChildren()).isNull();
            });
        }
    }

    @Nested
    class WriteAgentMappings {

        @Test
        public void writeAgentMappings() throws IOException {
            accessor.writeAgentMappings("new content");

            Optional<String> result = accessor.readAgentMappings();

            assertThat(result).hasValue("new content");

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void overwriteAgentMappings() throws IOException {
            createTestFiles("agent_mappings.yaml=old content");

            Optional<String> before = accessor.readAgentMappings();

            accessor.writeAgentMappings("new content");

            Optional<String> after = accessor.readAgentMappings();

            assertThat(before).hasValue("old content");
            assertThat(after).hasValue("new content");

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }
    }

    @Nested
    class DeleteConfiguration {

        @Test
        public void deleteNonExistingFile() {
            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> accessor.deleteConfiguration("first.yml"))
                    .withMessageStartingWith("Path cannot be deleted because it does not exist: ");

            verifyZeroInteractions(eventPublisher);
        }

        @Test
        public void deleteRoot() {
            createTestFiles("files/file.yml");

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> accessor.deleteConfiguration("."))
                    .withMessageStartingWith("Cannot delete base directory: .");

            verifyZeroInteractions(eventPublisher);
        }

        @Test
        public void deleteRootTraversal() {
            createTestFiles("files/file.yml");

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> accessor.deleteConfiguration("./dummy/.."))
                    .withMessageStartingWith("Cannot delete base directory: .");

            verifyZeroInteractions(eventPublisher);
        }

        @Test
        public void deleteFile() throws IOException {
            createTestFiles("files/first.yml");

            Optional<List<FileInfo>> before = accessor.listConfigurationFiles("");

            accessor.deleteConfiguration("first.yml");

            Optional<List<FileInfo>> after = accessor.listConfigurationFiles("");

            assertThat(before.get()).hasSize(1);
            assertThat(after.get()).isEmpty();

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void deleteDirectory() throws IOException {
            createTestFiles("files/sub/first.yml");

            Optional<List<FileInfo>> before = accessor.listConfigurationFiles("sub");

            accessor.deleteConfiguration("sub");

            Optional<List<FileInfo>> after = accessor.listConfigurationFiles("");

            assertThat(before.get()).hasSize(1);
            assertThat(after.get()).isEmpty();

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }
    }

    @Nested
    class WriteConfigurationFile {

        @Test
        public void writeFile() throws IOException {
            Optional<List<FileInfo>> before = accessor.listConfigurationFiles("");

            accessor.writeConfigurationFile("first.yml", "new content");

            Optional<List<FileInfo>> after = accessor.listConfigurationFiles("");
            Optional<String> fileContent = accessor.readConfigurationFile("first.yml");

            assertThat(before.get()).isEmpty();
            assertThat(after.get()).isNotEmpty()
                    .anySatisfy(fileInfo -> {
                        assertThat(fileInfo.getName()).isEqualTo("first.yml");
                        assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(fileInfo.getChildren()).isNull();
                    });

            assertThat(fileContent).hasValue("new content");

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void overwriteFile() throws IOException {
            createTestFiles("files/first.yml=old content");

            Optional<String> before = accessor.readConfigurationFile("first.yml");

            accessor.writeConfigurationFile("first.yml", "new content");

            Optional<String> after = accessor.readConfigurationFile("first.yml");

            assertThat(before).hasValue("old content");
            assertThat(after).hasValue("new content");

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void fileIsDirectory() {
            createTestFiles("files/sub/first.yml");

            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> accessor.writeConfigurationFile("sub", "new content"))
                    .withMessageStartingWith("Cannot write file because target is already a directory: ");

            verifyZeroInteractions(eventPublisher);
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

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
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

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
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
                    .withMessage("User path escapes the base path: ..\\test");
        }
    }
}