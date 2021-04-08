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
import org.springframework.test.context.TestExecutionListeners;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import rocks.inspectit.oce.eum.server.utils.ResetMetricsTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = JaegerExporterIntTest.EnvInitializer.class)
@Testcontainers(disabledWithoutDocker = true)
@TestExecutionListeners(listeners = ResetMetricsTestExecutionListener.class)
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
    public static GenericContainer<?> jaegerContainer =
            new GenericContainer<>("ghcr.io/open-telemetry/java-test-containers:jaeger")
                    .withExposedPorts(COLLECTOR_PORT, QUERY_PORT, HEALTH_PORT)
                    .waitingFor(Wait.forHttp("/").forPort(HEALTH_PORT));

    static class EnvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    String.format("inspectit-eum-server.exporters.tracing.jaeger.grpc=%s:%d", jaegerContainer.getHost(), jaegerContainer.getMappedPort(COLLECTOR_PORT)),
                    "inspectit-eum-server.exporters.tracing.jaeger.service-name=" + JaegerExporterIntTest.SERVICE_NAME
            ).applyTo(applicationContext);
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
        return "{\"resourceSpans\":[{\"resource\":{\"attributes\":[{\"key\":\"service.name\",\"type\":0,\"stringValue\":\"" + SERVICE_NAME + "\"},{\"key\":\"telemetry.sdk.language\",\"type\":0,\"stringValue\":\"webjs\"},{\"key\":\"telemetry.sdk.name\",\"type\":0,\"stringValue\":\"opentelemetry\"},{\"key\":\"telemetry.sdk.version\",\"type\":0,\"stringValue\":\"0.9.0\"}],\"droppedAttributesCount\":0},\"instrumentationLibrarySpans\":[{\"spans\":[{\"traceId\":\"nqPKtzup5z3q4e9SdFXEaw==\",\"spanId\":\"S3XoCZ1bIZo=\",\"name\":\"https://example.org/8PwjppjPBnHBWWYjDdGz\",\"kind\":3,\"startTimeUnixNano\":" + now + ",\"endTimeUnixNano\":" + now + ",\"attributes\":[{\"key\":\"component\",\"type\":0,\"stringValue\":\"xml-http-request\"},{\"key\":\"http.method\",\"type\":0,\"stringValue\":\"POST\"},{\"key\":\"http.url\",\"type\":0,\"stringValue\":\"https://hookb.in/8PwjppjPBnHBWWYjDdGz\"},{\"key\":\"http.status_code\",\"type\":2,\"doubleValue\":0},{\"key\":\"http.status_text\",\"type\":0,\"stringValue\":\"\"},{\"key\":\"http.host\",\"type\":0,\"stringValue\":\"hookb.in\"},{\"key\":\"http.scheme\",\"type\":0,\"stringValue\":\"https\"},{\"key\":\"http.user_agent\",\"type\":0,\"stringValue\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36\"}],\"droppedAttributesCount\":0,\"events\":[{\"timeUnixNano\":1613729658889170000,\"name\":\"open\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1613729658890130000,\"name\":\"send\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1613729658891400000,\"name\":\"fetchStart\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1613729658926715000,\"name\":\"responseEnd\",\"attributes\":[],\"droppedAttributesCount\":0},{\"timeUnixNano\":1613729658928125000,\"name\":\"error\",\"attributes\":[],\"droppedAttributesCount\":0}],\"droppedEventsCount\":0,\"status\":{\"code\":0},\"links\":[],\"droppedLinksCount\":0}],\"instrumentationLibrary\":{\"name\":\"opentelemetry - webjs\",\"version\":\"0.9.0\"}}]}]}";
    }

    private boolean assertJaegerHaveTrace() {
        try {
            String url = String.format("http://%s:%d/api/traces?service=%s",
                    jaegerContainer.getHost(), jaegerContainer.getMappedPort(QUERY_PORT), SERVICE_NAME);

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
