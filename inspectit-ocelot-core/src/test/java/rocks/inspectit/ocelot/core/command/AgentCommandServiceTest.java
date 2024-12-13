package rocks.inspectit.ocelot.core.command;

import org.junit.jupiter.api.AfterEach;
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
public class AgentCommandServiceTest {

    @InjectMocks
    private AgentCommandService service;

    @Mock
    private ScheduledExecutorService executor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitConfig configuration;

    @Mock
    private HttpCommandFetcher commandFetcher;

    @Nested
    public class DoEnable {

        @Captor
        private ArgumentCaptor<URI> uriCaptor;

        @Test
        public void successfullyEnabled() throws MalformedURLException {
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
    public class DoDisable {

        @Test
        public void notEnabled() {
            boolean result = service.doDisable();

            assertThat(result).isTrue();
            verifyNoMoreInteractions(commandFetcher);
        }

        @Test
        public void isEnabled() throws MalformedURLException {
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
    public class GetCommandUri {

        @Test
        public void validCommandUrl() throws Exception {
            when(configuration.getAgentCommands().getUrl()).thenReturn(new URL("http://example.org:8090/api"));

            URI result = service.getCommandUri(configuration);

            assertThat(result.toString()).isEqualTo("http://example.org:8090/api");
        }

        @Test
        public void deriveUrlWithoutConfigUrl() {
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(null);

            assertThat(configuration.getConfig().getHttp().getUrl()).isNull();
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> service.getCommandUri(configuration))
                    .withMessage("The URL cannot derived from the HTTP configuration URL because it is null.");
        }

        @Test
        public void deriveUrl() throws Exception {
            when(configuration.getConfig()
                    .getHttp()
                    .getUrl()).thenReturn(new URL("http://example.org:8090/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getAgentCommands().getAgentCommandPath()).thenReturn("/api/v1/agent/command");
            URI result = service.getCommandUri(configuration);

            assertThat(result.toString()).isEqualTo("http://example.org:8090/api/v1/agent/command");
        }

        @Test
        public void deriveUrlWithoutPort() throws Exception {
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(new URL("http://example.org/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getAgentCommands().getAgentCommandPath()).thenReturn("/api/command");
            URI result = service.getCommandUri(configuration);

            assertThat(result.toString()).isEqualTo("http://example.org/api/command");
        }

        @Test
        public void verifyPrioritization() throws Exception {
            lenient().when(configuration.getAgentCommands().getUrl()).thenReturn(new URL("http://example.org"));
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(new URL("http://example.org/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getAgentCommands().getAgentCommandPath()).thenReturn("/api/command");
            URI result = service.getCommandUri(configuration);

            assertThat(result.toString()).isEqualTo("http://example.org/api/command");
        }
    }

    @Nested
    public class TaskTimeout {

        @AfterEach
        void disableTimeout() {
            // Stop restarting the task after timeout
            service.doDisable();
        }

        @Test
        void shouldCancelFutureAndRestartWhenTimeoutExceeded() throws MalformedURLException {
            Duration timeout = Duration.ofMillis(500);
            when(configuration.getAgentCommands().getPollingInterval()).thenReturn(Duration.ofSeconds(5));
            when(configuration.getAgentCommands().getTaskTimeout()).thenReturn(timeout);
            when(configuration.getAgentCommands().getUrl()).thenReturn(new URL("http://example.org"));
            ScheduledFuture future = mock(ScheduledFuture.class);
            when(future.isCancelled()).thenReturn(true);
            when(executor.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);

            service.doEnable(configuration);

            verify(future, timeout(timeout.toMillis() + 100)).cancel(true);
            verify(executor, times(2)).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        }

        @Test
        void shouldNotCancelFutureWhenNoTimeout() throws MalformedURLException {
            Duration timeout = Duration.ofMillis(5000);
            when(configuration.getAgentCommands().getPollingInterval()).thenReturn(Duration.ofMillis(500));
            when(configuration.getAgentCommands().getTaskTimeout()).thenReturn(timeout);
            when(configuration.getAgentCommands().getUrl()).thenReturn(new URL("http://example.org"));
            ScheduledFuture future = mock(ScheduledFuture.class);
            when(executor.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);

            service.doEnable(configuration);

            verify(future, never()).cancel(true);
            verify(executor, times(1)).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        }

        @Test
        void shouldNotCancelFutureWhenTimeoutIsZero() throws MalformedURLException {
            Duration timeout = Duration.ofMillis(0);
            when(configuration.getAgentCommands().getPollingInterval()).thenReturn(Duration.ofMillis(500));
            when(configuration.getAgentCommands().getTaskTimeout()).thenReturn(timeout);
            when(configuration.getAgentCommands().getUrl()).thenReturn(new URL("http://example.org"));
            ScheduledFuture future = mock(ScheduledFuture.class);
            when(executor.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);

            service.doEnable(configuration);

            verify(future, never()).cancel(true);
            verify(executor, times(1)).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        }
    }
}
