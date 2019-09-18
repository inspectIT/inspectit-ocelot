package rocks.inspectit.ocelot.file.dirmanagers;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
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
public class GitProviderTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");
    private static final String FILES_SUBFOLDER = "git";

    @InjectMocks
    GitProvider gitProvider;

    private Git git;

    private Repository repo;

    @BeforeEach
    private void setupGitProvider() throws IOException, GitAPIException {
        Files.createDirectories(rootWorkDir);
        InspectitServerSettings conf = new InspectitServerSettings();
        conf.setWorkingDirectory(fmRoot.toString());
        initTestGit(conf);
        gitProvider = new GitProvider();
        gitProvider.config = conf;
        gitProvider.git = git;
        gitProvider.repo = repo;
    }

    private void initTestGit(InspectitServerSettings config) throws IOException, GitAPIException {
        Path filesRoot = Paths.get(config.getWorkingDirectory()).resolve("git").toAbsolutePath().normalize();
        Files.createDirectories(filesRoot);
        File localPath = new File(String.valueOf(filesRoot));
        filesRoot = Paths.get(config.getWorkingDirectory()).resolve(FILES_SUBFOLDER).toAbsolutePath().normalize();
        Files.createDirectories(filesRoot);
        git = Git.init().setDirectory(localPath).call();
        repo = git.getRepository();
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

    private static void deleteDirectory(Path path) throws IOException {
        List<Path> files = Files.walk(path).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(files);
        for (Path f : files) {
            File file = new File(f.toString());
            file.delete();
        }
    }

    @Nested
    public class CommitAllChanges {
        @Test
        void testCommit() throws GitAPIException, IOException {
            setupTestFiles(false, "/a");
            setupTestFiles(true, "git/b");
            List<String> output = Arrays.asList("b", "files/a");
            gitProvider.commitAllChanges();
            List<String> fileList = gitProvider.listFiles("", true);
            fileList.addAll(gitProvider.listFiles("", false));

            assertThat(fileList).isEqualTo(output);
        }
    }

    @Nested
    public class CommitFile {
        @Test
        void singleFileCommit() throws IOException, GitAPIException {
            createFileWithContent("/a", "testContent");
            gitProvider.commitFile("files");

            assertThat(gitProvider.readFile("files/a")).isEqualTo("testContent");
        }
    }

    @Nested
    public class listFiles {
        @Test
        void testlistSpecialFiles() throws GitAPIException, IOException {
            setupTestFiles(false, "/a");
            setupTestFiles(true, "git/b");
            git.add().addFilepattern("b").call();
            git.add().addFilepattern("files/a").call();
            git.commit().setAll(true)
                    .setMessage("testCommit")
                    .call();
            List<String> output = Arrays.asList("b");

            assertThat(gitProvider.listFiles("", true)).isEqualTo(output);
        }
    }

    @Nested
    public class ReadFile {
        @Test
        void readExistingFile() throws IOException, GitAPIException {
            createFileWithContent("/a", "testContent");
            git.add().addFilepattern("files/a").call();
            git.commit().setAll(true)
                    .setMessage("testCommit")
                    .call();

            assertThat(gitProvider.readFile("files/a")).isEqualTo("testContent");
        }

        @Test
        void readNonExistingFile() throws IOException, GitAPIException {
            createFileWithContent("/a", "testContent");
            git.add().addFilepattern("files/a").call();
            git.commit().setAll(true)
                    .setMessage("testCommit")
                    .call();

            assertThat(gitProvider.readFile("files/b")).isEqualTo(null);
        }
    }

}
