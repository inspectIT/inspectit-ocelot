package rocks.inspectit.ocelot.instrumentation.special.remote;

import com.google.common.collect.ImmutableMap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.*;
import org.mockito.internal.util.io.IOUtil;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.utils.TestUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * uses global-propagation-tests.yml
 */
public class ServletApiContextPropagationTest {

    public static final int PORT = 9999;

    public static final String TEST_PATH = "/test";

    public static final String TEST_URL = "http://localhost:" + PORT + TEST_PATH;

    private Server server;

    @BeforeAll
    static void waitForInstrumentation() throws Exception {
        TestUtils.waitForClassInstrumentations(Arrays.asList(TestFilter.class, HttpServlet.class, CloseableHttpClient.class,
                Class.forName("sun.net.www.protocol.http.HttpURLConnection")), true, 30, TimeUnit.SECONDS);
    }

    @BeforeEach
    void setupJetty() {
        server = new Server(PORT);
    }

    @AfterEach
    void cleanup() throws Exception {
        server.stop();
    }

    void startServer(Consumer<ServletHandler> shInitializer) {
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);
        shInitializer.accept(servletHandler);
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestServlet extends HttpServlet {

        public static Map<String, String> lastTags;

        public static String writerResponse;

        public static String outputStreamResponse;

        public static Object upPropagationValue;

        public static void reset() {
            lastTags = null;
            writerResponse = null;
            outputStreamResponse = null;
            upPropagationValue = null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            lastTags = TestUtils.getCurrentTagsAsMap();

            InternalInspectitContext ctx = null;
            try {
                if (upPropagationValue != null) {
                    ctx = Instances.contextManager.enterNewContext();
                    ctx.setData("up_propagated", upPropagationValue);
                    ctx.makeActive();
                }

                if (writerResponse != null) {
                    PrintWriter writer = resp.getWriter();
                    writer.print(writerResponse);
                    writer.flush();
                }

                if (outputStreamResponse != null) {
                    ServletOutputStream sout = resp.getOutputStream();
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(sout));
                    bw.write(outputStreamResponse);
                    bw.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (ctx != null) {
                    ctx.close();
                }
            }

        }

    }

    public static class TestFilter implements Filter {

        public static Map<String, String> lastTags;

        public static Map<String, String> overrideTags;

        public static void reset() {
            lastTags = null;
            overrideTags = null;
        }

        @Override
        public void init(FilterConfig filterConfig) {

        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            lastTags = TestUtils.getCurrentTagsAsMap();
            if (overrideTags != null) {
                InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
                overrideTags.forEach(ctx::setData);
                try {
                    ctx.makeActive();
                    chain.doFilter(request, response);
                } finally {
                    ctx.close();
                }
            } else {
                chain.doFilter(request, response);
            }
        }

        @Override
        public void destroy() {
        }
    }

    @Nested
    class DownPropagation {

        @Test
        void testPropagationViaServlet() throws Exception {
            startServer(sh ->
                    sh.addServletWithMapping(TestServlet.class, "/*")
            );
            TestServlet.reset();

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            urlConnection.setRequestMethod("GET");
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("down_propagated", "hello world");
            ctx.setData("down_propagated_2", "testPropagationViaServlet");
            ctx.makeActive();
            int code = urlConnection.getResponseCode();
            ctx.close();

            assertThat(code).isEqualTo(200);
            assertThat(TestServlet.lastTags).containsEntry("down_propagated", "hello world");
            assertThat(TestServlet.lastTags).containsEntry("down_propagated_2", "testPropagationViaServlet");
        }

        @Test
        void testPropagationHappensOnlyOnce() throws Exception {
            startServer(sh -> {
                sh.addServletWithMapping(TestServlet.class, "/*");
                sh.addFilterWithMapping(TestFilter.class, "/*", FilterMapping.DEFAULT);
            });
            TestServlet.reset();
            TestFilter.reset();

            TestFilter.overrideTags = ImmutableMap.of("down_propagated_2", "overridden!");

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            urlConnection.setRequestMethod("GET");
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("down_propagated", "hello world");
            ctx.setData("down_propagated_2", "testPropagationViaServlet");
            ctx.makeActive();
            int code = urlConnection.getResponseCode();
            ctx.close();

            assertThat(code).isEqualTo(200);
            assertThat(TestFilter.lastTags).containsEntry("down_propagated", "hello world");
            assertThat(TestFilter.lastTags).containsEntry("down_propagated_2", "testPropagationViaServlet");

            assertThat(TestServlet.lastTags).containsEntry("down_propagated", "hello world");
            assertThat(TestServlet.lastTags).containsEntry("down_propagated_2", "overridden!");
        }
    }

    @Nested
    class UpPropagation {

        @Test
        void testUpPropagationWithEmptyResponse() throws Exception {
            startServer(sh ->
                    sh.addServletWithMapping(TestServlet.class, "/*")
            );
            TestServlet.reset();
            TestServlet.upPropagationValue = Math.PI;

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            urlConnection.setRequestMethod("GET");
            String correlHeader = urlConnection.getHeaderField("Correlation-Context");

            assertThat(correlHeader).contains("up_propagated=" + Math.PI + ";type=d");
        }

        @Test
        void testUpPropagationWithResponseViaWriter() throws Exception {
            startServer(sh ->
                    sh.addServletWithMapping(TestServlet.class, "/*"));
            TestServlet.reset();
            TestServlet.upPropagationValue = Math.PI;
            TestServlet.writerResponse = "Hallo Welt!";

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            urlConnection.setRequestMethod("GET");
            String correlHeader = urlConnection.getHeaderField("Correlation-Context");

            assertThat(correlHeader).contains("up_propagated=" + Math.PI + ";type=d");
            assertThat(IOUtil.readLines(urlConnection.getInputStream())).containsExactly("Hallo Welt!");
        }

        @Test
        void testUpPropagationWithResponseViaOutputStream() throws Exception {
            startServer(sh ->
                    sh.addServletWithMapping(TestServlet.class, "/*")
            );
            TestServlet.reset();
            TestServlet.upPropagationValue = Math.PI;
            TestServlet.outputStreamResponse = "Hallo Welt!";

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            urlConnection.setRequestMethod("GET");
            String correlHeader = urlConnection.getHeaderField("Correlation-Context");

            assertThat(correlHeader).contains("up_propagated=" + Math.PI + ";type=d");
            assertThat(IOUtil.readLines(urlConnection.getInputStream())).containsExactly("Hallo Welt!");
        }
    }

}
