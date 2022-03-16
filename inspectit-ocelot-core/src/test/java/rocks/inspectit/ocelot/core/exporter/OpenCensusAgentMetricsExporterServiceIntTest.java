package rocks.inspectit.ocelot.core.exporter;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import io.github.netmikey.logunit.api.LogCapturer;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.opencensus.proto.agent.metrics.v1.ExportMetricsServiceRequest;
import io.opencensus.proto.agent.metrics.v1.ExportMetricsServiceResponse;
import io.opencensus.proto.agent.metrics.v1.MetricsServiceGrpc;
import io.opencensus.stats.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DirtiesContext
public class OpenCensusAgentMetricsExporterServiceIntTest extends SpringTestBase {

    public static final String HOST = "localhost";

    public static final int PORT = 55678;

    private static Server agent;

    @Autowired
    private StatsRecorder statsRecorder;

    private static FakeOcAgentMetricsServiceGrpcImpl fakeOcAgentMetricsServiceGrpc = new FakeOcAgentMetricsServiceGrpcImpl();

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create()
            .captureForType(OpenCensusAgentMetricsExporterService.class, org.slf4j.event.Level.WARN);

    //@BeforeAll
    public static void setUp() {
        agent = getServer("localhost:55678", fakeOcAgentMetricsServiceGrpc);
    }

    //@AfterAll
    public static void tearDown() {
        agent.shutdown();
    }

    /**
     * To test the client, a fake GRPC server servers the fake class implementation {@link FakeOcAgentMetricsServiceGrpcImpl}.
     */
    //@Test This test is currently deactivated, since the current implementation of the trace exporter tries to connect to a google service before starting
    // and the request runs into a timeout.
    public void testGrpcRequest() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.open-census-agent.address", "localhost:55678");
            props.setProperty("inspectit.exporters.metrics.open-census-agent.use-insecure", "true");
        });
        try {
            agent.start();
            recordDummyMetric();
        } catch (IOException e) {
            e.printStackTrace();
        }
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(fakeOcAgentMetricsServiceGrpc.getExportMetricsServiceRequests().size()).isGreaterThan(0);
        });
    }

    private void recordDummyMetric() {
        MeasureMap mm = statsRecorder.newMeasureMap();
        Measure.MeasureLong measure = Measure.MeasureLong.create("oc-test-metric-name", "metric-description", "unit of metric");
        View view = View.create(View.Name.create("oc-test-metric-name"), "description", measure, Aggregation.Sum.create(), Collections.emptyList());
        Stats.getViewManager().registerView(view);
        mm.put(measure, 20);
        mm.record();
    }

    private static Server getServer(String endPoint, BindableService service) {
        ServerBuilder<?> builder = NettyServerBuilder.forAddress(new InetSocketAddress(HOST, PORT));
        Executor executor = MoreExecutors.directExecutor();
        builder.executor(executor);
        return builder.addService(service).build();
    }

    /**
     * Based on the integration test of the OpenCensusAgentMetricsExporter (https://github.com/census-instrumentation/opencensus-java/tree/master/exporters/metrics/ocagent/)
     */
    static class FakeOcAgentMetricsServiceGrpcImpl extends MetricsServiceGrpc.MetricsServiceImplBase {

        @GuardedBy("this")
        private final List<ExportMetricsServiceRequest> exportMetricsServiceRequests = new ArrayList<>();

        @GuardedBy("this")
        private final StreamObserver<ExportMetricsServiceRequest> exportRequestObserver = new StreamObserver<ExportMetricsServiceRequest>() {
            @Override
            public void onNext(ExportMetricsServiceRequest value) {
                addExportRequest(value);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
            }
        };

        @Override
        public synchronized StreamObserver<ExportMetricsServiceRequest> export(StreamObserver<ExportMetricsServiceResponse> responseObserver) {
            return exportRequestObserver;
        }

        private synchronized void addExportRequest(ExportMetricsServiceRequest request) {
            exportMetricsServiceRequests.add(request);
        }

        synchronized List<ExportMetricsServiceRequest> getExportMetricsServiceRequests() {
            return Collections.unmodifiableList(exportMetricsServiceRequests);
        }
    }

    @DirtiesContext
    @Test
    void testNoAddressSet() throws Exception {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.open-census-agent.address", "");
            // change a second property so change is detected and another try of enabling triggered,
            // since address already is empty by default
            props.setProperty("inspectit.exporters.metrics.open-census-agent.use-insecure", "true");
            props.setProperty("inspectit.exporters.metrics.open-census-agent.enabled", ExporterEnabledState.ENABLED);
        });
        warnLogs.assertContains("'address'");
    }

}
