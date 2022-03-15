package rocks.inspectit.ocelot.core.command;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentCommandServiceTest {

    @InjectMocks
    private AgentCommandService service;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitConfig configuration;

    @Nested
    public class DoEnable {

        @Test
        public void successfullyEnabled() throws InterruptedException {
            lenient().when(configuration.getAgentCommands().getUrl()).thenReturn("inspectit.rocks:9090");

            boolean result = service.doEnable(configuration);

            assertThat(result).isTrue();
            assertThat(service.getClient()).isNotNull();
        }
    }

    @Nested
    public class DoDisable {

        @Test
        public void notEnabled() {
            boolean result = service.doDisable();

            assertThat(result).isTrue();
            assertThat(service.getClient()).isNull();
        }

        @Test
        public void isEnabled() throws MalformedURLException, InterruptedException {
            lenient().when(configuration.getAgentCommands().getUrl()).thenReturn("inspectitrocks:9090");

            service.doEnable(configuration);
            TimeUnit.SECONDS.sleep(5);
            assertThat(service.getClient()).isNotNull();

            boolean result = service.doDisable();
            TimeUnit.SECONDS.sleep(5);

            assertThat(result).isTrue();
            assertThat(service.getClient()).isNull();
        }
    }

    @Nested
    public class GetCommandUrl {

        @Test
        public void validCommandUrl() throws Exception {
            when(configuration.getAgentCommands().getUrl()).thenReturn("example.org:9090");

            String result = service.getCommandUrl(configuration);

            assertThat(result).isEqualTo("example.org:9090");
        }

        @Test
        public void deriveUrlWithoutConfigUrl() {
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(null);

            assertThat(configuration.getConfig().getHttp().getUrl()).isNull();
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> service.getCommandUrl(configuration))
                    .withMessage("The URL cannot be derived from the HTTP configuration URL because it is null.");
        }

        @Test
        public void deriveUrlWithoutPort() throws MalformedURLException {
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(new URL("https://inspectit.rocks"));
            when(configuration.getAgentCommands().getAgentCommandPort()).thenReturn(null);

            assertThat(configuration.getAgentCommands().getAgentCommandPort()).isNull();
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> service.getCommandUrl(configuration))
                    .withMessage("The URL cannot be derived from the HTTP configuration URL because the agentCommandPort is null.");
        }

        @Test
        public void deriveUrl() throws Exception {
            when(configuration.getConfig()
                    .getHttp()
                    .getUrl()).thenReturn(new URL("http://example.org:8090/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getAgentCommands().getAgentCommandPort()).thenReturn(9090);
            String result = service.getCommandUrl(configuration);

            assertThat(result).isEqualTo("example.org:9090");
        }

        @Test
        public void verifyPrioritization() throws Exception {
            lenient().when(configuration.getAgentCommands().getUrl()).thenReturn("inspectit.rocks:9090");
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(new URL("http://example.org/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getAgentCommands().getAgentCommandPort()).thenReturn(9090);
            String result = service.getCommandUrl(configuration);

            assertThat(result).isEqualTo("example.org:9090");
        }
    }
}
