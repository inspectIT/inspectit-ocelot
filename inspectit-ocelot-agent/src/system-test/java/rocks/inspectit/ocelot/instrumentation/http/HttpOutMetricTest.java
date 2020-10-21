package rocks.inspectit.ocelot.instrumentation.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opencensus.stats.AggregationData;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.*;
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

    private static final String PATH_200 = "/test";

    private static final String PATH_500 = "/error";

    private static String WIREMOCK_HOST_PORT;

    private static String WIREMOCK_URL;

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void setupWiremock() {
        wireMockServer = new WireMockServer(options().dynamicPort());

        wireMockServer.start();
        configureFor(wireMockServer.port());

        stubFor(get(urlPathEqualTo(PATH_500))
                .willReturn(aResponse()
                        .withStatus(500)));
        stubFor(get(urlPathEqualTo(PATH_200))
                .willReturn(aResponse()
                        .withStatus(200)));

        WIREMOCK_HOST_PORT = "localhost:" + wireMockServer.port();
        WIREMOCK_URL = "http://" + WIREMOCK_HOST_PORT;
    }

    @AfterAll
    public static void cleanup() {
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
                    Class.forName("org.apache.http.impl.client.InternalHttpClient")), true,
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
            client.execute(new HttpGet(WIREMOCK_URL + PATH_200 + "?x=32423"));
            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "apache_client_test");
            tags.put("http_host", WIREMOCK_HOST_PORT);
            tags.put("http_path", PATH_200);
            tags.put("http_status", "200");
            tags.put("http_method", "GET");
            tags.put("error", "false");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags))
                    .getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

        @Test
        void testErrorStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "apache_client_test");
            ctx.makeActive();
            client.execute(new HttpGet(WIREMOCK_URL + PATH_500 + "?x=32423"));
            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "apache_client_test");
            tags.put("http_host", WIREMOCK_HOST_PORT);
            tags.put("http_path", PATH_500);
            tags.put("http_status", "500");
            tags.put("http_method", "GET");
            tags.put("error", "true");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags))
                    .getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

        @Test
        void testExceptionStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "apache_client_test");
            ctx.makeActive();
            Exception caughtException = null;
            try {
                HttpGet request = new HttpGet("http://idontexist");
                request.setConfig(RequestConfig.custom().setConnectTimeout(1000).build());
                client.execute(request);
            } catch (Exception e) {
                caughtException = e;
            }
            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "apache_client_test");
            tags.put("http_host", "idontexist");
            tags.put("http_path", "");
            tags.put("http_status", caughtException.getClass().getSimpleName());
            tags.put("http_method", "GET");
            tags.put("error", "true");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags))
                    .getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }
    }

    @Nested
    class HttpUrlConnection {

        @BeforeEach
        void setupClient() throws Exception {
            TestUtils.waitForClassInstrumentation(Class.forName("sun.net.www.protocol.http.HttpURLConnection"), true, 30, TimeUnit.SECONDS);
        }

        @Test
        void testSuccessStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "urlconn_client_test");
            ctx.makeActive();

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(WIREMOCK_URL + PATH_200 + "?x=32423").openConnection();
            urlConnection.getResponseCode();

            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "urlconn_client_test");
            tags.put("http_host", WIREMOCK_HOST_PORT);
            tags.put("http_path", PATH_200);
            tags.put("http_status", "200");
            tags.put("http_method", "GET");
            tags.put("error", "false");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags))
                    .getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

        @Test
        void testErrorStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "urlconn_client_test");
            ctx.makeActive();

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(WIREMOCK_URL + PATH_500 + "?x=32423").openConnection();
            urlConnection.getResponseCode();

            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "urlconn_client_test");
            tags.put("http_host", WIREMOCK_HOST_PORT);
            tags.put("http_path", PATH_500);
            tags.put("http_status", "500");
            tags.put("http_method", "GET");
            tags.put("error", "true");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags))
                    .getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

        @Test
        void testExceptionStatus() {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "urlconn_client_test");
            ctx.makeActive();

            Exception caughtException = null;
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL("http://idontexist").openConnection();
                urlConnection.setConnectTimeout(1000);
                urlConnection.getResponseCode();
            } catch (Exception e) {
                caughtException = e;
            }

            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "urlconn_client_test");
            tags.put("http_host", "idontexist");
            tags.put("http_path", "");
            tags.put("http_status", caughtException.getClass().getSimpleName());
            tags.put("http_method", "GET");
            tags.put("error", "true");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags))
                    .getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }
    }
}