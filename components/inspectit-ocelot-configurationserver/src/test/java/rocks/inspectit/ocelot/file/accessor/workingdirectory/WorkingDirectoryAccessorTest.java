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
import rocks.inspectit.ocelot.file.accessor.DirectoryTraversalException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

            Optional<String> result = accessor.readConfigurationFile("test.yml");

            assertThat(result).hasValue("content");
        }

        @Test
        public void readNestedFile() {
            createTestFiles("files/sub/test.yml=content");

            Optional<String> result = accessor.readConfigurationFile("sub/test.yml");

            assertThat(result).hasValue("content");
        }

        @Test
        public void traversalException() {
            createTestFiles("files/test.yml");

            assertThatExceptionOfType(DirectoryTraversalException.class)
                    .isThrownBy(() -> accessor.readConfigurationFile("sub/../test.yml"));
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
        public void writeAgentMappings() {
            accessor.writeAgentMappings("new content");

            Optional<String> result = accessor.readAgentMappings();

            assertThat(result).hasValue("new content");

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void overwriteAgentMappings() {
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
    class DeleteConfigurationFile {

        @Test
        public void deleteNonExistingFile() {
            boolean result = accessor.deleteConfigurationFile("first.yml");

            assertThat(result).isFalse();

            verifyZeroInteractions(eventPublisher);
        }

        @Test
        public void deleteFile() {
            createTestFiles("files/first.yml");

            Optional<List<FileInfo>> before = accessor.listConfigurationFiles("");

            boolean result = accessor.deleteConfigurationFile("first.yml");

            Optional<List<FileInfo>> after = accessor.listConfigurationFiles("");

            assertThat(result).isTrue();
            assertThat(before.get()).hasSize(1);
            assertThat(after.get()).isEmpty();

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }
    }

    @Nested
    class WriteConfigurationFile {

        @Test
        public void writeFile() {
            Optional<List<FileInfo>> before = accessor.listConfigurationFiles("");

            boolean result = accessor.writeConfigurationFile("first.yml", "new content");

            Optional<List<FileInfo>> after = accessor.listConfigurationFiles("");
            Optional<String> fileContent = accessor.readConfigurationFile("first.yml");

            assertThat(result).isTrue();
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
        public void overwriteFile() {
            createTestFiles("files/first.yml=old content");

            Optional<String> before = accessor.readConfigurationFile("first.yml");

            boolean result = accessor.writeConfigurationFile("first.yml", "new content");

            Optional<String> after = accessor.readConfigurationFile("first.yml");

            assertThat(result).isTrue();
            assertThat(before).hasValue("old content");
            assertThat(after).hasValue("new content");

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }
    }

    @Nested
    class MoveConfigurationFile {

        @Test
        public void moveFile() {
            createTestFiles("files/first.yml=my file");

            Optional<String> beforeA = accessor.readConfigurationFile("first.yml");
            Optional<String> beforeB = accessor.readConfigurationFile("sub/moved.yml");

            boolean result = accessor.moveConfigurationFile("first.yml", "sub/moved.yml");

            Optional<String> afterA = accessor.readConfigurationFile("first.yml");
            Optional<String> afterB = accessor.readConfigurationFile("sub/moved.yml");

            assertThat(result).isTrue();
            assertThat(beforeA).hasValue("my file");
            assertThat(beforeB).isEmpty();
            assertThat(afterA).isEmpty();
            assertThat(afterB).hasValue("my file");

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void moveDirectory() {
            createTestFiles("files/sub_a/file.yml=my file");

            Optional<String> beforeA = accessor.readConfigurationFile("sub_a/file.yml");
            Optional<String> beforeB = accessor.readConfigurationFile("sub_b/file.yml");

            boolean result = accessor.moveConfigurationFile("sub_a", "sub_b");

            Optional<String> afterA = accessor.readConfigurationFile("sub_a/file.yml");
            Optional<String> afterB = accessor.readConfigurationFile("sub_b/file.yml");

            assertThat(result).isTrue();
            assertThat(beforeA).hasValue("my file");
            assertThat(beforeB).isEmpty();
            assertThat(afterA).isEmpty();
            assertThat(afterB).hasValue("my file");

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }
    }
}