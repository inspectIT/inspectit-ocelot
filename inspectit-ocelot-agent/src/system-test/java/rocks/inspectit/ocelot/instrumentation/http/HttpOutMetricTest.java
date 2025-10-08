package rocks.inspectit.ocelot.instrumentation.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.*;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
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
import static org.awaitility.Awaitility.await;

/**
 * uses global-propagation-tests.yml
 */
class HttpOutMetricTest extends InstrumentationSysTestBase {

    static final String PATH_200 = "/test";

    static final String PATH_500 = "/error";

    static String WIREMOCK_HOST_PORT;

    static String WIREMOCK_URL;

    static WireMockServer wireMockServer;

    @BeforeAll
    static void setupWiremock() {
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
    static void cleanup() {
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
            ctx.setData("service.name", "apache_client_test");
            ctx.makeActive();
            client.execute(new HttpGet(WIREMOCK_URL + PATH_200 + "?x=32423"));
            ctx.close();

            Map<String, String> attributes = new HashMap<>();
            attributes.put("service.name", "apache_client_test");
            attributes.put("http_host", WIREMOCK_HOST_PORT);
            attributes.put("http_path", PATH_200);
            attributes.put("http_status", "200");
            attributes.put("http_method", "GET");
            attributes.put("error", "false");

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(MetricTestUtils.getDataForHistogramView("http_out_responsetime", attributes))
                            .isNotNull()
                            .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                                assertThat(pointData.getCount()).isEqualTo(1);
                                assertThat(pointData.getSum()).isGreaterThan(0);
                            })
            );
        }

        @Test
        void testErrorStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service.name", "apache_client_test");
            ctx.makeActive();
            client.execute(new HttpGet(WIREMOCK_URL + PATH_500 + "?x=32423"));
            ctx.close();

            Map<String, String> attributes = new HashMap<>();
            attributes.put("service.name", "apache_client_test");
            attributes.put("http_host", WIREMOCK_HOST_PORT);
            attributes.put("http_path", PATH_500);
            attributes.put("http_status", "500");
            attributes.put("http_method", "GET");
            attributes.put("error", "true");

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(MetricTestUtils.getDataForHistogramView("http_out_responsetime", attributes))
                            .isNotNull()
                            .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                                assertThat(pointData.getCount()).isEqualTo(1);
                                assertThat(pointData.getSum()).isGreaterThan(0);
                            })
            );
        }

        @Test
        void testExceptionStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service.name", "apache_client_test");
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

            Map<String, String> attributes = new HashMap<>();
            attributes.put("service.name", "apache_client_test");
            attributes.put("http_host", "idontexist");
            attributes.put("http_path", "");
            attributes.put("http_status", caughtException.getClass().getSimpleName());
            attributes.put("http_method", "GET");
            attributes.put("error", "true");

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(MetricTestUtils.getDataForHistogramView("http_out_responsetime", attributes))
                            .isNotNull()
                            .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                                assertThat(pointData.getCount()).isEqualTo(1);
                                assertThat(pointData.getSum()).isGreaterThan(0);
                            })
            );
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
            ctx.setData("service.name", "urlconn_client_test");
            ctx.makeActive();

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(WIREMOCK_URL + PATH_200 + "?x=32423").openConnection();
            urlConnection.getResponseCode();

            ctx.close();

            Map<String, String> attributes = new HashMap<>();
            attributes.put("service.name", "urlconn_client_test");
            attributes.put("http_host", WIREMOCK_HOST_PORT);
            attributes.put("http_path", PATH_200);
            attributes.put("http_status", "200");
            attributes.put("http_method", "GET");
            attributes.put("error", "false");

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(MetricTestUtils.getDataForHistogramView("http_out_responsetime", attributes))
                            .isNotNull()
                            .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                                assertThat(pointData.getCount()).isEqualTo(1);
                                assertThat(pointData.getSum()).isGreaterThan(0);
                            })
            );
        }

        @Test
        void testErrorStatus() throws Exception {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service.name", "urlconn_client_test");
            ctx.makeActive();

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(WIREMOCK_URL + PATH_500 + "?x=32423").openConnection();
            urlConnection.getResponseCode();

            ctx.close();

            Map<String, String> attributes = new HashMap<>();
            attributes.put("service.name", "urlconn_client_test");
            attributes.put("http_host", WIREMOCK_HOST_PORT);
            attributes.put("http_path", PATH_500);
            attributes.put("http_status", "500");
            attributes.put("http_method", "GET");
            attributes.put("error", "true");

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(MetricTestUtils.getDataForHistogramView("http_out_responsetime", attributes))
                            .isNotNull()
                            .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                                assertThat(pointData.getCount()).isEqualTo(1);
                                assertThat(pointData.getSum()).isGreaterThan(0);
                            })
            );
        }

        @Test
        void testExceptionStatus() {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service.name", "urlconn_client_test");
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

            Map<String, String> attributes = new HashMap<>();
            attributes.put("service.name", "urlconn_client_test");
            attributes.put("http_host", "idontexist");
            attributes.put("http_path", "");
            attributes.put("http_status", caughtException.getClass().getSimpleName());
            attributes.put("http_method", "GET");
            attributes.put("error", "true");

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(MetricTestUtils.getDataForHistogramView("http_out_responsetime", attributes))
                            .isNotNull()
                            .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                                assertThat(pointData.getCount()).isEqualTo(1);
                                assertThat(pointData.getSum()).isGreaterThan(0);
                            })
            );
        }
    }
}
