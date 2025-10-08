package rocks.inspectit.ocelot.core.command;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentCommandServiceTest {

    @InjectMocks
    AgentCommandService service;

    @Mock
    ScheduledExecutorService executor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitConfig configuration;

    @Mock
    HttpCommandFetcher commandFetcher;

    @Nested
    class DoEnable {

        @Captor
        ArgumentCaptor<URI> uriCaptor;

        @Test
        void successfullyEnabled() throws MalformedURLException {
            when(configuration.getAgentCommands().getPollingInterval()).thenReturn(Duration.ofSeconds(1));
            lenient().when(configuration.getAgentCommands().getUrl()).thenReturn(new URL("http://inspectit.rocks"));
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(new URL("http://example.org/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getAgentCommands().getAgentCommandPath()).thenReturn("/api/v1/agent/command");

            boolean result = service.doEnable(configuration);

            verify(executor).scheduleWithFixedDelay(service, 1000, 1000, TimeUnit.MILLISECONDS);
            verify(commandFetcher).setCommandUri(uriCaptor.capture());
            verifyNoMoreInteractions(executor, commandFetcher);
            assertThat(result).isTrue();
            assertThat(uriCaptor.getValue().toString()).isEqualTo("http://example.org/api/v1/agent/command");
        }
    }

    @Nested
    class DoDisable {

        @Test
        void notEnabled() {
            boolean result = service.doDisable();

            assertThat(result).isTrue();
            verifyNoMoreInteractions(commandFetcher);
        }

        @Test
        void isEnabled() throws MalformedURLException {
            when(configuration.getAgentCommands().getPollingInterval()).thenReturn(Duration.ofSeconds(1));
            when(configuration.getAgentCommands().getUrl()).thenReturn(new URL("http://example.org"));
            ScheduledFuture futureMock = mock(ScheduledFuture.class);
            when(executor.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(futureMock);

            service.doEnable(configuration);

            boolean result = service.doDisable();

            assertThat(result).isTrue();
            verify(futureMock).cancel(true);
            verify(commandFetcher).setCommandUri(any());
            verifyNoMoreInteractions(commandFetcher);
        }
    }

    @Nested
    class GetCommandUri {

        @Test
        void validCommandUrl() throws Exception {
            when(configuration.getAgentCommands().getUrl()).thenReturn(new URL("http://example.org:8090/api"));

            URI result = service.getCommandUri(configuration);

            assertThat(result.toString()).isEqualTo("http://example.org:8090/api");
        }

        @Test
        void deriveUrlWithoutConfigUrl() {
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(null);

            assertThat(configuration.getConfig().getHttp().getUrl()).isNull();
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> service.getCommandUri(configuration))
                    .withMessage("The URL cannot derived from the HTTP configuration URL because it is null");
        }

        @Test
        void deriveUrl() throws Exception {
            when(configuration.getConfig()
                    .getHttp()
                    .getUrl()).thenReturn(new URL("http://example.org:8090/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getAgentCommands().getAgentCommandPath()).thenReturn("/api/v1/agent/command");
            URI result = service.getCommandUri(configuration);

            assertThat(result.toString()).isEqualTo("http://example.org:8090/api/v1/agent/command");
        }

        @Test
        void deriveUrlWithoutPort() throws Exception {
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(new URL("http://example.org/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getAgentCommands().getAgentCommandPath()).thenReturn("/api/command");
            URI result = service.getCommandUri(configuration);

            assertThat(result.toString()).isEqualTo("http://example.org/api/command");
        }

        @Test
        void verifyPrioritization() throws Exception {
            lenient().when(configuration.getAgentCommands().getUrl()).thenReturn(new URL("http://example.org"));
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(new URL("http://example.org/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getAgentCommands().getAgentCommandPath()).thenReturn("/api/command");
            URI result = service.getCommandUri(configuration);

            assertThat(result.toString()).isEqualTo("http://example.org/api/command");
        }
    }
}
