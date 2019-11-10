package rocks.inspectit.ocelot.file;


import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.dirmanagers.GitDirectoryManager;
import rocks.inspectit.ocelot.file.dirmanagers.WorkingDirectoryManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileManagerTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");
    private static final Path filesRoot = fmRoot.resolve("files");

    private static final Charset ENCODING = StandardCharsets.UTF_8;
    static final String FILES_SUBFOLDER = "files/configuration";

    @InjectMocks
    private FileManager fm;

    @Mock
    private GitDirectoryManager gdm;

    @Mock
    private WorkingDirectoryManager wdm;


    @Mock
    ApplicationEventPublisher eventPublisher;

    @BeforeAll
    private static void setup() throws Exception {
        Files.createDirectories(rootWorkDir);
    }

    @AfterAll
    private static void clean() throws Exception {
        deleteDirectory(rootWorkDir);
    }

    @BeforeEach
    private void setupFileManager() throws Exception {
        InspectitServerSettings conf = new InspectitServerSettings();
        conf.setWorkingDirectory(fmRoot.toString());
        fm.config = conf;
        fm.init();
    }

    @AfterEach
    private void cleanFileManager() throws Exception {
        deleteDirectory(fmRoot);
    }

    private static void setupTestFiles(String... paths) {
        try {
            for (String path : paths) {
                if (!path.contains("=")) {
                    Files.createDirectories(fmRoot.resolve(FileManager.FILES_SUBFOLDER).resolve(path));
                } else {
                    String[] splitted = path.split("=");
                    Path file = fmRoot.resolve(FileManager.FILES_SUBFOLDER).resolve(splitted[0]);
                    Files.createDirectories(file.getParent());
                    String content = splitted.length > 1 ? splitted[1] : "";
                    Files.write(file, content.getBytes(FileManager.ENCODING));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        List<Path> files = Files.walk(path).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(files);
        for (Path f : files) {
            Files.delete(f);
        }
    }

    @FunctionalInterface
    private interface Verfification<T> {
        void verify(T t) throws Exception;
    }

    @Nested
    class ReadSpecialFile {

        @Test
        void readEmptyFileVersioning() throws Exception {
            setupTestFiles("fileA=", "fileB=something");
            when(gdm.readFile(any())).thenReturn("");

            assertThat(fm.readSpecialFile("fileA", true)).isEqualTo("");
            assertThat(fm.readSpecialFile("./fileA", true)).isEqualTo("");
        }

        @Test
        void readNonEmptyFileVersioning() throws Exception {
            setupTestFiles("fileA=", "sub/fileB=something\nsomething else");
            when(gdm.readFile(any())).thenReturn("something\nsomething else");

            assertThat(fm.readSpecialFile("sub/fileB", true)).isEqualTo("something\nsomething else");
            assertThat(fm.readSpecialFile("./sub/../sub/fileB", true)).isEqualTo("something\nsomething else");
        }

        @Test
        void readEmptyFile() throws Exception {
            setupTestFiles("fileA=", "fileB=something");
            when(wdm.readFile(any())).thenReturn("");

            assertThat(fm.readSpecialFile("fileA", false)).isEqualTo("");
            assertThat(fm.readSpecialFile("./fileA", false)).isEqualTo("");
        }

        @Test
        void readNonEmpty() throws Exception {
            setupTestFiles("fileA=", "sub/fileB=something\nsomething else");
            when(wdm.readFile(any())).thenReturn("something\nsomething else");

            assertThat(fm.readSpecialFile("sub/fileB", false)).isEqualTo("something\nsomething else");
            assertThat(fm.readSpecialFile("./sub/../sub/fileB", false)).isEqualTo("something\nsomething else");
        }
    }
}
