package rocks.inspectit.ocelot.core.exporter;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {
        "inspectit.exporters.tracing.zipkin.url=http://127.0.0.1:9411/api/v2/spans"
})
@DirtiesContext
public class ZipKinExporterServiceIntTest extends SpringTestBase {

    public static final int ZIPKIN_PORT = 9411;
    public static final String ZIPKIN_PATH = "/api/v2/spans";
    private WireMockServer wireMockServer;

    @BeforeEach
    void setupWiremock() {
        wireMockServer = new WireMockServer(options().port(ZIPKIN_PORT));
        wireMockServer.start();
        configureFor(wireMockServer.port());
        stubFor(get(urlPathEqualTo(ZIPKIN_PATH))
                .willReturn(aResponse()
                        .withStatus(200)));

    }

    @AfterEach
    void cleanup() {
        wireMockServer.stop();
    }

    @Test
    void verifyTraceSent() {
        Tracing.getTracer().spanBuilder("zipkinspan")
                .setSampler(Samplers.alwaysSample())
                .startSpanAndRun(() -> {
                });
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postRequestedFor(urlPathEqualTo(ZIPKIN_PATH)).withRequestBody(containing("zipkinspan")));
        });
    }

}
