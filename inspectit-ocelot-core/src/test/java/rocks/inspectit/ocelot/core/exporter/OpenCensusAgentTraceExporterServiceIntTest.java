package rocks.inspectit.ocelot.core.exporter;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import io.github.netmikey.logunit.api.LogCapturer;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.opencensus.proto.agent.trace.v1.*;
import io.opencensus.proto.trace.v1.ConstantSampler;
import io.opencensus.proto.trace.v1.TraceConfig;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.core.SpringTestBase;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {"inspectit.exporters.tracing.open-census-agent.address=localhost:55678", "inspectit.exporters.tracing.open-census-agent.use-insecure=true",})
@DirtiesContext
public class OpenCensusAgentTraceExporterServiceIntTest extends SpringTestBase {

    public static final String HOST = "localhost";

    public static final int PORT = 55678;

    public static final String SPAN_NAME = "ocagentspan";

    private static Server agent;

    private static FakeOcAgentTraceServiceGrpcImpl fakeOcAgentTraceServiceGrpc = new FakeOcAgentTraceServiceGrpcImpl();

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create()
            .captureForType(OpenCensusAgentTraceExporterService.class, org.slf4j.event.Level.WARN);

    //@BeforeAll
    public static void setUp() {
        agent = getServer("localhost:55678", fakeOcAgentTraceServiceGrpc);
    }

    //@AfterAll
    public static void tearDown() {
        agent.shutdown();
    }

    /**
     * To test the client, a fake GRPC server servers the fake class implementation {@link FakeOcAgentTraceServiceGrpcImpl}.
     */
    //@Test This test is currently deactivated, since the current implementation of the trace exporter tries to connect to a google service before starting
    // and the request runs into a timeout.
    public void testGrpcRequest() {
        try {
            agent.start();
            Tracing.getTracer().spanBuilder(SPAN_NAME).setSampler(Samplers.alwaysSample()).startSpanAndRun(() -> {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(fakeOcAgentTraceServiceGrpc.getExportTraceServiceRequests()).anySatisfy((req) -> assertThat(req.getSpansList()).anySatisfy((span) -> assertThat(span.getName()
                    .getValue()).isEqualTo(SPAN_NAME)));
        });
    }

    private static Server getServer(String endPoint, BindableService service) {
        ServerBuilder<?> builder = NettyServerBuilder.forAddress(new InetSocketAddress(HOST, PORT));
        Executor executor = MoreExecutors.directExecutor();
        builder.executor(executor);
        return builder.addService(service).build();
    }

    /**
     * Based on the integration test of the OpenCensusAgentTraceExporter (https://github.com/census-instrumentation/opencensus-java/tree/master/exporters/trace/ocagent)
     */
    static class FakeOcAgentTraceServiceGrpcImpl extends TraceServiceGrpc.TraceServiceImplBase {

        // Default updatedLibraryConfig uses an always sampler.
        @GuardedBy("this")
        private UpdatedLibraryConfig updatedLibraryConfig = UpdatedLibraryConfig.newBuilder()
                .setConfig(TraceConfig.newBuilder()
                        .setConstantSampler(ConstantSampler.newBuilder()
                                .setDecision(ConstantSampler.ConstantDecision.ALWAYS_ON)
                                .build())
                        .build())
                .build();

        @GuardedBy("this")
        private final List<CurrentLibraryConfig> currentLibraryConfigs = new ArrayList<>();

        @GuardedBy("this")
        private final CopyOnWriteArrayList<ExportTraceServiceRequest> exportTraceServiceRequests = new CopyOnWriteArrayList<>();

        @GuardedBy("this")
        private final AtomicReference<StreamObserver<UpdatedLibraryConfig>> configRequestObserverRef = new AtomicReference<>();

        @GuardedBy("this")
        private final StreamObserver<CurrentLibraryConfig> configResponseObserver = new StreamObserver<CurrentLibraryConfig>() {
            @Override
            public void onNext(CurrentLibraryConfig value) {
                addCurrentLibraryConfig(value);
                try {
                    // Do not send UpdatedLibraryConfigs too frequently.
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendUpdatedLibraryConfig();
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                resetConfigRequestObserverRef();
            }

            @Override
            public void onCompleted() {
                resetConfigRequestObserverRef();
            }
        };

        @GuardedBy("this")
        private final StreamObserver<ExportTraceServiceRequest> exportRequestObserver = new StreamObserver<ExportTraceServiceRequest>() {
            @Override
            public void onNext(ExportTraceServiceRequest value) {
                addExportRequest(value);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();

            }

            @Override
            public void onCompleted() {
            }
        };

        @GuardedBy("this")
        private CountDownLatch countDownLatch;

        @Override
        public synchronized StreamObserver<CurrentLibraryConfig> config(StreamObserver<UpdatedLibraryConfig> updatedLibraryConfigStreamObserver) {
            configRequestObserverRef.set(updatedLibraryConfigStreamObserver);
            return configResponseObserver;
        }

        @Override
        public synchronized StreamObserver<ExportTraceServiceRequest> export(StreamObserver<ExportTraceServiceResponse> exportTraceServiceResponseStreamObserver) {
            return exportRequestObserver;
        }

        private synchronized void addCurrentLibraryConfig(CurrentLibraryConfig currentLibraryConfig) {
            if (countDownLatch != null && countDownLatch.getCount() == 0) {
                return;
            }
            currentLibraryConfigs.add(currentLibraryConfig);
        }

        private synchronized void addExportRequest(ExportTraceServiceRequest request) {
            exportTraceServiceRequests.add(request);
        }

        // Returns the stored ExportTraceServiceRequests.
        synchronized List<ExportTraceServiceRequest> getExportTraceServiceRequests() {
            return exportTraceServiceRequests;
        }

        private synchronized void sendUpdatedLibraryConfig() {
            @Nullable StreamObserver<UpdatedLibraryConfig> configRequestObserver = configRequestObserverRef.get();
            if (configRequestObserver != null) {
                configRequestObserver.onNext(updatedLibraryConfig);
            }
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }

        private synchronized void resetConfigRequestObserverRef() {
            configRequestObserverRef.set(null);
        }
    }

    @DirtiesContext
    @Test
    void testNoAddressSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.open-census-agent.address", "");
            props.setProperty("inspectit.exporters.tracing.open-census-agent.enabled", ExporterEnabledState.ENABLED);
        });
        warnLogs.assertContains("'address'");
    }
}
