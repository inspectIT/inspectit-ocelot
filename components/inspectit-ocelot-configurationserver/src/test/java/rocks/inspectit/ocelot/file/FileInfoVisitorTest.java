package rocks.inspectit.ocelot.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.*;

public class FileInfoVisitorTest {

    private Path createTestFile(String test) throws IOException {
        Path tempFile = Files.createTempFile("ocelot", ".yml");
        Files.write(tempFile, test.getBytes(StandardCharsets.UTF_8));
        return tempFile;
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

            Files.delete(testFile);

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

            Files.delete(testFile);

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

            Files.delete(testFile);

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

            Files.delete(testFile);

            assertThat(result).hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType)
                    .contains(tuple(testFile.getFileName().toString(), FileInfo.Type.FILE));
        }
    }

}