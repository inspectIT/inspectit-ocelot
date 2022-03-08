package rocks.inspectit.oce.eum.server.exporters;

import io.prometheus.client.CollectorRegistry;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.SocketUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test of PrometheusExporterService
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PrometheusExporterServiceIntTest.EnvInitializer.class)
public class PrometheusExporterServiceIntTest {

    private static String URL_KEY = "u";

    private static String SUT_URL = "http://test.com/login";

    private static String BEACON_KEY_NAME = "t_page";

    private static String FAKE_BEACON_KEY_NAME = "does_not_exist";

    private static int PROMETHEUS_PORT;

    static class EnvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            PROMETHEUS_PORT = SocketUtils.findAvailableTcpPort(20000);
            TestPropertyValues.of(String.format("inspectit-eum-server.exporters.metrics.prometheus.port=%d", PROMETHEUS_PORT))
                    .applyTo(applicationContext);
            TestPropertyValues.of(String.format("inspectit-eum-server.exporters.metrics.prometheus.enabled=ENABLED"))
                    .applyTo(applicationContext);
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    private static CloseableHttpClient httpClient;

    @BeforeAll
    public static void beforeClass() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @BeforeEach
    public void initClient() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        httpClient = builder.build();
    }

    /**
     * Sends a beacon to the mocked endpoint.
     */
    private void sendBeacon(Map<String, String> beacon) throws Exception {
        List<NameValuePair> params = beacon.entrySet()
                .stream()
                .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        String beaconEntity = EntityUtils.toString(new UrlEncodedFormEntity(params));

        mockMvc.perform(post("/beacon").contentType(MediaType.APPLICATION_FORM_URLENCODED).content(beaconEntity))
                .andExpect(status().isOk());
    }

    private Map<String, String> getBasicBeacon() {
        Map<String, String> beacon = new HashMap<>();
        beacon.put(URL_KEY, SUT_URL);
        return beacon;
    }

    @AfterEach
    public void closeClient() throws Exception {
        httpClient.close();
    }

    @Test
    public void testDefaultSettings() throws Exception {
        HttpGet httpGet = new HttpGet("http://localhost:" + PROMETHEUS_PORT + "/metrics");
        int statusCode = httpClient.execute(httpGet).getStatusLine().getStatusCode();

        assertThat(statusCode).isEqualTo(200);
    }

    /**
     * The application should expose no view, since no beacon entry maps to the default implementation.
     *
     * @throws Exception
     */
    @Test
    public void expectNoViews() throws Exception {
        Map<String, String> beacon = getBasicBeacon();
        beacon.put(FAKE_BEACON_KEY_NAME, "Fake Value");

        sendBeacon(beacon);

        await().atMost(15, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).untilAsserted(() -> {
            HttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + PROMETHEUS_PORT + "/metrics)"));
            ResponseHandler responseHandler = new BasicResponseHandler();
            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
            assertThat(responseHandler.handleResponse(response).toString()).doesNotContain("Fake Value");
        });
    }

    /**
     * The application should expose one view, since one beacon entry maps to the default implementation.
     *
     * @throws Exception
     */
    @Test
    public void expectOneView() throws Exception {
        Map<String, String> beacon = getBasicBeacon();
        beacon.put(BEACON_KEY_NAME, "12");

        sendBeacon(beacon);

        await().atMost(15, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).untilAsserted(() -> {
            HttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + PROMETHEUS_PORT + "/metrics)"));
            ResponseHandler responseHandler = new BasicResponseHandler();
            assertThat(responseHandler.handleResponse(response)
                    .toString()).contains("page_ready_time_SUM{COUNTRY_CODE=\"\",OS=\"\",URL=\"http://test.com/login\",} 12.0");
        });
    }
}
