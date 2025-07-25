package rocks.inspectit.ocelot.instrumentation.servicegraph;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opencensus.stats.AggregationData;
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

/**
 * uses global-propagation-tests.yml
 */
public class HttpServiceOutMetricTest {

    public static final int PORT = 9999;

    public static final String TEST_PATH = "/test";

    public static final String TEST_URL = "http://localhost:" + PORT + TEST_PATH;

    public static final String SERVICE_NAME = "systemtest"; //configured in agent-overwrites.yml

    private WireMockServer wireMockServer;

    public static String targetName;

    @BeforeEach
    void setupWiremock() throws Exception {
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
    void cleanup() throws Exception {
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

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("protocol", "http");
            tags.put("service.name", "apache_sg_test");
            tags.put("target_service", SERVICE_NAME);

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("service/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("service/out/responsetime/sum", tags))
                    .getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
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

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("protocol", "http");
            tags.put("service.name", "httpurlconn_sg_test");
            tags.put("target_service", SERVICE_NAME);

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("service/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("service/out/responsetime/sum", tags))
                    .getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
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

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("protocol", "jms");
            tags.put("service.name", "jms_sg_test");
            tags.put("target_external", QUEUE_NAME);
            // no target_service tag, since we do receive an answer from the target because of message queue

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("service/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("service/out/responsetime/sum", tags))
                    .getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }
    }
}