package rocks.inspectit.oce.metrics;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

public class MetricsSysTestBase {

    @BeforeEach
    void flushMetrics() {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://0.0.0.0:8888/metrics");
        try {
            httpclient.execute(httpGet);
            httpclient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
