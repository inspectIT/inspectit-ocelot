package rocks.inspectit.oce.eum.server.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.awaitility.Duration;


@Testcontainers(disabledWithoutDocker = true)
public class JaegerExporterIntTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OkHttpClient client = new OkHttpClient();

    private static final int QUERY_PORT = 16686;
    private static final int COLLECTOR_PORT = 14250;
    private static final int HEALTH_PORT = 14269;
    private static final String SERVICE_NAME = "E2E-test";
    private static final String JAEGER_URL = "http://localhost";

    @Container
    public static GenericContainer<?> jaegerContainer =
            new GenericContainer<>("ghcr.io/open-telemetry/java-test-containers:jaeger")
                    .withExposedPorts(COLLECTOR_PORT, QUERY_PORT, HEALTH_PORT)
                    .waitingFor(Wait.forHttp("/").forPort(HEALTH_PORT));

    @Test
    void testJaegerIntegration() {
        Awaitility.await()
                .atMost(Duration.TEN_SECONDS)
                .until(JaegerExporterIntTest::assertJaegerHaveTrace);
    }

    private static boolean assertJaegerHaveTrace() {
        try {
            String url =
                    String.format(
                            "%s/api/traces?service=%s",
                            String.format(JAEGER_URL + ":%d", jaegerContainer.getMappedPort(QUERY_PORT)),
                            SERVICE_NAME);

            Request request = new Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();

            final JsonNode json;
            try (Response response = client.newCall(request).execute()) {
                json = objectMapper.readTree(response.body().byteStream());
            }

            return json.get("data").get(0).get("traceID") != null;
        } catch (Exception e) {
            return false;
        }
    }
}
