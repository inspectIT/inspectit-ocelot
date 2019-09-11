package rocks.inspectit.ocelot.file.dirmanagers;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class WorkingDirManagerTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");

    @InjectMocks
    private WorkingDirManager wdm;

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
        wdm.config = conf;
        wdm.init();
    }

    @AfterEach
    private void cleanFileManager() throws Exception {
        deleteDirectory(fmRoot);
    }

    private static void setupTestFiles(String... paths) {
        try {
            for (String path : paths) {
                if (!path.contains("=")) {
                    if (path.contains("/")) {
                        String[] pathArray = path.split("/");
                        Files.createDirectories(fmRoot.resolve(WorkingDirManager.FILES_SUBFOLDER).resolve(pathArray[0]));
                        String newPaths = WorkingDirManager.FILES_SUBFOLDER + "/" + pathArray[0];
                        Files.createFile(fmRoot.resolve(newPaths).resolve(pathArray[1]));
                    } else {
                        Files.createFile(fmRoot.resolve(WorkingDirManager.FILES_SUBFOLDER).resolve(path));
                    }
                } else {
                    String[] splitted = path.split("=");
                    Path file = fmRoot.resolve(WorkingDirManager.FILES_SUBFOLDER).resolve(splitted[0]);
                    Files.createDirectories(file.getParent());
                    String content = splitted.length > 1 ? splitted[1] : "";
                    Files.write(file, content.getBytes(WorkingDirManager.ENCODING));
                }
            }
        } catch (Exception e) {
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        List<Path> files = Files.walk(path).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(files);
        for (Path f : files) {
            Files.delete(f);
        }
    }

    private static void createFileWithContent(String path, String content) throws IOException {
        File f;
        if (path.contains("/")) {
            String paths[] = path.split("/");
            Files.createDirectories(fmRoot.resolve(WorkingDirManager.FILES_SUBFOLDER).resolve(paths[0]));
            String finalPath = WorkingDirManager.FILES_SUBFOLDER + "/" + paths[0];
            f = new File(String.valueOf(fmRoot.resolve(finalPath).resolve(paths[1])));

        } else {
            f = new File(String.valueOf(fmRoot.resolve(WorkingDirManager.FILES_SUBFOLDER).resolve(path)));
        }
        FileWriter fw = new FileWriter(f);
        fw.write(content);
        fw.flush();
        fw.close();
    }

    private static String readFile(String path) throws IOException {
        Path file = fmRoot.resolve(WorkingDirManager.FILES_SUBFOLDER).resolve(path);
        return new String(Files.readAllBytes(file), WorkingDirManager.ENCODING);
    }


    @Nested
    public class ReadFile {
        @Test
        void readTopLevelFile() throws IOException {
            createFileWithContent("testFile", "testContent");

            assertThat(wdm.readFile("testFile")).isEqualTo("testContent");
        }

        @Test
        void readSubfolderFile() throws IOException {
            createFileWithContent("directory/testFile", "testContent");

            assertThat(wdm.readFile("directory/testFile")).isEqualTo("testContent");
        }
    }

    @Nested
    public class WriteFile {
        @Test
        void writeTopLevelFile() throws IOException {
            wdm.writeFile("name", "content");

            assertThat(readFile("name")).isEqualTo("content");
        }

        @Test
        void writeFileAndSubfolder() throws IOException {
            wdm.writeFile("dir/name", "content");

            assertThat(readFile("dir/name")).isEqualTo("content");
        }


    }

    @Nested
    public class ListFiles {
        @Test
        void listFilesTopLevel() throws IOException {
            setupTestFiles("a", "b", "c");
            List<String> output = Arrays.asList("a", "b", "c");

            assertThat(wdm.listFiles("")).isEqualTo(output);
        }

        @Test
        void listFilesSubFolder() throws IOException {
            setupTestFiles("directory/a");
            List<String> output = Arrays.asList("directory/a");

            assertThat(wdm.listFiles("")).isEqualTo(output);
        }

    }
}
