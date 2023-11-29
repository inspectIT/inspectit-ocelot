package rocks.inspectit.ocelot.mappings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.events.AgentMappingsSourceBranchChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.versioning.Branch;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static rocks.inspectit.ocelot.file.versioning.Branch.LIVE;

@ExtendWith(MockitoExtension.class)
public class AgentMappingSerializerTest {

    AgentMappingSerializer serializer;
    @Mock
    AbstractWorkingDirectoryAccessor workingDirectoryAccessor;
    @Mock
    FileManager fileManager;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    RevisionAccess revisionAccess;

    @BeforeEach
    public void setup() {
        InspectitServerSettings settings = InspectitServerSettings.builder().initialAgentMappingsSourceBranch("workspace").build();
        serializer = new AgentMappingSerializer(settings, fileManager, eventPublisher);
        serializer.postConstruct();
    }

    @Nested
    public class WriteAgentMappings {

        @Test
        public void successfullyWriteYaml() throws IOException {
            AgentMapping mapping = AgentMapping.builder()
                    .name("mapping")
                    .source("/any-source")
                    .sourceBranch(LIVE)
                    .attribute("key", "val")
                    .build();

            serializer.writeAgentMappings(Collections.singletonList(mapping), workingDirectoryAccessor);

            ArgumentCaptor<String> writtenFile = ArgumentCaptor.forClass(String.class);
            verify(workingDirectoryAccessor).writeAgentMappings(writtenFile.capture());

            assertThat(writtenFile.getValue()).isEqualTo("- name: \"mapping\"\n" +
                    "  sourceBranch: \"LIVE\"\n"+
                    "  sources:\n" +
                    "  - \"/any-source\"\n" +
                    "  attributes:\n" +
                    "    key: \"val\"\n");
            verifyNoMoreInteractions(workingDirectoryAccessor);
        }
    }

    @Nested
    public class ReadAgentMappings {

        @Test
        public void successfullyReadYaml() {
            String dummyYaml = "- name: \"mapping\"\n" +
                    "  sourceBranch: \"LIVE\"\n" +
                    "  sources:\n" +
                    "  - \"/any-source\"\n" +
                    "  attributes:\n" +
                    "    key: \"val\"\n";

            doReturn(Optional.of(dummyYaml)).when(workingDirectoryAccessor).readAgentMappings();

            List<AgentMapping> result = serializer.readAgentMappings(workingDirectoryAccessor);

            assertThat(result).hasSize(1);
            AgentMapping mapping = result.get(0);
            assertThat(mapping.name()).isEqualTo("mapping");
            assertThat(mapping.sources()).containsExactly("/any-source");
            assertThat(mapping.attributes()).containsEntry("key", "val");
            assertThat(mapping.sourceBranch()).isEqualTo(LIVE);
        }
    }

    @Nested
    public class setAgentMappingsSourceBranch {

        @Test
        void verifySourceBranchHasChanged() {
            when(fileManager.getLiveRevision()).thenReturn(revisionAccess);
            when(revisionAccess.agentMappingsExist()).thenReturn(true);

            Branch oldBranch = serializer.getSourceBranch();
            Branch newBranch = serializer.setSourceBranch(LIVE);

            verify(eventPublisher, times(1)).publishEvent(any(AgentMappingsSourceBranchChangedEvent.class));
            assertThat(oldBranch.equals(newBranch)).isFalse();
        }

        @Test
        void verifySourceBranchHasNotChanged() {
            when(fileManager.getLiveRevision()).thenReturn(revisionAccess);
            when(revisionAccess.agentMappingsExist()).thenReturn(false);

            Branch oldBranch = serializer.getSourceBranch();
            Branch newBranch = serializer.setSourceBranch(LIVE);

            verify(eventPublisher, times(0)).publishEvent(any(AgentMappingsSourceBranchChangedEvent.class));
            assertThat(oldBranch.equals(newBranch)).isTrue();
        }
    }
}
