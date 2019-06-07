package rocks.inspectit.ocelot.instrumentation.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opencensus.stats.AggregationData;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * uses global-propagation-tests.yml
 */
public class HttpOutMetricTest {


    public static final int PORT = 9999;
    public static final String PATH_200 = "/test";
    public static final String PATH_404 = "/error";
    public static final String HOST = "localhost:" + PORT; //configured in agent-overwrites.yml
    public static final String URL_START = "http://" + HOST; //configured in agent-overwrites.yml

    private WireMockServer wireMockServer;

    public static String targetName;

    @BeforeEach
    void setupWiremock() throws Exception {
        wireMockServer = new WireMockServer(options().port(PORT));
        wireMockServer.addMockServiceRequestListener((req, resp) -> {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("prop_target_service", targetName);
            ctx.makeActive();
            ctx.close();
        });
        wireMockServer.start();
        configureFor(wireMockServer.port());

        stubFor(get(urlPathEqualTo(PATH_404))
                .willReturn(aResponse()
                        .withStatus(404)));
        stubFor(get(urlPathEqualTo(PATH_200))
                .willReturn(aResponse()
                        .withStatus(200)));

    }

    @AfterEach
    void cleanup() {
        wireMockServer.stop();
    }

    @Nested
    class ApacheClient {

        CloseableHttpClient client;

        @BeforeEach
        void setupClient() throws Exception {
            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            HttpClientBuilder builder = HttpClientBuilder.create();
            builder.setDefaultRequestConfig(requestBuilder.build());
            client = builder.build();

            TestUtils.waitForClassInstrumentations(Arrays.asList(
                    CloseableHttpClient.class,
                    Class.forName("org.apache.http.impl.client.InternalHttpClient")),
                    15, TimeUnit.SECONDS);
        }

        @AfterEach
        void destroyClient() throws Exception {
            client.close();
        }

        @Test
        void testSuccessStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "apache_client_test");
            ctx.makeActive();
            client.execute(new HttpGet(URL_START + PATH_200 + "?x=32423"));
            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "apache_client_test");
            tags.put("http_host", HOST);
            tags.put("http_path", PATH_200);
            tags.put("http_status", "200");
            tags.put("http_method", "GET");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags)).getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

        @Test
        void testErrorStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "apache_client_test");
            ctx.makeActive();
            client.execute(new HttpGet(URL_START + PATH_404 + "?x=32423"));
            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "apache_client_test");
            tags.put("http_host", HOST);
            tags.put("http_path", PATH_404);
            tags.put("http_status", "404");
            tags.put("http_method", "GET");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags)).getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

    }


    @Nested
    class HttpUrlConnection {


        @BeforeEach
        void setupClient() throws Exception {
            TestUtils.waitForClassInstrumentation(Class.forName("sun.net.www.protocol.http.HttpURLConnection"), 10, TimeUnit.SECONDS);
        }

        @Test
        void testSuccessStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "urlconn_client_test");
            ctx.makeActive();

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(URL_START + PATH_200 + "?x=32423").openConnection();
            urlConnection.getResponseCode();

            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "urlconn_client_test");
            tags.put("http_host", HOST);
            tags.put("http_path", PATH_200);
            tags.put("http_status", "200");
            tags.put("http_method", "GET");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags)).getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

        @Test
        void testErrorStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "urlconn_client_test");
            ctx.makeActive();

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(URL_START + PATH_404 + "?x=32423").openConnection();
            urlConnection.getResponseCode();

            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "urlconn_client_test");
            tags.put("http_host", HOST);
            tags.put("http_path", PATH_404);
            tags.put("http_status", "404");
            tags.put("http_method", "GET");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags)).getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

    }


}