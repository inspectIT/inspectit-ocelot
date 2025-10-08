package rocks.inspectit.ocelot.instrumentation.servicegraph;

import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import rocks.inspectit.ocelot.utils.TestUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * uses global-propagation-tests.yml
 */
class ServiceInMetricTest extends InstrumentationSysTestBase {

    static final int PORT = 9999;

    static final String TEST_PATH = "/test";

    static final String TEST_URL = "http://localhost:" + PORT + TEST_PATH;

    static final String SERVICE_NAME = "systemtest";

    Server server;

    void fireRequest(String originService) {

        try {
            InternalInspectitContext context = Instances.contextManager.enterNewContext();
            context.setData("service.name", originService);
            context.makeActive();

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.getResponseCode();
            urlConnection.disconnect();

            context.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            resp.setStatus(200);
        }
    }

    @Nested
    class ServletAPI {

        @Test
        void testInternalCallRecording() throws Exception {
            server = new Server(PORT);
            ServletHandler servletHandler = new ServletHandler();
            server.setHandler(servletHandler);
            servletHandler.addServletWithMapping(TestServlet.class, "/*");
            server.start();

            TestUtils.waitForClassInstrumentations(Arrays.asList(HttpServlet.class,
                    Class.forName("sun.net.www.protocol.http.HttpURLConnection")), true, 30, TimeUnit.SECONDS);

            Map<String, String> attributes = new HashMap<>();
            attributes.put("protocol", "http");
            attributes.put("service.name", SERVICE_NAME);
            attributes.put("origin_service", "servlet_origin");

            await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
                fireRequest("servlet_origin");

                assertThat(MetricTestUtils.getDataForHistogramView("service_in_responsetime", attributes))
                        .isNotNull()
                        .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                            assertThat(pointData.getCount()).isEqualTo(1);
                            assertThat(pointData.getSum()).isGreaterThan(0);
                        });
            });

            server.stop();
        }
    }
}
