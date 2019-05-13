package rocks.inspectit.ocelot.rest.agent;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @InjectMocks
    AgentController controller;

    @Nested
    public class FetchConfiguration {

        @Test
        public void valid() {

            String result = controller.fetchConfiguration("my-agent");

            assertThat(result).isNotEmpty();
            assertThat(result).contains("my-agent");
        }

    }

}