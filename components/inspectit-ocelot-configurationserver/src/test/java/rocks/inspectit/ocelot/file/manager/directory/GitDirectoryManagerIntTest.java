package rocks.inspectit.ocelot.file.manager.directory;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileVersionResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;


public class GitDirectoryManagerIntTest extends AbstractRepositoryTest {

    private GitDirectoryManager gitDirectoryManager;

    private Git git;

    @BeforeEach
    private void setupFileManager() {
        VersioningManager versioningManager = new VersioningManager();
        versioningManager.config = serverSettings;
        versioningManager.init();

        git = versioningManager.git;

        gitDirectoryManager = new GitDirectoryManager();
        gitDirectoryManager.config = serverSettings;
        gitDirectoryManager.versioningManager = versioningManager;
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

    private static String readFile(String path) throws IOException {
        Path file = tempDirectory.resolve("files/configuration").resolve(path);
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
            createTestFiles("configuration/a", "configuration/b", "configuration/c");

            List<FileInfo> beforeCommit = gitDirectoryManager.listFiles("", true);
            assertThat(beforeCommit).isEmpty();

            gitDirectoryManager.commit();

            List<FileInfo> afterCommit = gitDirectoryManager.listFiles("", true);
            assertThat(afterCommit).hasSize(1);
            FileInfo fileInfo = afterCommit.get(0);
            assertThat(fileInfo).extracting(FileInfo::getName).isEqualTo("configuration");
            assertThat(fileInfo).extracting(FileInfo::getType).isEqualTo(FileInfo.Type.DIRECTORY);
            List<FileInfo> children = fileInfo.getChildren();
            assertThat(children).extracting(FileInfo::getName).containsExactly("a", "b", "c");
            assertThat(children).extracting(FileInfo::getType).containsOnly(FileInfo.Type.FILE);
        }
    }

    @Nested
    public class ListFiles {

        @Test
        void listEmptyRepo() throws IOException {
            List<String> emptyList = Collections.emptyList();

            List<FileInfo> output = gitDirectoryManager.listFiles("", true);

            assertThat(output).isEqualTo(emptyList);
        }

        @Test
        void listRepoTest() throws GitAPIException, IOException {
            createTestFiles("configuration/a", "configuration/b", "configuration/c");
            commitAll();

            List<FileInfo> output = gitDirectoryManager.listFiles("configuration", true);

            assertThat(output).hasSize(3)
                    .extracting(FileInfo::getName, FileInfo::getType)
                    .containsExactly(
                            tuple("a", FileInfo.Type.FILE),
                            tuple("b", FileInfo.Type.FILE),
                            tuple("c", FileInfo.Type.FILE)
                    );
        }
    }

    @Nested
    public class ReadFile {

        @Test
        void readFile() throws IOException, GitAPIException {
            createTestFile("hello", "world");
            commitAll();

            String output = gitDirectoryManager.readFile("hello");

            assertThat(output).isEqualTo("world");
        }
    }

    @Nested
    public class CommitFiles {

        @Test
        void commitFiles() throws IOException, GitAPIException {
            createTestFile("testFile", "This is not an easter egg!");
            commitAll();

            String beforeCommit = gitDirectoryManager.readFile("testFile");
            assertThat(beforeCommit).isEqualTo("This is not an easter egg!");

            createTestFile("testFile", "But this is one =)");

            gitDirectoryManager.commit();

            String afterCommit = gitDirectoryManager.readFile("testFile");
            assertThat(afterCommit).isEqualTo("But this is one =)");
        }
    }

    @Nested
    public class GetAllCommits {

        @Test
        void onlyInitialCommitPresent() throws IOException, GitAPIException {
            List<FileVersionResponse> output = gitDirectoryManager.getCommits();

            assertThat(output.size()).isEqualTo(0);
        }

        @Test
        void multipleCommits() throws GitAPIException, IOException {
            createTestFile("testFile", "Hello User!");
            commitAll();

            List<FileVersionResponse> output = gitDirectoryManager.getCommits();

            assertThat(output).hasSize(1);

            assertThat(output).extracting(FileVersionResponse::getCommitContent)
                    .flatExtracting(list -> ((List<String>) list))
                    .containsExactly("testFile");
        }
    }

    @Nested
    public class GetFileFromVersion {

        @Test
        void commitExistsWithFile() throws IOException, GitAPIException {
            createTestFile("testFile", "Hello User!");
            String commitId = commitAll().getName();
            createTestFile("testFile", "Hello World!");
            commitAll();

            String result = gitDirectoryManager.getFileContent("testFile", commitId);
            String resultLatest = gitDirectoryManager.readFile("testFile");

            assertThat(result).isEqualTo("Hello User!");
            assertThat(resultLatest).isEqualTo("Hello World!");
        }

        @Test
        void commitDoesNotContainFile() throws IOException, GitAPIException {
            createTestFile("testFile", "Hello User!");
            String commitId = commitAll().getName();

            String result = gitDirectoryManager.getFileContent("not-existing-file", commitId);

            assertThat(result).isEqualTo(null);
        }
    }

    @Nested
    public class GetCommitsByFile {

        @Test
        void noFileCommits() throws IOException, GitAPIException {
            String filePath = "test";

            List<FileVersionResponse> output = gitDirectoryManager.getCommitsByFile(filePath);

            assertThat(output).isEqualTo(Collections.emptyList());
        }

        @Test
        void fileCommitsForOtherFiles() throws IOException, GitAPIException {
            createTestFile("dummyFile", "test");
            commitAll();
            String filePath = "test";

            List<FileVersionResponse> output = gitDirectoryManager.getCommitsByFile(filePath);

            assertThat(output).isEqualTo(Collections.emptyList());
        }

        @Test
        void commitsForFile() throws GitAPIException, IOException {
            createTestFile("testFile");
            commitAll();

            List<FileVersionResponse> output = gitDirectoryManager.getCommitsByFile("testFile");

            assertThat(output.size()).isEqualTo(1);
        }
    }

    @Nested
    public class CommitFile {

        @Test
        void commitSingleFile() throws IOException, GitAPIException {
            createTestFile("testFile_a", "content_a");
            createTestFile("testFile_b", "content_b");
            commitAll();

            createTestFile("testFile_a", "content_c");

            gitDirectoryManager.commitFile("testFile_a");

            List<FileVersionResponse> testFileACommits = gitDirectoryManager.getCommitsByFile("testFile_a");
            List<FileVersionResponse> testFileBCommits = gitDirectoryManager.getCommitsByFile("testFile_b");

            assertThat(testFileACommits).hasSize(2);
            assertThat(testFileBCommits).hasSize(1);
        }
    }
}