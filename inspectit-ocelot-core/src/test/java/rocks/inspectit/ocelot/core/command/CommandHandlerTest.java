package rocks.inspectit.ocelot.core.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.config.model.config.RetrySettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommandHandlerTest {

    @InjectMocks
    private CommandHandler handler;

    @Mock
    private InspectitEnvironment environment;

    @Mock
    private HttpCommandFetcher commandFetcher;

    @Mock
    private CommandDelegator commandDelegator;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpResponse httpResponse;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    ArgumentCaptor<CommandResponse> responseCaptor;

    @BeforeEach
    public void beforeEach() {
        handler.objectMapper = objectMapper;
        AgentCommandSettings agentCommands = new AgentCommandSettings();

        InspectitConfig inspectitConfig = new InspectitConfig();
        inspectitConfig.setAgentCommands(agentCommands);

        when(environment.getCurrentConfig()).thenReturn(inspectitConfig);
    }

    @Nested
    public class NextCommand {

        @Test
        public void noCommand() throws IOException {
            when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
            when(commandFetcher.fetchCommand(any(), anyBoolean())).thenReturn(httpResponse);

            handler.nextCommand();

            verify(commandFetcher).fetchCommand(null, false);
        }

        @Test
        public void pingCommandSend() throws IOException {
            environment.getCurrentConfig().getAgentCommands().setLiveModeDuration(Duration.ZERO);
            PingCommand command = new PingCommand();
            PingCommand.Response pingResponse = new PingCommand.Response();
            StatusLine statusLine = mock(StatusLine.class);
            when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK, HttpStatus.SC_NO_CONTENT);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(objectMapper.readValue(any(InputStream.class), any(Class.class))).thenReturn(command);
            when(commandDelegator.delegate(command)).thenReturn(pingResponse);
            when(commandFetcher.fetchCommand(nullable(CommandResponse.class), anyBoolean())).thenReturn(httpResponse);

            handler.nextCommand();

            verify(objectMapper).readValue(any(InputStream.class), any(Class.class));
            verify(commandFetcher, times(2)).fetchCommand(responseCaptor.capture(), anyBoolean());
            assertThat(responseCaptor.getAllValues()).containsExactly(null, pingResponse);
        }
    }

    @Nested
    class Retries {

        private final HttpResponse successfulResponse = httpResponseFor(200);

        private final HttpResponse unsuccessfulResponse = httpResponseFor(503);

        @BeforeEach
        void setup() {
            RetrySettings retrySettings = new RetrySettings();
            retrySettings.setMaxAttempts(2);
            retrySettings.setInitialIntervalMillis(5);
            retrySettings.setMultiplier(1);
            retrySettings.setRandomizationFactor(0.1);
            environment.getCurrentConfig().getAgentCommands().setRetry(retrySettings);
        }

        @Test
        void succeedsIfFirstCommandHandlerCallSucceeds() throws IOException {
            handler.nextCommand();

            verify(commandFetcher).fetchCommand(any(), anyBoolean());
        }

        @Test
        void retriesOnCommandFetcherException() throws IOException {
            when(commandFetcher.fetchCommand(any(), anyBoolean()))
                    .thenThrow(IOException.class)
                    .thenReturn(successfulResponse);

            handler.nextCommand();

            verify(commandFetcher, times(2)).fetchCommand(any(), anyBoolean());
        }

        @Test
        void retriesOnUnsuccessfulHttpResponse() throws IOException {
            when(commandFetcher.fetchCommand(any(), anyBoolean()))
                    .thenReturn(unsuccessfulResponse)
                    .thenReturn(successfulResponse);

            handler.nextCommand();

            verify(commandFetcher, times(2)).fetchCommand(any(), anyBoolean());
        }

        @Test
        void failsWithExceptionIfReachesMaxAttempts() throws IOException {
            when(commandFetcher.fetchCommand(any(), anyBoolean())).thenReturn(unsuccessfulResponse);

            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                handler.nextCommand();
            });
        }
    }

    private HttpResponse httpResponseFor(int statusCode) {
        return new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), statusCode, null));
    }
}
