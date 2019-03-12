package rocks.inspectit.oce.instrumentation.special;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opencensus.common.Scope;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.bootstrap.context.IInspectitContext;
import rocks.inspectit.oce.utils.TestUtils;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * uses global-propagation-tests.yml
 */
public class ApacheHttpClientContextPropagationTest {

    public static final int PORT = 9999;
    public static final String TEST_URL = "http://localhost:" + PORT + "/test";
    private WireMockServer wireMockServer;

    private CloseableHttpClient client;


    @BeforeEach
    void setupAndInstrumentClient() throws Exception {
        //make sure the actual implementation gets loaded
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());

        client = builder.build();

        TestUtils.waitForInstrumentationToComplete();
    }

    @BeforeEach
    void setupWiremock() throws Exception {
        wireMockServer = new WireMockServer(options().port(PORT));
        wireMockServer.start();
        configureFor(wireMockServer.port());
    }

    @AfterEach
    void cleanup() throws Exception {
        wireMockServer.stop();
        client.close();
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
        void propagationViaSimpleExecute() throws Exception {
            try (Scope s = Tags.getTagger().emptyBuilder()
                    .put(TagKey.create("down_propagated"), TagValue.create("myvalue"))
                    .buildScoped()) {
                client.execute(URIUtils.extractHost(URI.create(TEST_URL)), new HttpGet(TEST_URL));
            }

            verify(getRequestedFor(urlEqualTo("/test"))
                    .withHeader("Correlation-Context", equalTo("down_propagated=myvalue")));
        }

    }


    @Nested
    class UpPropagation {

        @BeforeEach
        void setupResponse() {

            stubFor(get(urlEqualTo("/test"))
                    .willReturn(aResponse()
                            .withHeader("Correlation-Context", "up_propagated=" + Math.PI + ";type=d")
                            .withHeader("Correlation-Context", "up_propagated2 = Hello World ")
                            .withBody("body")
                            .withStatus(200)));
        }

        @Test
        void testUpPropagation() throws Exception {
            IInspectitContext myCtx = Instances.contextManager.enterNewContext();
            myCtx.makeActive();

            client.execute(URIUtils.extractHost(URI.create(TEST_URL)), new HttpGet(TEST_URL));

            myCtx.close();

            assertThat(myCtx.getData("up_propagated")).isEqualTo(Math.PI);
            assertThat(myCtx.getData("up_propagated2")).isEqualTo("Hello World");
        }


    }


}
