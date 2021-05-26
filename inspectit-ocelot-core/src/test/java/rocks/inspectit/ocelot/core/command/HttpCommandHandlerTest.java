package rocks.inspectit.ocelot.core.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.MultiValue;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.response.impl.PingResponse;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HttpCommandHandlerTest {

    @InjectMocks
    private HttpCommandHandler handler;

    private WireMockServer mockServer;

    private HttpConfigSettings httpSettings;

    private ObjectWriter objectWriter;

    @BeforeEach
    public void setup() throws MalformedURLException {
        mockServer = new WireMockServer(options().dynamicPort());
        mockServer.start();

        httpSettings = new HttpConfigSettings();
        httpSettings.setCommandUrl(new URL("http://localhost:" + mockServer.port() + "/"));
        httpSettings.setAttributes(new HashMap<>());

        handler.currentSettings = httpSettings;
        objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    }

    @AfterEach
    public void teardown() {
        mockServer.stop();
    }

    @Nested
    public class FetchCommand {

        @Test
        public void noCommand() throws UnsupportedEncodingException, JsonProcessingException {
            String commandJson = objectWriter.writeValueAsString(null);
            mockServer.stubFor(post(urlPathEqualTo("/")).willReturn(aResponse().withStatus(200).withBody(commandJson)));

            boolean updateResult = handler.fetchCommand();

            assertFalse(updateResult);
            List<ServeEvent> requests = mockServer.getServeEvents().getRequests();
            assertThat(requests).hasSize(1);
            List<String> headerKeys = requests.get(0)
                    .getRequest()
                    .getHeaders()
                    .all()
                    .stream()
                    .map(MultiValue::key)
                    .filter(key -> key.startsWith("X-OCELOT-"))
                    .collect(Collectors.toList());
            assertThat(headerKeys).containsOnly("X-OCELOT-AGENT-ID", "X-OCELOT-AGENT-VERSION", "X-OCELOT-JAVA-VERSION", "X-OCELOT-VM-NAME", "X-OCELOT-VM-VENDOR", "X-OCELOT-START-TIME");
        }

        @Test
        public void pingCommandSend() throws IOException {
            PingCommand command = new PingCommand();
            String commandJson = objectWriter.writeValueAsString(command);
            String emptyCommandJson = objectWriter.writeValueAsString(null);

            PingResponse response = new PingResponse(command.getCommandId(),true);
            CommandDelegator mockCommandDelegator = mock(CommandDelegator.class);
            when(mockCommandDelegator.delegate(Mockito.any())).thenReturn(response);

            handler.commandDelegator = mockCommandDelegator;

            // 2 Requests which worked + 3 from discovery mode.
            int expectedRequestSize = 5;

            mockServer.stubFor(post(urlPathEqualTo("/")).inScenario("First fetch")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(aResponse().withStatus(200).withBody(commandJson))
                    .willSetStateTo("No more commands"));

            // Second StubMapping
            mockServer.stubFor(post(urlPathEqualTo("/")).inScenario("First fetch")
                    .whenScenarioStateIs("No more commands")
                    .willReturn(aResponse().withStatus(200).withBody(emptyCommandJson)));

            boolean updateResult = handler.fetchCommand();

            assertFalse(updateResult);

            List<ServeEvent> requests = mockServer.getServeEvents().getRequests();
            assertThat(requests).hasSize(expectedRequestSize);

            List<String> headerKeys = requests.get(0)
                    .getRequest()
                    .getHeaders()
                    .all()
                    .stream()
                    .map(MultiValue::key)
                    .filter(key -> key.startsWith("X-OCELOT-"))
                    .collect(Collectors.toList());

            assertThat(headerKeys).containsOnly("X-OCELOT-AGENT-ID", "X-OCELOT-AGENT-VERSION", "X-OCELOT-JAVA-VERSION", "X-OCELOT-VM-NAME", "X-OCELOT-VM-VENDOR", "X-OCELOT-START-TIME");

            ObjectMapper mapper = new ObjectMapper();
            CommandResponse response0 = mapper.readValue(requests.get(0)
                    .getRequest()
                    .getBodyAsString(), CommandResponse.class);
            CommandResponse response1 = mapper.readValue(requests.get(1)
                    .getRequest()
                    .getBodyAsString(), CommandResponse.class);
            CommandResponse response2 = mapper.readValue(requests.get(2)
                    .getRequest()
                    .getBodyAsString(), CommandResponse.class);
            CommandResponse response3 = mapper.readValue(requests.get(3)
                    .getRequest()
                    .getBodyAsString(), CommandResponse.class);
            CommandResponse response4 = mapper.readValue(requests.get(4)
                    .getRequest()
                    .getBodyAsString(), CommandResponse.class);
            assertEquals(command.getCommandId(), response3.getCommandId());
            assertNull(response0);
            assertNull(response1);
            assertNull(response2);
            assertNull(response4);

        }

    }

}
