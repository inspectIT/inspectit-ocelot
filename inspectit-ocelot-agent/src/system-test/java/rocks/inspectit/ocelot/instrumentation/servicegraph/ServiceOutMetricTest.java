package rocks.inspectit.ocelot.instrumentation.servicegraph;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opencensus.stats.AggregationData;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.IInspectitContext;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * uses global-propagation-tests.yml
 */
public class ServiceOutMetricTest {

    public static final int PORT = 9999;
    public static final String TEST_PATH = "/test";
    public static final String TEST_URL = "http://localhost:" + PORT + TEST_PATH;
    public static final String SERVICE_NAME = "systemtest"; //configured in agent-overwrites.yml
    private WireMockServer wireMockServer;

    public static String targetName;

    @BeforeEach
    void setupWiremock() throws Exception {
        wireMockServer = new WireMockServer(options().port(PORT));
        wireMockServer.addMockServiceRequestListener((req, resp) -> {
            IInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("prop_target_service", targetName);
            ctx.makeActive();
            ctx.close();
        });
        wireMockServer.start();
        configureFor(wireMockServer.port());

        stubFor(get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse()
                        .withBody("body")
                        .withStatus(200)));
    }

    @AfterEach
    void cleanup() throws Exception {
        wireMockServer.stop();
    }

    @Nested
    class ApacheClient {

        @Test
        void testInternalCallRecording() throws Exception {
            targetName = "apache_test";

            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            HttpClientBuilder builder = HttpClientBuilder.create();
            builder.setDefaultRequestConfig(requestBuilder.build());
            CloseableHttpClient client = builder.build();

            TestUtils.waitForClassInstrumentation(CloseableHttpClient.class, 10, TimeUnit.SECONDS);

            client.execute(URIUtils.extractHost(URI.create(TEST_URL)), new HttpGet(TEST_URL));
            client.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("protocol", "http");
            tags.put("service", SERVICE_NAME);
            tags.put("target_service", targetName);

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("service/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("service/out/responsetime/sum", tags)).getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }
    }


}