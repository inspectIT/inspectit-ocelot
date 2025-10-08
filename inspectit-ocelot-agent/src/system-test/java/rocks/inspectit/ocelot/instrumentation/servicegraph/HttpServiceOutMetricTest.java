package rocks.inspectit.ocelot.instrumentation.servicegraph;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
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
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import rocks.inspectit.ocelot.utils.TestUtils;

import javax.jms.*;
import javax.servlet.http.HttpServlet;
import java.net.HttpURLConnection;
import java.net.URI;
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
class HttpServiceOutMetricTest extends InstrumentationSysTestBase {

    static final int PORT = 9999;

    static final String TEST_PATH = "/test";

    static final String TEST_URL = "http://localhost:" + PORT + TEST_PATH;

    static final String SERVICE_NAME = "systemtest"; //configured in agent-overwrites.yml

    WireMockServer wireMockServer;

    static String targetName;

    @BeforeEach
    void setupWiremock() {
        wireMockServer = new WireMockServer(options().port(PORT));
        wireMockServer.start();
        configureFor(wireMockServer.port());

        stubFor(get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse()
                        .withBody("body")
                        .withStatus(200)));

        TestUtils.waitForClassInstrumentation(HttpServlet.class, true, 30, TimeUnit.SECONDS);
    }

    @AfterEach
    void cleanup() {
        wireMockServer.stop();
    }

    @Nested
    class ApacheClient {

        @Test
        void testInternalCallRecording() throws Exception {
            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            HttpClientBuilder builder = HttpClientBuilder.create();
            builder.setDefaultRequestConfig(requestBuilder.build());
            CloseableHttpClient client = builder.build();

            TestUtils.waitForClassInstrumentations(Arrays.asList(
                    CloseableHttpClient.class,
                    Class.forName("org.apache.http.impl.client.InternalHttpClient")), true, 30, TimeUnit.SECONDS);

            InternalInspectitContext serviceOverride = Instances.contextManager.enterNewContext();
            serviceOverride.setData("service.name", "apache_sg_test");
            serviceOverride.makeActive();
            client.execute(URIUtils.extractHost(URI.create(TEST_URL)), new HttpGet(TEST_URL));
            client.close();
            serviceOverride.close();

            Map<String, String> attributes = new HashMap<>();
            attributes.put("protocol", "http");
            attributes.put("service.name", "apache_sg_test");
            attributes.put("target_service", SERVICE_NAME);

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(MetricTestUtils.getDataForHistogramView("service_out_responsetime", attributes))
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

        @Test
        void testInternalCallRecording() throws Exception {
            targetName = "urlconn_test";

            TestUtils.waitForClassInstrumentation(Class.forName("sun.net.www.protocol.http.HttpURLConnection"), true, 30, TimeUnit.SECONDS);

            InternalInspectitContext serviceOverride = Instances.contextManager.enterNewContext();
            serviceOverride.setData("service.name", "httpurlconn_sg_test");
            serviceOverride.makeActive();
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            urlConnection.getResponseCode();
            serviceOverride.close();

            Map<String, String> attributes = new HashMap<>();
            attributes.put("protocol", "http");
            attributes.put("service.name", "httpurlconn_sg_test");
            attributes.put("target_service", SERVICE_NAME);

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(MetricTestUtils.getDataForHistogramView("service_out_responsetime", attributes))
                            .isNotNull()
                            .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                                assertThat(pointData.getCount()).isEqualTo(1);
                                assertThat(pointData.getSum()).isGreaterThan(0);
                            })
            );
        }
    }

    @Nested
    class JmsAPI {
        private BrokerService broker;
        private Connection connection;
        private Session session;
        private MessageProducer producer;
        private final String QUEUE_NAME = "serviceGraphTestQueue";

        @BeforeEach
        void setUp() throws Exception {
            broker = new BrokerService();
            broker.setPersistent(false);
            broker.setUseJmx(false);
            broker.start();

            ConnectionFactory factory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
            connection = factory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createQueue(QUEUE_NAME);
            producer = session.createProducer(queue);

            TestUtils.waitForClassInstrumentations(Arrays.asList(
                    Class.forName("javax.jms.MessageProducer"),
                    Class.forName("javax.jms.MessageListener"),
                    Class.forName("org.apache.activemq.ActiveMQMessageProducer")
            ), true, 30, TimeUnit.SECONDS);
        }

        @AfterEach
        void tearDown() throws Exception {
            connection.close();
            broker.stop();
        }

        @Test
        void testInternalCallRecording() throws Exception {
            InternalInspectitContext serviceOverride = Instances.contextManager.enterNewContext();
            serviceOverride.setData("service.name", "jms_sg_test");
            serviceOverride.makeActive();

            TextMessage message = session.createTextMessage("test");
            producer.send(message);
            serviceOverride.close();


            Map<String, String> attributes = new HashMap<>();
            attributes.put("protocol", "jms");
            attributes.put("service.name", "jms_sg_test");
            attributes.put("target_external", QUEUE_NAME);
            // no target_service tag, since we do not receive an answer from the target because of message queue

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(MetricTestUtils.getDataForHistogramView("service_out_responsetime", attributes))
                            .isNotNull()
                            .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                                assertThat(pointData.getCount()).isEqualTo(1);
                                assertThat(pointData.getSum()).isGreaterThan(0);
                            })
            );
        }
    }
}
