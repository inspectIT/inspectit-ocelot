package rocks.inspectit.ocelot.core.exporter;

import ch.qos.logback.classic.Level;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.util.Timeout;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@TestPropertySource(properties = {"inspectit.exporters.metrics.prometheus.enabled=ENABLED"})
@DirtiesContext
public class PrometheusExporterServiceIntTest extends SpringTestBase {

    private static final Timeout HTTP_TIMEOUT = Timeout.of(1000, TimeUnit.MILLISECONDS);

    private static CloseableHttpClient testClient;

    @Autowired
    PrometheusExporterService service;

    @BeforeAll
    static void initClient() {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(HTTP_TIMEOUT);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(HTTP_TIMEOUT);
        requestBuilder = requestBuilder.setResponseTimeout(HTTP_TIMEOUT);

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());
        testClient = builder.build();
    }

    @AfterAll
    static void closeClient() throws Exception {
        testClient.close();
    }

    void assertGet200(String url) throws Exception {
        ClassicHttpRequest request = ClassicRequestBuilder.get().setUri(url).build();
        CloseableHttpResponse response = testClient.execute(request);
        int statusCode = response.getCode();
        assertThat(statusCode).isEqualTo(200);
        response.close();
    }

    void assertUnavailable(String url) {
        ClassicHttpRequest request = ClassicRequestBuilder.get().setUri(url).build();
        Throwable throwable = catchThrowable(() -> testClient.execute(request).getCode());

        assertThat(throwable).isInstanceOf(IOException.class);
    }

    /**
     * Sets the switch of 'inspectit.exporters.metrics.prometheus.enabled' to the given value
     */
    void localSwitch(ExporterEnabledState enabled) {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.prometheus.enabled", enabled);
        });
    }

    /**
     * Starts the {@link PrometheusExporterService} if it is not already running
     */
    @BeforeEach
    void enableService() {
        localSwitch(ExporterEnabledState.ENABLED);
    }

    @DirtiesContext
    @Test
    void testDefaultSettings() throws Exception {
        assertGet200("http://localhost:8888/metrics");
        assertNoLogsOfLevelOrGreater(Level.WARN);
    }

    @DirtiesContext
    @Test
    void testMasterSwitch() {
        updateProperties(props -> {
            props.setProperty("inspectit.metrics.enabled", "false");
        });
        assertUnavailable("http://localhost:8888/metrics");
        assertNoLogsOfLevelOrGreater(Level.WARN);
    }

    @DirtiesContext
    @Test
    void testLocalSwitch() throws Exception {
        assertGet200("http://localhost:8888/metrics");
        localSwitch(ExporterEnabledState.DISABLED);
        Awaitility.waitAtMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(service.isEnabled()).isFalse());
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
        localSwitch(ExporterEnabledState.DISABLED);
        assertThat(service.isEnabled()).isFalse();
        assertUnavailable("http://localhost:8888/metrics");
        // enable prometheus
        enableService();
        assertThat(service.isEnabled()).isTrue();
        assertGet200("http://localhost:8888/metrics");
    }
}
