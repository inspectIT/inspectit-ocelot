package rocks.inspectit.ocelot.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FileInfoVisitorTest {

    private List<Path> testFiles;

    @BeforeEach
    public void beforeEach() {
        testFiles = new LinkedList<>();
    }

    @AfterEach
    public void afterEach() throws IOException {
        for (Path file : testFiles) {
            Files.delete(file);
        }
    }

    private Path createTestFile(String test) throws IOException {
        Path tempFile = Files.createTempFile("ocelot", ".yml");
        Files.write(tempFile, test.getBytes(StandardCharsets.UTF_8));
        testFiles.add(tempFile);
        return tempFile;
    }

    /**
     * root@inspectit-ocelot-configuration-server-6d58469594-5kk5z:/configuration-server/files# ls -lah
     * ...
     * drwxr-xr-x 2 root root 4.0K Sep  1 12:15 ..2021_09_01_12_15_55.274058932
     * lrwxrwxrwx 1 root root   31 Sep  1 12:15 ..data -> ..2021_09_01_12_15_55.274058932
     * lrwxrwxrwx 1 root root   16 Sep  1 12:15 some_instrumentation.yml -> ..data/some_instrumentation.yml
     *
     * In words:
     * The folder ..2021_09_01_12_15_55.274058932 contains the some_instrumentation.yml
     * On the root path, the mysql.yml is a sym link to ..data/some_instrumentation.yml, whereby ..data itself is
     * a symlink to the folder ..2021_09_01_12_15_55.274058932
     *
     */
    private Path createK8sConfigMapScenario(String test) throws IOException {
        Path configDir = Files.createTempDirectory("configDir");
        Path tempFile = Files.createTempFile(configDir, "ocelot", ".yml");

        Files.write(tempFile, test.getBytes(StandardCharsets.UTF_8));

        Path symLinkFromDataToConfigFolder = Paths.get(configDir.getParent().toString(), "..data");
        Files.createSymbolicLink(symLinkFromDataToConfigFolder, configDir.getFileName());

        Path symLinkConfigFilenameToConfigFile = Paths.get(configDir.getParent().toString(), "some_instrumentation.yml");
        Files.createSymbolicLink(symLinkConfigFilenameToConfigFile, Paths.get("..data", tempFile.getFileName().toString()));

        // house keeping
        testFiles.add(symLinkConfigFilenameToConfigFile);
        testFiles.add(symLinkFromDataToConfigFolder);
        testFiles.add(tempFile);
        testFiles.add(configDir);

        return symLinkConfigFilenameToConfigFile;
    }

    private Path createTestFileInHiddenFolder() throws IOException {
        Path hiddenFolder = Files.createTempDirectory("..configDir");

        testFiles.add(hiddenFolder);

        return hiddenFolder;
    }

    @Nested
    class VisitFile {

        @Test
        public void visitStandardFile() throws IOException {
            Path testFile = createTestFile("test");
            FileInfoVisitor visitor = new FileInfoVisitor();

            visitor.preVisitDirectory(testFile.getParent(), null);
            visitor.visitFile(testFile, null);

            List<FileInfo> result = visitor.getFileInfos();

            assertThat(result).hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType)
                    .contains(tuple(testFile.getFileName().toString(), FileInfo.Type.FILE));
        }

        @Test
        public void visitUiFile() throws IOException {
            Path testFile = createTestFile("# {\"type\": \"method-configuration\"}");
            FileInfoVisitor visitor = new FileInfoVisitor();

            visitor.preVisitDirectory(testFile.getParent(), null);
            visitor.visitFile(testFile, null);

            List<FileInfo> result = visitor.getFileInfos();

            assertThat(result).hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType)
                    .contains(tuple(testFile.getFileName().toString(), FileInfo.Type.UI_METHOD_CONFIGURATION));
        }

        @Test
        public void visitStandardFile_standardComment() throws IOException {
            Path testFile = createTestFile("# hello");
            FileInfoVisitor visitor = new FileInfoVisitor();

            visitor.preVisitDirectory(testFile.getParent(), null);
            visitor.visitFile(testFile, null);

            List<FileInfo> result = visitor.getFileInfos();

            assertThat(result).hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType)
                    .contains(tuple(testFile.getFileName().toString(), FileInfo.Type.FILE));
        }

        @Test
        public void visitUiFile_emptyMap() throws IOException {
            Path testFile = createTestFile("# {}");
            FileInfoVisitor visitor = new FileInfoVisitor();

            visitor.preVisitDirectory(testFile.getParent(), null);
            visitor.visitFile(testFile, null);

            List<FileInfo> result = visitor.getFileInfos();

            assertThat(result).hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType)
                    .contains(tuple(testFile.getFileName().toString(), FileInfo.Type.FILE));
        }

        @Test
        public void skipSubtreeIfHiddenFolder() throws IOException {
            Path hiddenFolder = createTestFileInHiddenFolder();
            FileInfoVisitor visitor = new FileInfoVisitor();

            FileVisitResult result = visitor.preVisitDirectory(hiddenFolder, null);

            assertThat(result.equals(FileVisitResult.SKIP_SUBTREE));
        }

        @Test
        public void visitSymbolicLink() throws IOException {
            Path symbolicLink = createK8sConfigMapScenario("# {}");
            FileInfoVisitor visitor = new FileInfoVisitor();

            visitor.preVisitDirectory(symbolicLink.getParent(), null);
            visitor.visitFile(symbolicLink, null);

            List<FileInfo> result = visitor.getFileInfos();

            assertThat(result).hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType)
                    .contains(tuple(symbolicLink.getFileName().toString(), FileInfo.Type.FILE));
        }

    }

}
