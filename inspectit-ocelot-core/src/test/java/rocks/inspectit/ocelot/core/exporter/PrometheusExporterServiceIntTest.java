package rocks.inspectit.ocelot.core.exporter;

import ch.qos.logback.classic.Level;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.MeterProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

@TestPropertySource(properties = {"inspecit.exporters.metrics.prometheus.enabled=true"})
@DirtiesContext
public class PrometheusExporterServiceIntTest extends SpringTestBase {

    private static final int HTTP_TIMEOUT = 1000;

    private static CloseableHttpClient testClient;

    @Autowired
    PrometheusExporterService service;

    @BeforeAll
    static void initClient() {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(HTTP_TIMEOUT);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(HTTP_TIMEOUT);

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());
        testClient = builder.build();
    }

    @AfterAll
    static void closeClient() throws Exception {
        testClient.close();
    }

    void assertGet200(String url) throws Exception {
        CloseableHttpResponse response = testClient.execute(new HttpGet(url));
        int statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode).isEqualTo(200);
        response.close();
    }

    void assertUnavailable(String url) {
        Throwable throwable = catchThrowable(() -> testClient.execute(new HttpGet(url))
                .getStatusLine()
                .getStatusCode());

        assertThat(throwable).isInstanceOf(IOException.class);
    }

    /**
     * Sets the switch of 'inspectit.exporters.metrics.prometheus.enabled' to the given value
     *
     * @param enabled
     */
    void switchPrometheusExporterService(boolean enabled) {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.prometheus.enabled", enabled + "");
        });
    }

    /**
     * Starts the {@link PrometheusExporterService} if it is not already running
     */
    @BeforeEach
    void enableService() {
        switchPrometheusExporterService(true);
    }

    @DirtiesContext
    @Test
    void testDefaultSettings() throws Exception {
        assertGet200("http://localhost:8888/metrics");
        assertNoLogsOfLevelOrGreater(Level.WARN);
    }

    @DirtiesContext
    @Test
    void testMasterSwitch() throws Exception {
        updateProperties(props -> {
            props.setProperty("inspectit.metrics.enabled", "false");
        });
        assertUnavailable("http://localhost:8888/metrics");
        assertNoLogsOfLevelOrGreater(Level.WARN);
    }

    @DirtiesContext
    @Test
    void testLocalSwitch() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.prometheus.enabled", "false");
        });
        System.out.println("test local switch unavail");
        assertUnavailable("http://localhost:8888/metrics");
        assertNoLogsOfLevelOrGreater(Level.WARN);
    }

    @DirtiesContext
    @Test
    void testChangePort() throws Exception {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.prometheus.port", "8899");
        });
        assertUnavailable("http://localhost:8888/metrics");
        assertGet200("http://localhost:8899/metrics");
        assertNoLogsOfLevelOrGreater(Level.WARN);
    }

    @DirtiesContext
    @Test
    void testRestartPrometheus() throws Exception {

        assertGet200("http://localhost:8888/metrics");
        assertUnavailable("http://localhost:8889/metrics");
        // disable prometheus
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.prometheus.enabled", "false");
        });
        assertUnavailable("http://localhost:8888/metrics");
        // enable prometheus
        enableService();
        assertThat(service.isEnabled()).isTrue();
        assertGet200("http://localhost:8888/metrics");
    }
}
