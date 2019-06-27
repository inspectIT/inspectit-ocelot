package rocks.inspectit.ocelot.rest.mappings;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentMappingControllerTest {

    @InjectMocks
    AgentMappingController controller;

    @Mock
    AgentMappingManager mappingManager;

    @Nested
    public class GetMappings {

        @Test
        public void successfullyGetAgentMappings() {
            List<AgentMapping> dummyList = new ArrayList<>();
            when(mappingManager.getAgentMappings()).thenReturn(dummyList);

            List<AgentMapping> result = controller.getMappings();

            verify(mappingManager).getAgentMappings();
            verifyNoMoreInteractions(mappingManager);
            assertThat(result).isSameAs(dummyList);
        }
    }

    @Nested
    public class PutMappings {

        @Test
        public void successfullyPutMappings() throws IOException {
            List<AgentMapping> dummyList = new ArrayList<>();

            controller.putMappings(dummyList);

            ArgumentCaptor<List<AgentMapping>> mappingCaptor = ArgumentCaptor.forClass(List.class);
            verify(mappingManager).setAgentMappings(mappingCaptor.capture());
            verifyNoMoreInteractions(mappingManager);
            assertThat(mappingCaptor.getValue()).isSameAs(dummyList);
        }
    }

    @Nested
    public class GetMappingByName {

        @Test
        public void successfullyGetMappingByName() {
            AgentMapping mappingMock = mock(AgentMapping.class);
            when(mappingManager.getAgentMapping("name")).thenReturn(Optional.of(mappingMock));

            ResponseEntity<AgentMapping> result = controller.getMappingByName("name");

            verify(mappingManager).getAgentMapping(eq("name"));
            verifyNoMoreInteractions(mappingManager);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(mappingMock);
        }


        @Test
        public void mappingNotFound() {
            when(mappingManager.getAgentMapping("name")).thenReturn(Optional.empty());

            ResponseEntity<AgentMapping> result = controller.getMappingByName("name");

            verify(mappingManager).getAgentMapping(eq("name"));
            verifyNoMoreInteractions(mappingManager);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(result.getBody()).isNull();
        }
    }

    @Nested
    public class DeleteMappingByName {

        @Test
        public void successfullyDeleteMappingByName() throws IOException {
            when(mappingManager.deleteAgentMapping("name")).thenReturn(true);

            ResponseEntity result = controller.deleteMappingByName("name");

            verify(mappingManager).deleteAgentMapping(eq("name"));
            verifyNoMoreInteractions(mappingManager);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }


        @Test
        public void mappingNotFound() throws IOException {
            when(mappingManager.deleteAgentMapping("name")).thenReturn(false);

            ResponseEntity result = controller.deleteMappingByName("name");

            verify(mappingManager).deleteAgentMapping(eq("name"));
            verifyNoMoreInteractions(mappingManager);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    public class PutMapping {

        @Mock
        AgentMapping agentMapping;

        @Test
        public void successfullyPutMapping() throws IOException {
            ResponseEntity result = controller.putMapping("mappingName", agentMapping, null, null);

            verify(agentMapping).setName("mappingName");
            verify(mappingManager).addAgentMapping(same(agentMapping));
            verifyNoMoreInteractions(mappingManager, agentMapping);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        public void beforeAndAfterIsSet() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> controller.putMapping("mappingName", agentMapping, "before", "after"))
                    .withMessage("The 'before' and 'after' parameters cannot be used together.");

            verifyZeroInteractions(mappingManager);
        }

        @Test
        public void addMappingBefore() throws IOException {
            ResponseEntity result = controller.putMapping("mappingName", agentMapping, "before", null);

            verify(mappingManager).addAgentMappingBefore(same(agentMapping), eq("before"));
            verifyNoMoreInteractions(mappingManager);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        public void addMappingAfter() throws IOException {
            ResponseEntity result = controller.putMapping("mappingName", agentMapping, null, "after");

            verify(mappingManager).addAgentMappingAfter(same(agentMapping), eq("after"));
            verifyNoMoreInteractions(mappingManager);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}