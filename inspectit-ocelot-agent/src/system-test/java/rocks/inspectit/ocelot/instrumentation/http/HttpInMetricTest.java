package rocks.inspectit.ocelot.instrumentation.http;

import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
public class HttpInMetricTest extends InstrumentationSysTestBase {

    private Server server;

    void fireRequest(String url) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.getResponseMessage();
            urlConnection.disconnect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // 123 was not a valid status code and rejected by jdk 21
            resp.setStatus(200);
        }
    }

    @Nested
    class ServletAPI {

        @Test
        void testRequestRecorded() throws Exception {
            server = new Server(0);
            ServletHandler servletHandler = new ServletHandler();
            server.setHandler(servletHandler);
            servletHandler.addServletWithMapping(TestServlet.class, "/*");
            server.start();

            TestUtils.waitForClassInstrumentations(Arrays.asList(HttpServlet.class,
                    Class.forName("sun.net.www.protocol.http.HttpURLConnection")), true, 30, TimeUnit.SECONDS);

            fireRequest("http://localhost:" + server.getURI().getPort() + "/servletapi");
            server.stop();

            Map<String, String> attributes = new HashMap<>();
            attributes.put("http_path", "/servletapi");
            attributes.put("http_status", "200");

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(MetricTestUtils.getDataForHistogramView("http_in_responsetime", attributes))
                            .isNotNull()
                            .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                                assertThat(pointData.getCount()).isEqualTo(1);
                                assertThat(pointData.getSum()).isGreaterThan(0);
                            })
            );
        }
    }
}
