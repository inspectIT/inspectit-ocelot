package rocks.inspectit.ocelot.rest.hook;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.file.FileManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class WebhookControllerTest {

    @InjectMocks
    protected WebhookController controller;

    @Mock
    private FileManager fileManager;

    @Nested
    public class SynchronizeWorkspace {

        @Test
        public void triggerSynchronization() throws Exception {
            ResponseEntity<?> result = controller.synchronizeWorkspace();

            verify(fileManager).synchronizeWorkspace();
            verifyNoMoreInteractions(fileManager);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}