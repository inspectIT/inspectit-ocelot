package rocks.inspectit.oce.core.exporter;

import ch.qos.logback.classic.Level;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.oce.core.SpringTestBase;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class PrometheusExporterServiceIntTest extends SpringTestBase {

    private static final int HTTP_TIMEOUT = 1000;

    private static CloseableHttpClient testClient;

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
        int statusCode = testClient.execute(new HttpGet(url)).getStatusLine().getStatusCode();

        assertThat(statusCode).isEqualTo(200);
    }

    void assertUnavailable(String url) throws Exception {
        Throwable throwable = catchThrowable(() -> testClient.execute(new HttpGet(url)).getStatusLine().getStatusCode());

        assertThat(throwable).isInstanceOf(IOException.class);
    }

    @Test
    void testDefaultSettings() throws Exception {
        assertGet200("http://localhost:8888/metrics");
        assertNoLogsOfLevelorGreater(Level.WARN);
    }


    @DirtiesContext
    @Test
    void testMasterSwitch() throws Exception {
        updateProperties(props -> {
            props.setProperty("inspectit.metrics.enabled", "false");
        });
        assertUnavailable("http://localhost:8888/metrics");
        assertNoLogsOfLevelorGreater(Level.WARN);
    }

    @DirtiesContext
    @Test
    void testLocalSwitch() throws Exception {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.prometheus.enabled", "false");
        });
        assertUnavailable("http://localhost:8888/metrics");
        assertNoLogsOfLevelorGreater(Level.WARN);
    }


    @DirtiesContext
    @Test
    void testChangePort() throws Exception {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.prometheus.port", "8899");
        });
        assertUnavailable("http://localhost:8888/metrics");
        assertGet200("http://localhost:8899/metrics");
        assertNoLogsOfLevelorGreater(Level.WARN);
    }
}
