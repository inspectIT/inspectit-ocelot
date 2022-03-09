package rocks.inspectit.ocelot.core.exporter;

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.testcontainers.Testcontainers.exposeHostPorts;

/**
 * Base class for exporter integration tests. Verifies integration with the OpenTelemetry Collector. The Collector can be configured to accept the required data over gRPC or HTTP and exports the data over gRPC to a server running in process, allowing assertions to be made against the data.
 * This class is based on the {@link io.opentelemetry.integrationtest.OtlpExporterIntegrationTest}
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class ExporterServiceIntegrationTestBase extends SpringTestBase {

    static final String COLLECTOR_IMAGE = "ghcr.io/open-telemetry/opentelemetry-java/otel-collector";

    static final Integer COLLECTOR_OTLP_GRPC_PORT = 4317;

    static final Integer COLLECTOR_OTLP_HTTP_PORT = 4318;

    static final Integer COLLECTOR_HEALTH_CHECK_PORT = 13133;

    static final Integer COLLECTOR_JAEGER_GRPC_PORT = 14250;

    static final Integer COLLECTOR_JAEGER_THRIFT_HTTP_PORT = 14268;

    static final Integer COLLECTOR_JAEGER_THRIFT_BINARY_PORT = 6832;

    static final Integer COLLECTOR_JAEGER_THRIFT_COMPACT_PORT = 6831;

    static final Integer COLLECTOR_PROMETHEUS_PORT = 8888;

    static final String INSTRUMENTATION_NAME = "rocks.inspectit.ocelot.instrumentation";

    static final String INSTRUMENTATION_VERSION = "0.0.1";

    static final Resource RESOURCE = Resource.getDefault()
            .toBuilder()
            .put(ResourceAttributes.SERVICE_NAME, "OTEL integration test")
            .build();

    private static final Logger LOGGER = Logger.getLogger(ExporterServiceIntegrationTestBase.class.getName());

    /**
     * The {@link OtlpGrpcServer} used as an exporter endpoirt for the OpenTelemetry Collector
     */
    static OtlpGrpcServer grpcServer;

    /**
     * The OpenTelemetry Collector
     */
    static GenericContainer<?> collector;

    @BeforeAll
    static void startCollector() {

        // start the gRPC server
        grpcServer = new OtlpGrpcServer();
        grpcServer.start();

        // Expose the port the in-process OTLP gRPC server will run on before the collector is
        // initialized so the collector can connect to it.
        exposeHostPorts(grpcServer.httpPort());

        collector = new GenericContainer<>(DockerImageName.parse(COLLECTOR_IMAGE)).withEnv("LOGGING_EXPORTER_LOG_LEVEL", "INFO")
                .withEnv("OTLP_EXPORTER_ENDPOINT", "host.testcontainers.internal:" + grpcServer.httpPort())
                .withEnv("PROMETHEUS_SCRAPE_TARGET", String.format("host.testcontainers.internal:%s", COLLECTOR_PROMETHEUS_PORT))
                .withClasspathResourceMapping("otel-config.yaml", "/otel-config.yaml", BindMode.READ_ONLY)
                .withCommand("--config", "/otel-config.yaml")
                .withLogConsumer(outputFrame -> LOGGER.log(Level.INFO, outputFrame.getUtf8String().replace("\n", "")))
                // expose all relevant ports
                .withExposedPorts(COLLECTOR_OTLP_GRPC_PORT, COLLECTOR_OTLP_HTTP_PORT, COLLECTOR_HEALTH_CHECK_PORT, COLLECTOR_JAEGER_THRIFT_HTTP_PORT, COLLECTOR_JAEGER_THRIFT_BINARY_PORT, COLLECTOR_JAEGER_THRIFT_COMPACT_PORT, COLLECTOR_JAEGER_GRPC_PORT, COLLECTOR_PROMETHEUS_PORT)
                .waitingFor(Wait.forHttp("/").forPort(COLLECTOR_HEALTH_CHECK_PORT));

        // collector.withStartupTimeout(Duration.of(1, ChronoUnit.MINUTES));
        // note: in case you receive the 'Caused by: org.testcontainers.containers.ContainerLaunchException: Timed out waiting for container port to open' exception,
        // uncomment the above line. The exception is probably caused by Docker Desktop hiccups and should only appear locally.
        collector.start();
    }

    @AfterAll
    static void stop() {
        grpcServer.stop().join();
        collector.stop();
    }

    @BeforeEach
    void reset() {
        grpcServer.reset();
    }

    /**
     * The current {@link GlobalOpenTelemetry#getTracer(String)}
     *
     * @return The {@link Tracer} registered at {@link GlobalOpenTelemetry}
     */
    static Tracer getTracer() {
        return GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
    }

    /**
     * Gets the desired endpoint of the {@link #collector} constructed as 'http://{@link GenericContainer#getHost() collector.getHost()}:{@link GenericContainer#getMappedPort(int) collector.getMappedPort(port)}/path'
     *
     * @param originalPort the port to get the actual mapped port for
     * @param path         the path
     *
     * @return the constructed endpoint for the {@link #collector}
     */
    static String getEndpoint(Integer originalPort, String path) {
        return String.format("http://%s:%d/%s", collector.getHost(), collector.getMappedPort(originalPort), path.startsWith("/") ? path.substring(1) : path);
    }

    /**
     * Creates a nested trace with parent and child span and flushes them.
     */
    void makeSpansAndFlush() {
        // start span and nested span
        Span parentSpan = getTracer().spanBuilder("openTelemetryParentSpan").startSpan();
        try (Scope scope = parentSpan.makeCurrent()) {
            Span childSpan = getTracer().spanBuilder("openTelemetryChildSpan").startSpan();
            try (Scope child = childSpan.makeCurrent()) {
                // do sth
            } finally {
                childSpan.end();
            }
        } finally {
            parentSpan.end();
        }

        // flush pending spans
        Instances.openTelemetryController.flush();
    }

    /**
     * OpenTelemetry Protocol gRPC Server
     */
    static class OtlpGrpcServer extends ServerExtension {

        final List<ExportTraceServiceRequest> traceRequests = new ArrayList<>();

        final List<ExportMetricsServiceRequest> metricRequests = new ArrayList<>();

        final List<ExportLogsServiceRequest> logRequests = new ArrayList<>();

        private void reset() {
            traceRequests.clear();
            metricRequests.clear();
            logRequests.clear();
        }

        @Override
        protected void configure(ServerBuilder sb) {
            sb.service("/opentelemetry.proto.collector.trace.v1.TraceService/Export", new AbstractUnaryGrpcService() {
                @Override
                protected CompletionStage<byte[]> handleMessage(ServiceRequestContext ctx, byte[] message) {
                    try {
                        traceRequests.add(ExportTraceServiceRequest.parseFrom(message));
                    } catch (InvalidProtocolBufferException e) {
                        throw new UncheckedIOException(e);
                    }
                    return completedFuture(ExportTraceServiceResponse.getDefaultInstance().toByteArray());
                }
            });
            sb.service("/opentelemetry.proto.collector.metrics.v1.MetricsService/Export", new AbstractUnaryGrpcService() {
                @Override
                protected CompletionStage<byte[]> handleMessage(ServiceRequestContext ctx, byte[] message) {
                    try {
                        metricRequests.add(ExportMetricsServiceRequest.parseFrom(message));
                    } catch (InvalidProtocolBufferException e) {
                        throw new UncheckedIOException(e);
                    }
                    return completedFuture(ExportMetricsServiceResponse.getDefaultInstance().toByteArray());
                }
            });
            sb.service("/opentelemetry.proto.collector.logs.v1.LogsService/Export", new AbstractUnaryGrpcService() {
                @Override
                protected CompletionStage<byte[]> handleMessage(ServiceRequestContext ctx, byte[] message) {
                    try {
                        logRequests.add(ExportLogsServiceRequest.parseFrom(message));
                    } catch (InvalidProtocolBufferException e) {
                        throw new UncheckedIOException(e);
                    }
                    return completedFuture(ExportLogsServiceResponse.getDefaultInstance().toByteArray());
                }
            });
            sb.http(0);
        }
    }

}
