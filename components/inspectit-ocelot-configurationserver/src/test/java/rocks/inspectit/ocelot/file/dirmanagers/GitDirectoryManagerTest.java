package rocks.inspectit.ocelot.file.dirmanagers;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class GitDirectoryManagerTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");

    @InjectMocks
    private GitDirectoryManager gdm;

    @Mock
    private VersionController gp;

    @Mock
    private WorkingDirectoryManager wdm;

    @Mock
    ApplicationEventPublisher eventPublisher;


    @Nested
    public class CommitAllChanges {
        @Test
        void testCommit() throws IOException, GitAPIException {
            List<String> beforeCommit = gdm.listFiles();
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
            List<String> emptyList = Collections.emptyList();

            assertThat(gdm.listFiles()).isEqualTo(emptyList);
        }

        @Test
        void listRepoTest() throws GitAPIException, IOException {
            gdm.commitAllChanges();
            List<String> output = Arrays.asList("a", "b", "c");

            assertThat(gdm.listFiles()).isEqualTo(output);
        }
    }

    @Nested
    public class ReadFile {
        @Test
        void readFile() throws IOException, GitAPIException {
            gdm.commitAllChanges();

            assertThat(gdm.readFile("hello")).isEqualTo("world");
        }
    }

    @Nested
    public class ReadAgentMappingFiles {
        @Test
        void readAgentMappingFile() throws GitAPIException, IOException {
            gdm.commitAllChanges();

            assertThat(gdm.readAgentMappingFile()).isEqualTo("Hello World!");
        }
    }

    @Nested
    public class CommitAgentMappingFiles {
        @Test
        void newFileCreated() throws IOException, GitAPIException {
            gdm.commitAllChanges();

            gdm.commitAgentMappingFile();

            String agentMappingContent = gdm.readAgentMappingFile();
            String dummyFileContent = gdm.readFile("dummyFile");
            boolean agentMappingChanged = "This is not an easter egg!".equals(agentMappingContent);
            boolean dummyFileNotChanged = dummyFileContent == null;
            assertThat(agentMappingChanged && dummyFileNotChanged).isEqualTo(true);

        }

        @Test
        void commitAgentMappingOnly() throws IOException, GitAPIException {
            gdm.commitAllChanges();

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
            gdm.commitAllChanges();
            gdm.commitFiles();

            String agentMappingContent = gdm.readAgentMappingFile();
            String dummyFileContent = gdm.readFile("dummyFile");
            boolean agentMappingChanged = "Hello World!".equals(agentMappingContent);
            boolean dummyFileNotChanged = "But this is one =)".equals(dummyFileContent);
            assertThat(agentMappingChanged && dummyFileNotChanged).isEqualTo(true);
        }
    }

}
