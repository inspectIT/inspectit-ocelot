package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opencensus.trace.Span;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpRemoteTracingTest extends TraceTestBase {

    public static final int PORT = 9999;

    public static final String TEST_PATH = "/test";

    public static final String TEST_URL = "http://localhost:" + PORT + TEST_PATH;

    private static Server server;

    @BeforeAll
    static void setupJetty() {
        server = new Server(PORT);
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);
        servletHandler.addServletWithMapping(TracingServlet.class, "/*");
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void cleanup() throws Exception {
        server.stop();
    }

    public static class TracingServlet extends HttpServlet {

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            super.service(req, res);
            myHandler();
        }

        public void myHandler() {

        }
    }

    @Nested
    class HttpUrlConnectionTest {

        void clientSpan() {
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
                urlConnection.setRequestMethod("GET");
                int code = urlConnection.getResponseCode();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void testPropagationViaServlet() throws Exception {

            TestUtils.waitForClassInstrumentation(HttpUrlConnectionTest.class, true, 15, TimeUnit.SECONDS);
            TestUtils.waitForClassInstrumentation(TracingServlet.class, true, 15, TimeUnit.SECONDS);
            TestUtils.waitForClassInstrumentation(Class.forName("sun.net.www.protocol.http.HttpURLConnection"), true, 30, TimeUnit.SECONDS);

            clientSpan();

            assertTraceExported((spans) ->
                    assertThat(spans)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).endsWith("HttpUrlConnectionTest.clientSpan");
                                assertThat(sp.getKind()).isEqualTo(Span.Kind.CLIENT);
                                assertThat(sp.getParentSpanId()).isNull();
                            })
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).endsWith("TracingServlet.myHandler");
                                assertThat(sp.getKind()).isEqualTo(Span.Kind.SERVER);
                                assertThat(sp.getParentSpanId()).isNotNull();
                            })

            );

        }

    }

    @Nested
    class ApacheClientConnectionTest {

        void clientSpan() {
            try {
                RequestConfig.Builder requestBuilder = RequestConfig.custom();
                HttpClientBuilder builder = HttpClientBuilder.create();
                builder.setDefaultRequestConfig(requestBuilder.build());

                CloseableHttpClient client = builder.build();

                client.execute(URIUtils.extractHost(URI.create(TEST_URL)), new HttpGet(TEST_URL));

                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void testPropagationViaServlet() throws Exception {

            TestUtils.waitForClassInstrumentations(Arrays.asList(
                    CloseableHttpClient.class,
                    Class.forName("org.apache.http.impl.client.InternalHttpClient"),
                    ApacheClientConnectionTest.class,
                    TracingServlet.class), true, 15, TimeUnit.SECONDS);
            clientSpan();

            assertTraceExported((spans) ->
                    assertThat(spans)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).endsWith("ApacheClientConnectionTest.clientSpan");
                                assertThat(sp.getKind()).isEqualTo(Span.Kind.CLIENT);
                                assertThat(sp.getParentSpanId()).isNull();
                            })
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).endsWith("TracingServlet.myHandler");
                                assertThat(sp.getKind()).isEqualTo(Span.Kind.SERVER);
                                assertThat(sp.getParentSpanId()).isNotNull();
                            })

            );

        }

    }
}

