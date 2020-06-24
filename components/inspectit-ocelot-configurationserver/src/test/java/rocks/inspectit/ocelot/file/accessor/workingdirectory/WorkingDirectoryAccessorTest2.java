package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.file.FileChangedEvent;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileTestBase;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessorTest;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkingDirectoryAccessorTest2 extends AbstractFileAccessorTest<WorkingDirectoryAccessor> {

    private ApplicationEventPublisher eventPublisher;

    @Override
    protected WorkingDirectoryAccessor createInstance() throws Exception {
        tempDirectory = Files.createTempDirectory("ocelot");

        eventPublisher = mock(ApplicationEventPublisher.class);

        return new WorkingDirectoryAccessor(tempDirectory, eventPublisher);
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

            List<FileInfo> before = accessor.listConfigurationFiles("");

            accessor.deleteConfiguration("first.yml");

            List<FileInfo> after = accessor.listConfigurationFiles("");

            assertThat(before).hasSize(1);
            assertThat(after).isEmpty();

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void deleteDirectory() throws IOException {
            createTestFiles("files/sub/first.yml");

            List<FileInfo> before = accessor.listConfigurationFiles("sub");

            accessor.deleteConfiguration("sub");

            List<FileInfo> after = accessor.listConfigurationFiles("");

            assertThat(before).hasSize(1);
            assertThat(after).isEmpty();

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
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
                    .withMessageStartingWith("User path escapes the base path:");
        }
    }
}