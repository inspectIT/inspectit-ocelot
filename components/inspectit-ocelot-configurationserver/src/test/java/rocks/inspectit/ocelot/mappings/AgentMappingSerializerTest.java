package rocks.inspectit.ocelot.mappings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.versioning.Branch;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentMappingSerializerTest {

    AgentMappingSerializer serializer;

    @Mock
    AbstractWorkingDirectoryAccessor fileAccessor;

    @BeforeEach
    public void setup() {
        serializer = new AgentMappingSerializer();
        serializer.postConstruct();
    }

    @Nested
    public class WriteAgentMappings {

        @Test
        public void successfullyWriteYaml() throws IOException {
            AgentMapping mapping = AgentMapping.builder()
                    .name("mapping")
                    .source("/any-source")
                    .sourceBranch(Branch.LIVE)
                    .attribute("key", "val")
                    .build();

            serializer.writeAgentMappings(Collections.singletonList(mapping), fileAccessor);

            ArgumentCaptor<String> writtenFile = ArgumentCaptor.forClass(String.class);
            verify(fileAccessor).writeAgentMappings(writtenFile.capture());

            assertThat(writtenFile.getValue()).isEqualTo("- name: \"mapping\"\n" +
                    "  sourceBranch: \"LIVE\"\n"+
                    "  sources:\n" +
                    "  - \"/any-source\"\n" +
                    "  attributes:\n" +
                    "    key: \"val\"\n");
            verifyNoMoreInteractions(fileAccessor);
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

            doReturn(Optional.of(dummyYaml)).when(fileAccessor).readAgentMappings();

            List<AgentMapping> result = serializer.readAgentMappings(fileAccessor);

            assertThat(result).hasSize(1);
            AgentMapping mapping = result.get(0);
            assertThat(mapping.getName()).isEqualTo("mapping");
            assertThat(mapping.getSources()).containsExactly("/any-source");
            assertThat(mapping.getAttributes()).containsEntry("key", "val");
            assertThat(mapping.getSourceBranch()).isEqualTo(Branch.LIVE);
        }
    }
}