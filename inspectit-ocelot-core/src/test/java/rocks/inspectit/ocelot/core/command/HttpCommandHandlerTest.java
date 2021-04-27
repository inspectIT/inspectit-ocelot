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
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.AgentCommand;
import rocks.inspectit.ocelot.commons.models.AgentCommandType;
import rocks.inspectit.ocelot.commons.models.AgentResponse;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;
import rocks.inspectit.ocelot.core.command.HttpCommandHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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

            assertTrue(updateResult);
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
        public void healthCommandSend() throws IOException {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            AgentCommand command = new AgentCommand(AgentCommandType.GET_HEALTH, runtime.getName(), UUID.randomUUID(), null);
            String commandJson = objectWriter.writeValueAsString(command);
            String emptyCommandJson = objectWriter.writeValueAsString(AgentCommand.getEmptyCommand());

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
            AgentResponse response0 = mapper.readValue(requests.get(0)
                    .getRequest()
                    .getBodyAsString(), AgentResponse.class);
            AgentResponse response1 = mapper.readValue(requests.get(1)
                    .getRequest()
                    .getBodyAsString(), AgentResponse.class);
            AgentResponse response2 = mapper.readValue(requests.get(2)
                    .getRequest()
                    .getBodyAsString(), AgentResponse.class);
            AgentResponse response3 = mapper.readValue(requests.get(3)
                    .getRequest()
                    .getBodyAsString(), AgentResponse.class);
            AgentResponse response4 = mapper.readValue(requests.get(4)
                    .getRequest()
                    .getBodyAsString(), AgentResponse.class);
            assertEquals(command.getCommandId(), response3.getCommandId());
            assertEquals("OK", response3.getPayload());
            assertEquals(AgentResponse.getEmptyResponse(), response0);
            assertEquals(AgentResponse.getEmptyResponse(), response1);
            assertEquals(AgentResponse.getEmptyResponse(), response2);
            assertEquals(AgentResponse.getEmptyResponse(), response4);

        }

    }

}
