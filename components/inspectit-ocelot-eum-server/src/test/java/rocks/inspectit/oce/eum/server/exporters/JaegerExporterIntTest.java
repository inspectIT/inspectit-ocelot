package rocks.inspectit.oce.eum.server.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = JaegerExporterIntTest.EnvInitializer.class)
@Testcontainers(disabledWithoutDocker = true)
public class JaegerExporterIntTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int QUERY_PORT = 16686;

    private static final int COLLECTOR_PORT = 14250;

    private static final int HEALTH_PORT = 14269;

    public static final String SERVICE_NAME = "E2E-test";

    @Container
    public static GenericContainer<?> jaegerContainer = new GenericContainer<>("ghcr.io/open-telemetry/java-test-containers:jaeger").withExposedPorts(COLLECTOR_PORT, QUERY_PORT, HEALTH_PORT)
            .waitingFor(Wait.forHttp("/").forPort(HEALTH_PORT));

    static class EnvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of("inspectit-eum-server.exporters.tracing.jaegerGrpc.enabled=" + ExporterEnabledState.ENABLED, String.format("inspectit-eum-server.exporters.tracing.jaegerGrpc.grpc=%s:%d", jaegerContainer.getHost(), jaegerContainer.getMappedPort(COLLECTOR_PORT)), "inspectit-eum-server.exporters.tracing.jaegerGrpc.service-name=" + JaegerExporterIntTest.SERVICE_NAME)
                    .applyTo(applicationContext);
        }
    }

    @Test
    void testJaegerIntegration() throws InterruptedException {
        // warmup
        postSpan();

        for (int i = 0; i < 15; i++) {
            postSpan();

            Thread.sleep(1000);
            boolean haveTrace = assertJaegerHaveTrace();

            if (haveTrace) {
                // end test
                return;
            }
        }

        throw new RuntimeException("Jaeger doesn't received any traces");
    }

    private void postSpan() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(getSpanString(), headers);

        ResponseEntity<String> entity = restTemplate.postForEntity(String.format("http://localhost:%d/spans", port), request, String.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    private String getSpanString() {
        String now = System.currentTimeMillis() + "000001";
        return "{\"resourceSpans\":[{\"resource\":{\"attributes\":[{\"key\":\"service.name\",\"value\":{\"stringValue\":\"" + SERVICE_NAME + "\"}},{\"key\":\"telemetry.sdk.language\",\"value\":{\"stringValue\":\"webjs\"}},{\"key\":\"telemetry.sdk.name\",\"value\":{\"stringValue\":\"opentelemetry\"}},{\"key\":\"telemetry.sdk.version\",\"value\":{\"stringValue\":\"0.18.2\"}}],\"droppedAttributesCount\":0},\"instrumentationLibrarySpans\":[{\"spans\":[{\"traceId\":\"497d4e959f574a77d0d3abf05523ec5c\",\"spanId\":\"fc3d735ad8dd7399\",\"name\":\"HTTP GET\",\"kind\":3,\"startTimeUnixNano\":" + now + ",\"endTimeUnixNano\":" + now + ",\"attributes\":[{\"key\":\"http.method\",\"value\":{\"stringValue\":\"GET\"}},{\"key\":\"http.url\",\"value\":{\"stringValue\":\"http://localhost:1337?command=undefined\"}},{\"key\":\"http.response_content_length\",\"value\":{\"intValue\":665}},{\"key\":\"http.status_code\",\"value\":{\"intValue\":200}},{\"key\":\"http.status_text\",\"value\":{\"stringValue\":\"OK\"}},{\"key\":\"http.host\",\"value\":{\"stringValue\":\"localhost:1337\"}},{\"key\":\"http.scheme\",\"value\":{\"stringValue\":\"http\"}},{\"key\":\"http.user_agent\",\"value\":{\"stringValue\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36\"}}],\"droppedAttributesCount\":0,\"events\":[{\"timeUnixNano\":1619187815416888600,\"name\":\"open\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619187815417378600,\"name\":\"send\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619187815418218800,\"name\":\"fetchStart\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619187815420648700,\"name\":\"domainLookupStart\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619187815420648700,\"name\":\"domainLookupEnd\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619187815420648700,\"name\":\"connectStart\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619170468572063700,\"name\":\"secureConnectionStart\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619187815723468800,\"name\":\"connectEnd\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619187815723523600,\"name\":\"requestStart\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619187815732868600,\"name\":\"responseStart\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619187815734768600,\"name\":\"responseEnd\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1619187815735928600,\"name\":\"loaded\",\"attributes\":[],\"droppedAttributesCount\":0}],\"droppedEventsCount\":0,\"status\":{\"code\":0},\"links\":[],\"droppedLinksCount\":0}],\"instrumentationLibrary\":{\"name\":\"@opentelemetry/instrumentation-xml-http-request\",\"version\":\"0.18.2\"}}]}]}";
    }

    private boolean assertJaegerHaveTrace() {
        try {
            String url = String.format("http://%s:%d/api/traces?service=%s", jaegerContainer.getHost(), jaegerContainer.getMappedPort(QUERY_PORT), SERVICE_NAME);

            ResponseEntity<String> result = restTemplate.getForEntity(url, String.class);

            System.out.println("Jaeger response: " + result.getStatusCodeValue());
            System.out.println("|- " + result.getBody());

            JsonNode json = objectMapper.readTree(result.getBody());
            return json.get("data").get(0).get("traceID") != null;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        }
    }
}
