package rocks.inspectit.ocelot.agentconfiguration;

import inspectit.ocelot.configdocsgenerator.model.AgentDocumentation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.util.*;
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

    final String file = "/test.yml";

    @Nested
    class Create {
        @Test
        void verifyCreateHappyPath() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of(file));
            when(revisionAccess.configurationFileExists("test")).thenReturn(true);
            when(revisionAccess.configurationFileIsDirectory("test")).thenReturn(true);
            when(revisionAccess.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            when(revisionAccess.readConfigurationFile(file)).thenReturn(Optional.of("key: value"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();

            AgentDocumentation documentation = new AgentDocumentation(file, Collections.emptySet());

            AgentConfiguration config = AgentConfiguration.create(mapping, revisionAccess);

            assertThat(config.getMapping()).isEqualTo(mapping);
            assertThat(config.getConfigYaml()).isEqualTo("{key: value}\n");
            assertThat(config.getHash()).isNotBlank();
            assertThat(config.getDocumentationSuppliers()).isNotEmpty();

            AgentDocumentationSupplier createdSupplier = config.getDocumentationSuppliers().stream().findFirst().get();

            assertThat(createdSupplier.get()).isEqualTo(documentation);
        }
    }

    @Nested
    class LoadAndMergeYaml {

        @Test
        void yamlWithTab() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of(file));
            when(revisionAccess.configurationFileExists("test")).thenReturn(true);
            when(revisionAccess.configurationFileIsDirectory("test")).thenReturn(true);
            when(revisionAccess.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            when(revisionAccess.readConfigurationFile(file)).thenReturn(Optional.of("key:\tvalue"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();

            assertThatExceptionOfType(ObjectStructureMerger.InvalidConfigurationFileException.class).isThrownBy(
                    () -> AgentConfiguration.create(mapping, revisionAccess)
                    ).withMessage("The configuration file '%s' is invalid and cannot be parsed.", file);
        }
    }

    @Nested
    class LoadConfigForMapping {

        @Test
        void noSourcesSpecified() {
            AgentMapping mapping = AgentMapping.builder().build();
            AgentConfiguration config = AgentConfiguration.create(mapping, revisionAccess);
            String result = config.getConfigYaml();

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
            AgentConfiguration config = AgentConfiguration.create(mapping, revisionAccess);
            String result = config.getConfigYaml();

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
            AgentConfiguration config = AgentConfiguration.create(mapping, revisionAccess);
            String result = config.getConfigYaml();

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
            AgentConfiguration.create(mapping, revisionAccess);

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
            AgentConfiguration config = AgentConfiguration.create(mapping, revisionAccess);
            String result = config.getConfigYaml();

            assertThat(result).isEqualTo("{val1: z, val2: a, val3: b}\n");
            verify(revisionAccess, never()).readConfigurationFile("folder/somethingelse");
        }
    }

    @Nested
    class getDocumentations {

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
        void verifyGetEmptyDocumentations() {
            AgentConfiguration config = AgentConfiguration.NO_MATCHING_MAPPING;

            assertThat(config.getDocumentations()).isEmpty();
        }

        @Test
        void verifyGetDocumentations() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getAbsoluteFilePaths(any())).thenReturn(Stream.of(file));
            when(revisionAccess.configurationFileExists("test")).thenReturn(true);
            when(revisionAccess.configurationFileIsDirectory("test")).thenReturn(true);
            when(revisionAccess.listConfigurationFiles(anyString())).thenReturn(Collections.singletonList(fileInfo));
            when(revisionAccess.readConfigurationFile(file)).thenReturn(Optional.of(srcYaml));

            AgentDocumentation documentation = new AgentDocumentation(file, Collections.singleton("s_jdbc_statement_execute"));

            AgentMapping mapping = AgentMapping.builder()
                    .name("test-mapping")
                    .source("/test")
                    .sourceBranch(WORKSPACE)
                    .build();

            AgentConfiguration config = AgentConfiguration.create(mapping, revisionAccess);

            assertThat(config.getDocumentations()).containsExactly(documentation);
        }

        @Test
        void verifyGetDocumentationsWithoutFileAccessor() {
            AgentMapping mapping = AgentMapping.builder().build();

            AgentConfiguration config = AgentConfiguration.create(mapping, null);

            assertThat(config.getDocumentations()).isEmpty();
        }
    }
}
