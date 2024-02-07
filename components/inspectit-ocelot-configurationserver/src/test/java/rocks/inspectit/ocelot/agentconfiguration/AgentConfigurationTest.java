package rocks.inspectit.ocelot.agentconfiguration;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.versioning.Branch;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static rocks.inspectit.ocelot.file.versioning.Branch.WORKSPACE;

@ExtendWith(MockitoExtension.class)
public class AgentConfigurationTest {

    @Mock
    RevisionAccess revisionAccess;

    @Nested
    class LoadAndMergeYaml {

        @Test
        void loadYaml()  {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"));
            when(revisionAccess.configurationFileExists("test")).thenReturn(true);
            when(revisionAccess.configurationFileIsDirectory("test")).thenReturn(true);
            when(revisionAccess.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            when(revisionAccess.readConfigurationFile("/test.yml")).thenReturn(Optional.of("key: value"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();
            String string = AgentConfiguration.loadConfigForMapping(mapping, revisionAccess);

            assertThat(string).isEqualTo("{key: value}\n");
        }

        @Test
        void yamlWithTab() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of("/test.yml"));
            when(revisionAccess.configurationFileExists("test")).thenReturn(true);
            when(revisionAccess.configurationFileIsDirectory("test")).thenReturn(true);
            when(revisionAccess.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            when(revisionAccess.readConfigurationFile("/test.yml")).thenReturn(Optional.of("key:\tvalue"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();

            assertThatExceptionOfType(ObjectStructureMerger.InvalidConfigurationFileException.class).isThrownBy(
                    () -> AgentConfiguration.loadConfigForMapping(mapping, revisionAccess)
                    ).withMessage("The configuration file '/test.yml' is invalid and cannot be parsed.");
        }
    }

    @Nested
    class LoadConfigForMapping {

        @Test
        void noSourcesSpecified() {
            AgentMapping mapping = AgentMapping.builder().build();
            String result = AgentConfiguration.loadConfigForMapping(mapping, revisionAccess);

            assertThat(result).isEmpty();
        }

        @Test
        void nonExistingSourcesSpecified() {
            doReturn(false).when(revisionAccess).configurationFileExists("a.yml");
            doReturn(false).when(revisionAccess).configurationFileExists("some/folder");

            AgentMapping mapping = AgentMapping.builder()
                    .source("a.yml")
                    .source("/some/folder")
                    .sourceBranch(WORKSPACE)
                    .build();
            String result = AgentConfiguration.loadConfigForMapping(mapping, revisionAccess);

            assertThat(result).isEmpty();
        }

        @Test
        void nonYamlIgnored() {
            doReturn(true).when(revisionAccess).configurationFileExists(any());
            doReturn(false).when(revisionAccess).configurationFileIsDirectory(any());
            doReturn(Optional.of("")).when(revisionAccess).readConfigurationFile(any());

            AgentMapping mapping = AgentMapping.builder()
                    .source("a.yml")
                    .source("b.YmL")
                    .source("c.yaml")
                    .source("d.txt")
                    .sourceBranch(WORKSPACE)
                    .build();
            String result = AgentConfiguration.loadConfigForMapping(mapping, revisionAccess);

            assertThat(result).isEmpty();
            verify(revisionAccess).readConfigurationFile("a.yml");
            verify(revisionAccess).readConfigurationFile("b.YmL");
            verify(revisionAccess).readConfigurationFile("c.yaml");

            verify(revisionAccess, never()).readConfigurationFile("d.txt");
        }

        @Test
        void leadingSlashesInSourcesRemoved() {
            doReturn(false).when(revisionAccess).configurationFileExists("a.yml");

            lenient().doThrow(new RuntimeException()).when(revisionAccess).configurationFileExists(startsWith("/"));

            AgentMapping mapping = AgentMapping.builder()
                    .source("/a.yml")
                    .sourceBranch(WORKSPACE)
                    .build();
            AgentConfiguration.loadConfigForMapping(mapping, revisionAccess);

            verify(revisionAccess).configurationFileExists(eq("a.yml"));
        }

        @Test
        void priorityRespected() {

            when(revisionAccess.configurationFileExists(any())).thenReturn(true);

            doReturn(true).when(revisionAccess).configurationFileIsDirectory("folder");
            doReturn(false).when(revisionAccess).configurationFileIsDirectory("z.yml");

            List<FileInfo> fileInfos = Arrays.asList(FileInfo.builder()
                    .type(FileInfo.Type.FILE)
                    .name("b.yml")
                    .build(), FileInfo.builder().type(FileInfo.Type.FILE).name("a.yml").build(), FileInfo.builder()
                    .type(FileInfo.Type.FILE)
                    .name("somethingelse")
                    .build());

            when(revisionAccess.listConfigurationFiles("folder")).thenReturn(fileInfos);

            doReturn(Optional.of("{ val1: z}")).when(revisionAccess).readConfigurationFile("z.yml");
            doReturn(Optional.of("{ val1: a, val2: a}")).when(revisionAccess).readConfigurationFile("folder/a.yml");
            doReturn(Optional.of("{ val1: b, val2: b, val3: b}")).when(revisionAccess)
                    .readConfigurationFile("folder/b.yml");

            AgentMapping mapping = AgentMapping.builder()
                    .source("/z.yml")
                    .source("/folder")
                    .sourceBranch(WORKSPACE)
                    .build();
            String result = AgentConfiguration.loadConfigForMapping(mapping, revisionAccess);

            assertThat(result).isEqualTo("{val1: z, val2: a, val3: b}\n");
            verify(revisionAccess, never()).readConfigurationFile("folder/somethingelse");
        }
    }

    @Nested
    class SupplyDocsObjects {

        private final String srcYaml = """
            inspectit:
              instrumentation:
                scopes:
                  s_jdbc_statement_execute:
                    docs:
                      description: 'Scope for executed JDBC statements.'
                    methods:
                      - name: execute
            """;

        @Test
        void verifySuccessfulSupply() {
            FileInfo fileInfo = mock(FileInfo.class);
            String fileName = "/test.yml";
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of(fileName), Stream.of(fileName));
            when(revisionAccess.configurationFileExists(anyString())).thenReturn(true);
            when(revisionAccess.configurationFileIsDirectory(anyString())).thenReturn(true);
            when(revisionAccess.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            when(revisionAccess.readConfigurationFile(anyString())).thenReturn(Optional.of(srcYaml));

            Set<String> docsObjects = Set.of("s_jdbc_statement_execute");
            Map<String, Set<String>> docsObjectsByFile = new HashMap<>();
            docsObjectsByFile.put(fileName, docsObjects);

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();

            AgentConfiguration config = AgentConfiguration.create(mapping, revisionAccess);
            config.supplyDocsObjectsByFile();

            assertThat(config.getDocsObjectsByFile()).isEqualTo(docsObjectsByFile);
        }

        @Test
        void verifyNoSupply() {
            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();
            AgentConfiguration config = AgentConfiguration.create(mapping, new HashMap<>(), srcYaml);
            config.supplyDocsObjectsByFile();

            assertThat(config.getDocsObjectsByFile()).isEmpty();
        }
    }
}
