package rocks.inspectit.ocelot.instrumentation.http;

import io.opencensus.stats.AggregationData;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

/**
 * uses global-propagation-tests.yml
 */
public class HttpInMetricTest {

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
            resp.setStatus(123);
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

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("http_path", "/servletapi");
            tags.put("http_status", "123");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/in/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/in/responsetime/sum", tags))
                    .getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

    }

}
