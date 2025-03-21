package rocks.inspectit.ocelot.core.exporter;

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import io.opencensus.stats.*;
import io.opencensus.tags.*;
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
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.Testcontainers.exposeHostPorts;

/**
 * Base class for exporter integration tests. Verifies integration with the OpenTelemetry Collector.
 * The Collector can be configured to accept the required data over gRPC or HTTP and exports the data over gRPC
 * to a server running in process, allowing assertions to be made against the data.
 * This class is based on the {@link io.opentelemetry.integrationtest.OtlpExporterIntegrationTest}
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class ExporterServiceIntegrationTestBase extends SpringTestBase {

    static final String COLLECTOR_TAG = "0.100.0";

    static final String COLLECTOR_IMAGE = "otel/opentelemetry-collector-contrib:" + COLLECTOR_TAG;

    protected static final Integer COLLECTOR_OTLP_GRPC_PORT = 4317;

    static final Integer COLLECTOR_OTLP_HTTP_PORT = 4318;

    static final Integer COLLECTOR_HEALTH_CHECK_PORT = 13133;

    static final int COLLECTOR_ZIPKIN_PORT = 9411;

    static final String INSTRUMENTATION_NAME = "rocks.inspectit.ocelot.instrumentation";

    static final String INSTRUMENTATION_VERSION = "0.0.1";

    private static final Logger LOGGER = Logger.getLogger(ExporterServiceIntegrationTestBase.class.getName());

    /**
     * The {@link OtlpGrpcServer} used as an exporter endpoint for the OpenTelemetry Collector
     */
    static OtlpGrpcServer grpcServer;

    /**
     * The OpenTelemetry Collector
     */
    static GenericContainer<?> collector;

    @Autowired
    InspectitEnvironment environment;

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
                .withClasspathResourceMapping("otel-config.yaml", "/otel-config.yaml", BindMode.READ_ONLY)
                .withCommand("--config", "/otel-config.yaml")
                .withLogConsumer(outputFrame -> LOGGER.log(Level.INFO, "COLLECTOR: " + outputFrame.getUtf8String().replace("\n", "")))
                // expose all relevant ports
                .withExposedPorts(COLLECTOR_OTLP_GRPC_PORT, COLLECTOR_OTLP_HTTP_PORT, COLLECTOR_HEALTH_CHECK_PORT, COLLECTOR_ZIPKIN_PORT)
                .waitingFor(Wait.forHttp("/").forPort(COLLECTOR_HEALTH_CHECK_PORT));

        //collector.withStartupTimeout(Duration.of(1, ChronoUnit.MINUTES));
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
    static Tracer getOtelTracer() {
        return GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
    }

    /**
     * Gets the desired endpoint of the {@link #collector} constructed as
     * 'http://{@link GenericContainer#getHost() collector.getHost()}:{@link GenericContainer#getMappedPort(int) collector.getMappedPort(port)}/path'
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
     * Gets the desired endpoint of the {@link #collector} constructed as
     * 'http://{@link GenericContainer#getHost() collector.getHost()}:{@link GenericContainer#getMappedPort(int) collector.getMappedPort(port)}'
     *
     * @param originalPort the port to get the actual mapped port for
     *
     * @return the constructed endpoint for the {@link #collector}
     */
    protected static String getEndpoint(Integer originalPort) {
        return String.format("http://%s:%d", collector.getHost(), collector.getMappedPort(originalPort));
    }

    /**
     * Creates a nested trace with parent and child span and flushes them.
     *
     * @param parentSpanName the name of the parent {@link Span}
     * @param childSpanName  the name of the child {@link Span}
     */
    void makeSpansAndFlush(String parentSpanName, String childSpanName) {
        // start span and nested span
        Span parentSpan = getOtelTracer().spanBuilder(parentSpanName).startSpan();
        try (Scope scope = parentSpan.makeCurrent()) {
            Span childSpan = getOtelTracer().spanBuilder(childSpanName).startSpan();
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

    protected Measure.MeasureLong createMeasure(String measureName, String tagKey) {
        Measure.MeasureLong measure = Measure.MeasureLong.create(measureName, "desc", "1");

        View view = View.create(View.Name.create(measureName), "desc", measure, Aggregation.Sum.create(),
                Collections.singletonList(TagKey.create(tagKey)));
        Stats.getViewManager().registerView(view);

        return measure;
    }

    /**
     * Records a sum with the given value and tag.
     * Since we are still not using OTel to create metrics with the agent, we should stick to OC in tests.
     *
     * @param measure the measure to record
     * @param value  the value to add to the measure
     * @param tagKey the key of the tag
     * @param tagVal the value of the tag
     */
    protected void recordMeasureAndFlush(Measure.MeasureLong measure, int value, String tagKey, String tagVal) {
        TagContext tagContext = Tags.getTagger().emptyBuilder()
                .putLocal(TagKey.create(tagKey), TagValue.create(tagVal))
                .build();

        Stats.getStatsRecorder().newMeasureMap()
                .put(measure, value)
                .record(tagContext);

        Instances.openTelemetryController.flush();
    }

    /**
     * Verifies that the metric with the given value and key/value attribute (tag) has been exported to and received
     * by the {@link #grpcServer}
     *
     * @param measureName the name of the measure
     * @param value  the value of the measure
     * @param tagKey the key of the tag
     * @param tagVal the value of the tag
     */
    protected void awaitMetricsExported(String measureName, int value, String tagKey, String tagVal) {
        // create the attribute that we will use to verify that the metric has been written
        KeyValue attribute = KeyValue.newBuilder()
                .setKey(tagKey)
                .setValue(AnyValue.newBuilder().setStringValue(tagVal).build())
                .build();

        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(grpcServer.metricRequests.stream())
                        .anyMatch(mReq -> mReq.getResourceMetricsList().stream()
                            .anyMatch(rm ->  rm.getScopeMetrics(0)
                                .getMetricsList().stream()
                                // check for the specific attribute and value
                                .anyMatch(metric -> {
                                    boolean validName = metric.getName().equals(measureName);
                                    boolean validData = metric.getSum()
                                            .getDataPointsList()
                                            .stream()
                                            .anyMatch(d -> d.getAttributesList()
                                                    .contains(attribute) && d.getAsInt() == value);
                                    return validName && validData;
                                }))));
    }

    /**
     * Waits for the spans to be exported to and received by the {@link #grpcServer}. This method asserts that Spans
     * with the given names exist and that the child's {@link io.opentelemetry.proto.trace.v1.Span#getParentSpanId()}
     * equals the parent's {@link io.opentelemetry.proto.trace.v1.Span#getSpanId()}
     *
     * @param parentSpanName the name of the parent span
     * @param childSpanName  the name of the child span
     */
    void awaitSpansExported(String parentSpanName, String childSpanName) {

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {

            // get a flat list of spans
            Stream<List<io.opentelemetry.proto.trace.v1.Span>> spansLis = grpcServer.traceRequests.stream()
                    .flatMap(tr -> tr.getResourceSpansList()
                            .stream()
                            .flatMap(rs -> rs.getScopeSpansList()
                                    .stream()
                                    .map(ils -> ils.getSpansList())));

            // assert that parent and child span are present and that the parent's spanId equals the child's parentSpanId
            assertThat(spansLis.anyMatch(s -> {
                Optional<io.opentelemetry.proto.trace.v1.Span> childSpan = s.stream()
                        .filter(span -> span.getName().equals(childSpanName))
                        .findFirst();
                if (!childSpan.isPresent()) {
                    return false;
                }

                Optional<io.opentelemetry.proto.trace.v1.Span> parentSpan = s.stream()
                        .filter(span -> span.getName().equals(parentSpanName))
                        .findFirst();
                if (!parentSpan.isPresent()) {
                    return false;
                }

                return childSpan.get().getParentSpanId().equals(parentSpan.get().getSpanId());

            })).isTrue();

        });

    }

    /**
     * OpenTelemetry Protocol gRPC Server
     */
    public static class OtlpGrpcServer extends ServerExtension {

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
                        ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(message);
                        traceRequests.add(request);
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
                        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.parseFrom(message);
                        metricRequests.add(request);
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
                        ExportLogsServiceRequest request = ExportLogsServiceRequest.parseFrom(message);
                        logRequests.add(request);
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
