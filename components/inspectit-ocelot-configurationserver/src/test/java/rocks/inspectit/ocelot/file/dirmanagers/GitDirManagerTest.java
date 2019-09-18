package rocks.inspectit.ocelot.file.dirmanagers;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
public class GitDirManagerTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");

    @InjectMocks
    private GitDirManager gdm;

    private GitProvider gp;

    @Mock
    private WorkingDirManager wdm;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @BeforeEach
    private void setupFileManager() throws Exception {
        Files.createDirectories(rootWorkDir);
        InspectitServerSettings conf = new InspectitServerSettings();
        conf.setWorkingDirectory(fmRoot.toString());
        gp = new GitProvider();
        gp.config = conf;
        gp.init();
        //   gdm.workingDirManager = wdm;
        gdm.gitProvider = gp;
        gdm.config = conf;
        gdm.init();
    }

    @AfterEach
    private void cleanFileManager() throws Exception {
        deleteDirectory(rootWorkDir);
    }

    private static void setupTestFiles(boolean specialFiles, String... paths) {
        try {
            for (String path : paths) {
                if (!specialFiles) {
                    if (path.contains("/")) {
                        String[] pathArray = path.split("/");
                        Files.createDirectories(fmRoot.resolve(WorkingDirManager.FILES_SUBFOLDER).resolve(pathArray[0]));
                        String newPaths = WorkingDirManager.FILES_SUBFOLDER + "/" + pathArray[0];
                        Files.createFile(fmRoot.resolve(newPaths).resolve(pathArray[1]));
                    } else {
                        Files.createFile(fmRoot.resolve(WorkingDirManager.FILES_SUBFOLDER).resolve(path));
                    }
                } else {
                    if (path.contains("/")) {
                        String[] pathArray = path.split("/");
                        Files.createDirectories(fmRoot.resolve(pathArray[0]));
                        String newPaths = pathArray[0];
                        Files.createFile(fmRoot.resolve(newPaths).resolve(pathArray[1]));
                    } else {
                        Files.createFile(fmRoot.resolve(path));
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        List<Path> files = Files.walk(path).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(files);
        for (Path f : files) {
            File file = new File(f.toString());
            file.delete();
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
    public class CommitAllChanges {
        @Test
        void testCommit() throws IOException, GitAPIException {
            List<String> beforeCommit = gdm.listFiles();
            setupTestFiles(false, "a", "b", "c");
            List<String> afterCommit = Arrays.asList("a", "b", "c");

            gdm.commitAllChanges();
            assertThat(beforeCommit).isNotEqualTo(afterCommit);
            assertThat(gdm.listFiles()).isEqualTo(afterCommit);
        }
    }

    @Nested
    public class ListFiles {
        @Test
        void listEmptyRepo() throws IOException {
            List<String> emptyList = Arrays.asList();

            assertThat(gdm.listFiles()).isEqualTo(emptyList);
        }

        @Test
        void listRepoTest() throws GitAPIException, IOException {
            setupTestFiles(false, "a", "b", "c");
            gdm.commitAllChanges();
            List<String> output = Arrays.asList("a", "b", "c");

            assertThat(gdm.listFiles()).isEqualTo(output);
        }
    }

    @Nested
    public class ReadFile {
        @Test
        void readFile() throws IOException, GitAPIException {
            createFileWithContent("hello", "world");
            gdm.commitAllChanges();

            assertThat(gdm.readFile("hello")).isEqualTo("world");
        }
    }

    @Nested
    public class ReadAgentMappingFiles {
        @Test
        void readAgentMappingFile() throws GitAPIException, IOException {
            createFileWithContent("../agent_mappings.yaml", "Hello World!");
            gdm.commitAllChanges();

            assertThat(gdm.readAgentMappingFile()).isEqualTo("Hello World!");
        }
    }

    @Nested
    public class CommitAgentMappingFiles {
        @Test
        void newFileCreated() throws IOException, GitAPIException {
            createFileWithContent("../agent_mappings.yaml", "Hello World!");
            gdm.commitAllChanges();
            createFileWithContent("../agent_mappings.yaml", "This is not an easter egg!");
            createFileWithContent("dummyFile", "But this is one =)");

            gdm.commitAgentMappingFile();
            String agentMappingContent = gdm.readAgentMappingFile();
            String dummyFileContent = gdm.readFile("dummyFile");
            boolean agentMappingChanged = "This is not an easter egg!".equals(agentMappingContent);
            boolean dummyFileNotChanged = dummyFileContent == null;
            assertThat(agentMappingChanged && dummyFileNotChanged).isEqualTo(true);

        }

        @Test
        void commitAgentMappingOnly() throws IOException, GitAPIException {
            createFileWithContent("../agent_mappings.yaml", "Hello World!");
            createFileWithContent("dummyFile", "Hello User!");
            gdm.commitAllChanges();
            createFileWithContent("../agent_mappings.yaml", "This is not an easter egg!");
            createFileWithContent("dummyFile", "But this is one =)");

            gdm.commitAgentMappingFile();
            String agentMappingContent = gdm.readAgentMappingFile();
            String dummyFileContent = gdm.readFile("dummyFile");
            boolean agentMappingChanged = "This is not an easter egg!".equals(agentMappingContent);
            boolean dummyFileNotChanged = "Hello User!".equals(dummyFileContent);
            assertThat(agentMappingChanged && dummyFileNotChanged).isEqualTo(true);

        }

    }

    @Nested
    public class CommitFiles {
        @Test
        void commitOnlyFiles() throws IOException, GitAPIException {
            createFileWithContent("../agent_mappings.yaml", "Hello World!");
            createFileWithContent("dummyFile", "Hello User!");
            gdm.commitAllChanges();
            createFileWithContent("../agent_mappings.yaml", "This is not an easter egg!");
            createFileWithContent("dummyFile", "But this is one =)");

            gdm.commitFiles();
            String agentMappingContent = gdm.readAgentMappingFile();
            String dummyFileContent = gdm.readFile("dummyFile");
            boolean agentMappingChanged = "Hello World!".equals(agentMappingContent);
            boolean dummyFileNotChanged = "But this is one =)".equals(dummyFileContent);
            assertThat(agentMappingChanged && dummyFileNotChanged).isEqualTo(true);
        }
    }

}
