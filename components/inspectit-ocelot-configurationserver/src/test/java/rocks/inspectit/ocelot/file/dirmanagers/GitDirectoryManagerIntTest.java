package rocks.inspectit.ocelot.file.dirmanagers;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileVersionResponse;

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

public class GitDirectoryManagerIntTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");

    @Autowired
    private GitDirectoryManager gitDirectoryManager;

    @Autowired
    private VersionController versionController;

    private Git git;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @BeforeEach
    private void setupFileManager() throws Exception {
        Files.createDirectories(rootWorkDir);
        InspectitServerSettings conf = new InspectitServerSettings();
        conf.setWorkingDirectory(rootWorkDir.toString());
        versionController = new VersionController();
        versionController.config = conf;
        versionController.init();
        git = versionController.git;
        gitDirectoryManager = new GitDirectoryManager();
        gitDirectoryManager.versionController = versionController;
        gitDirectoryManager.config = conf;
        Files.createDirectory(rootWorkDir.resolve("files/configuration"));
    }

    @AfterEach
    private void cleanDirectory() throws Exception {
        deleteDirectory(rootWorkDir);
    }

    private ObjectId commitAll() throws GitAPIException {
        git.add()
                .addFilepattern(".")
                .call();
        RevCommit commit = git.commit()
                .setAll(true)
                .setAuthor("test", "test")
                .setMessage("testCommit")
                .call();
        git.reset();
        return commit.getId();
    }

    private static void setupTestFiles(boolean specialFiles, String... paths) {
        try {
            for (String path : paths) {
                if (!specialFiles) {
                    if (path.contains("/")) {
                        String[] pathArray = path.split("/");
                        Files.createDirectories(rootWorkDir.resolve("files/configuration").resolve(pathArray[0]));
                        String newPaths = "files/configuration/" + pathArray[0];
                        Files.createFile(rootWorkDir.resolve(newPaths).resolve(pathArray[1]));
                    } else {
                        Files.createFile(rootWorkDir.resolve("files/configuration").resolve(path));
                    }
                } else {
                    if (path.contains("/")) {
                        String[] pathArray = path.split("/");
                        Files.createDirectories(rootWorkDir.resolve(pathArray[0]));
                        String newPaths = pathArray[0];
                        Files.createFile(rootWorkDir.resolve(newPaths).resolve(pathArray[1]));
                    } else {
                        Files.createFile(rootWorkDir.resolve(path));
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
            Files.createDirectories(rootWorkDir.resolve("files/configuration").resolve(paths[0]));
            String finalPath = "files/configuration/" + paths[0];
            f = new File(String.valueOf(rootWorkDir.resolve(finalPath).resolve(paths[1])));

        } else {
            f = new File(String.valueOf(rootWorkDir.resolve("files/configuration").resolve(path)));
        }
        FileWriter fw = new FileWriter(f);
        fw.write(content);
        fw.flush();
        fw.close();
    }

    private static String readFile(String path) throws IOException {
        Path file = rootWorkDir.resolve("files/configuration").resolve(path);
        return new String(Files.readAllBytes(file), WorkingDirectoryManager.ENCODING);
    }

    private static FileInfo buildFileInfo(String name, String type) {
        FileInfo.Type fileType = FileInfo.Type.FILE;
        if (!type.equals("file")) {
            fileType = FileInfo.Type.DIRECTORY;
        }
        FileInfo.FileInfoBuilder builder = FileInfo.builder()
                .name(name)
                .type(fileType);
        return builder.build();
    }

    @Nested
    public class CommitAllChanges {
        @Test
        void testCommit() throws IOException, GitAPIException {
            List<FileInfo> beforeCommit = gitDirectoryManager.listFiles("", true);
            setupTestFiles(false, "a", "b", "c");
            FileInfo file1 = buildFileInfo("configuration/a", "file");
            FileInfo file2 = buildFileInfo("configuration/b", "file");
            FileInfo file3 = buildFileInfo("configuration/c", "file");
            List<FileInfo> afterCommit = Arrays.asList(file1, file2, file3);

            gitDirectoryManager.commitAllChanges();

            assertThat(beforeCommit).isNotEqualTo(afterCommit);
            assertThat(gitDirectoryManager.listFiles("", true)).isEqualTo(afterCommit);
        }
    }

    @Nested
    public class ListFiles {
        @Test
        void listEmptyRepo() throws IOException {
            List<String> emptyList = Arrays.asList();

            List<FileInfo> output = gitDirectoryManager.listFiles("", true);

            assertThat(output).isEqualTo(emptyList);
        }

        @Test
        void listRepoTest() throws GitAPIException, IOException {
            setupTestFiles(false, "a", "b", "c");
            commitAll();
            FileInfo file1 = buildFileInfo("configuration/a", "file");
            FileInfo file2 = buildFileInfo("configuration/b", "file");
            FileInfo file3 = buildFileInfo("configuration/c", "file");
            List<FileInfo> expected = Arrays.asList(file1, file2, file3);

            List<FileInfo> output = gitDirectoryManager.listFiles("", true);

            assertThat(output).isEqualTo(expected);
        }
    }

    @Nested
    public class ReadFile {
        @Test
        void readFile() throws IOException, GitAPIException {
            createFileWithContent("hello", "world");
            commitAll();

            String output = gitDirectoryManager.readFile("configuration/hello");

            assertThat(output).isEqualTo("world");
        }
    }

    @Nested
    public class CommitFiles {
        @Test
        void commitOnlyFiles() throws IOException, GitAPIException {
            createFileWithContent("../agent_mappings.yaml", "Hello World!");
            createFileWithContent("dummyFile", "Hello User!");
            commitAll();
            createFileWithContent("../agent_mappings.yaml", "This is not an easter egg!");
            createFileWithContent("dummyFile", "But this is one =)");

            gitDirectoryManager.commitFiles();

            String agentMappingContent = gitDirectoryManager.readFile("agent_mappings.yaml");
            String dummyFileContent = gitDirectoryManager.readFile("configuration/dummyFile");
            boolean agentMappingChanged = "Hello World!".equals(agentMappingContent);
            boolean dummyFileNotChanged = "But this is one =)".equals(dummyFileContent);
            assertThat(agentMappingChanged && dummyFileNotChanged).isEqualTo(true);
        }
    }

    @Nested
    public class GetAllCommits {
        @Test
        void onlyInitialCommitPresent() throws IOException, GitAPIException {
            List<FileVersionResponse> output = gitDirectoryManager.getAllCommits();

            assertThat(output.size()).isEqualTo(0);
        }

        @Test
        void multipleCommits() throws GitAPIException, IOException {
            createFileWithContent("../agent_mappings.yaml", "Hello World!");
            createFileWithContent("test", "Hello User!");
            commitAll();

            List<FileVersionResponse> output = gitDirectoryManager.getAllCommits();

            assertThat(output.size()).isEqualTo(1);
            FileVersionResponse testResponse = output.get(0);
            assertThat(((List) testResponse.getCommitContent()).contains("agent_mappings.yaml")).isEqualTo(true);
            assertThat(((List) testResponse.getCommitContent()).contains("configuration/test")).isEqualTo(true);

        }
    }

    @Nested
    public class GetFileFromVersion {
        @Test
        void commitExistsWithFile() throws IOException, GitAPIException {
            List<FileVersionResponse> hilfe = gitDirectoryManager.getAllCommits();
            createFileWithContent("dummyFile", "Hello User!");
            String commitId = commitAll().getName();

            String output = gitDirectoryManager.getFileFromVersion("configuration/dummyFile", commitId);

            assertThat(output).isEqualTo("Hello User!");
        }

        @Test
        void commitDoesNotContainFile() throws IOException, GitAPIException {
            createFileWithContent("dummyFile", "test");
            String commitId = commitAll().getName();

            String output = gitDirectoryManager.getFileFromVersion("configuration/aDifferentFile", commitId);

            assertThat(output).isEqualTo(null);
        }
    }

    @Nested
    public class GetCommitsOfFile {
        @Test
        void noFileCommits() throws IOException, GitAPIException {
            String filePath = "test";

            List<FileVersionResponse> output = gitDirectoryManager.getCommitsOfFile(filePath);

            assertThat(output).isEqualTo(Collections.emptyList());
        }

        @Test
        void fileCommitsForOtherFiles() throws IOException, GitAPIException {
            createFileWithContent("dummyFile", "test");
            commitAll();
            String filePath = "test";

            List<FileVersionResponse> output = gitDirectoryManager.getCommitsOfFile(filePath);

            assertThat(output).isEqualTo(Collections.emptyList());
        }

        @Test
        void commitsForFile() throws GitAPIException, IOException {
            String filePath = "configuration/dummyFile";
            createFileWithContent("dummyFile", "test");
            commitAll();

            List<FileVersionResponse> output = gitDirectoryManager.getCommitsOfFile(filePath);

            assertThat(output.size()).isEqualTo(1);
        }
    }

    @Nested
    public class CommitFile {
        @Test
        void commitSingleFile() throws IOException, GitAPIException {
            createFileWithContent("dummyFile", "test");
            createFileWithContent("anotherDummyFile", "another test");

            gitDirectoryManager.commitFile("configuration/dummyFile");

            assertThat(gitDirectoryManager.getCommitsOfFile("configuration/dummyFile").size()).isEqualTo(1);
            assertThat(gitDirectoryManager.getCommitsOfFile("configuration/anotherDummyFile").size()).isEqualTo(0);
        }

        @Test
        void multipleCommits() throws IOException, GitAPIException {
            createFileWithContent("testFile", "test");
            createFileWithContent("anotherTestFile", "another test");
            commitAll();
            createFileWithContent("testFile", "more tests");

            gitDirectoryManager.commitFile("configuration/testFile");

            List<FileVersionResponse> a = gitDirectoryManager.getCommitsOfFile("configuration/testFile");
            List<FileVersionResponse> b = gitDirectoryManager.getCommitsOfFile("configuration/anotherTestFile");

            assertThat(gitDirectoryManager.getCommitsOfFile("configuration/testFile").size()).isEqualTo(2);
            assertThat(gitDirectoryManager.getCommitsOfFile("configuration/anotherTestFile").size()).isEqualTo(1);
        }
    }
}