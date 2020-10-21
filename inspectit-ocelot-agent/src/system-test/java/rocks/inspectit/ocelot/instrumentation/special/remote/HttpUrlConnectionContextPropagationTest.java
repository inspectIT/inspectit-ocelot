package rocks.inspectit.ocelot.instrumentation.special.remote;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opencensus.common.Scope;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
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
public class HttpUrlConnectionContextPropagationTest {

    public static final int PORT = 9999;

    public static final String TEST_URL = "http://localhost:" + PORT + "/test";

    private WireMockServer wireMockServer;

    private final Map<String, Object> dataToPropagate = new HashMap<>();

    @BeforeAll
    static void waitForInstrumentation() throws Exception {
        //make sure the actual implementation gets loaded
        HttpURLConnection urlConnection = (HttpURLConnection) new URL("http://google.com").openConnection();
        urlConnection.getResponseCode();

        TestUtils.waitForClassInstrumentations(Arrays.asList(Class.forName("sun.net.www.protocol.http.HttpURLConnection")), true,
                30, TimeUnit.SECONDS);

    }

    @BeforeEach
    void setupWiremock() throws Exception {
        wireMockServer = new WireMockServer(options().port(PORT));
        wireMockServer.addMockServiceRequestListener((req, resp) -> {
            InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
            dataToPropagate.forEach(ctx::setData);
            ctx.makeActive();
            ctx.close();
        });
        wireMockServer.start();
        configureFor(wireMockServer.port());
    }

    @AfterEach
    void cleanup() throws Exception {
        dataToPropagate.clear();
        wireMockServer.stop();
    }

    @Nested
    class DownPropagation {

        @BeforeEach
        void setupResponse() {

            stubFor(get(urlEqualTo("/test"))
                    .willReturn(aResponse()
                            .withStatus(200)));
        }

        @Test
        void propagationViaGetResponseCode() throws Exception {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("down_propagated"), TagValue.create("myvalue"))
                    .buildScoped()) {
                urlConnection.getResponseCode();
            }

            verify(getRequestedFor(urlEqualTo("/test"))
                    .withHeader("Correlation-Context", containing("down_propagated=myvalue")));
        }

        @Test
        void propagationViaGetInputStream() throws Exception {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("down_propagated"), TagValue.create("myvalue"))
                    .buildScoped()) {

                urlConnection.getInputStream();
            }

            verify(getRequestedFor(urlEqualTo("/test"))
                    .withHeader("Correlation-Context", containing("down_propagated=myvalue")));
        }

        @Test
        void propagationViaConnect() throws Exception {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("down_propagated"), TagValue.create("myvalue"))
                    .buildScoped()) {

                urlConnection.connect();

            }

            urlConnection.getInputStream();

            verify(getRequestedFor(urlEqualTo("/test"))
                    .withHeader("Correlation-Context", containing("down_propagated=myvalue")));
        }

        @Test
        void propagationViaGetHeaderField() throws Exception {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("down_propagated"), TagValue.create("myvalue"))
                    .buildScoped()) {

                urlConnection.getHeaderField("some-header");
            }

            verify(getRequestedFor(urlEqualTo("/test"))
                    .withHeader("Correlation-Context", containing("down_propagated=myvalue")));
        }

    }

    @Nested
    class UpPropagation {

        @BeforeEach
        void setupResponse() {

            stubFor(get(urlEqualTo("/test"))
                    .willReturn(aResponse()
                            .withBody("body")
                            .withStatus(200)));
            dataToPropagate.put("up_propagated", Math.PI);
            dataToPropagate.put("up_propagated2", "Hello World");
        }

        @Test
        void propagationViaGetResponseCode() throws Exception {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();

            InternalInspectitContext myCtx = Instances.contextManager.enterNewContext();
            myCtx.makeActive();

            urlConnection.getResponseCode();

            myCtx.close();

            assertThat(myCtx.getData("up_propagated")).isEqualTo(Math.PI);
            assertThat(myCtx.getData("up_propagated2")).isEqualTo("Hello World");
        }

        @Test
        void propagationViaGetHeaderField() throws Exception {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();

            InternalInspectitContext myCtx = Instances.contextManager.enterNewContext();
            myCtx.makeActive();

            urlConnection.getHeaderField("some-header");

            myCtx.close();

            assertThat(myCtx.getData("up_propagated")).isEqualTo(Math.PI);
            assertThat(myCtx.getData("up_propagated2")).isEqualTo("Hello World");
        }

        @Test
        void propagationViaGetHeaderFields() throws Exception {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();

            InternalInspectitContext myCtx = Instances.contextManager.enterNewContext();
            myCtx.makeActive();

            urlConnection.getHeaderFields();

            myCtx.close();

            assertThat(myCtx.getData("up_propagated")).isEqualTo(Math.PI);
            assertThat(myCtx.getData("up_propagated2")).isEqualTo("Hello World");
        }

        @Test
        void propagationViaGetResponseMessage() throws Exception {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();

            InternalInspectitContext myCtx = Instances.contextManager.enterNewContext();
            myCtx.makeActive();

            urlConnection.getResponseMessage();

            myCtx.close();

            assertThat(myCtx.getData("up_propagated")).isEqualTo(Math.PI);
            assertThat(myCtx.getData("up_propagated2")).isEqualTo("Hello World");
        }

        @Test
        void propagationViaGetInputStream() throws Exception {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();

            InternalInspectitContext myCtx = Instances.contextManager.enterNewContext();
            myCtx.makeActive();

            urlConnection.getInputStream();

            myCtx.close();

            assertThat(myCtx.getData("up_propagated")).isEqualTo(Math.PI);
            assertThat(myCtx.getData("up_propagated2")).isEqualTo("Hello World");
        }

    }

}
