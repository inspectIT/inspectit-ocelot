package rocks.inspectit.oce.eum.server.tracing.opentelemtry;

import com.google.protobuf.ByteString;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AttributeKeyValue;
import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OpenTelemetryProtoConverterTest {

    @InjectMocks
    OpenTelemetryProtoConverter converter;

    @Nested
    class Convert {

        @Test
        public void happyPath() {
            final long time = System.nanoTime();
            ByteBuffer traceIdBuffer = ByteBuffer.allocate(2 * Long.BYTES);
            traceIdBuffer.putLong(1);
            traceIdBuffer.putLong(2);
            ByteBuffer spanIdBuffer = ByteBuffer.allocate(Long.BYTES);
            spanIdBuffer.putLong(3);

            ExportTraceServiceRequest data = ExportTraceServiceRequest.newBuilder()
                    .addResourceSpans(ResourceSpans.newBuilder()
                            .setResource(Resource.newBuilder()
                                    .addAttributes(AttributeKeyValue.newBuilder()
                                            .setType(AttributeKeyValue.ValueType.DOUBLE)
                                            .setKey("resource-key")
                                            .setDoubleValue(11.2d)
                                            .build()
                                    )
                                    .build()
                            )
                            .addInstrumentationLibrarySpans(InstrumentationLibrarySpans.newBuilder()
                                    .setInstrumentationLibrary(InstrumentationLibrary.newBuilder()
                                            .setName("test-inst")
                                            .setVersion("0.1.0")
                                            .build()
                                    )
                                    .addSpans(Span.newBuilder()
                                            .setTraceId(ByteString.copyFrom(traceIdBuffer.array()))
                                            .setSpanId(ByteString.copyFrom(spanIdBuffer.array()))
                                            .setName("test")
                                            .setKind(Span.SpanKind.CONSUMER)
                                            .setStartTimeUnixNano(time)
                                            .setEndTimeUnixNano(time + 1L)
                                            .setStatus(Status.newBuilder().setCodeValue(Status.StatusCode.Aborted_VALUE).setMessage("status-desc").build())
                                            .addAttributes(AttributeKeyValue.newBuilder()
                                                    .setType(AttributeKeyValue.ValueType.INT)
                                                    .setKey("a1")
                                                    .setIntValue(11)
                                                    .build()
                                            )
                                            .setDroppedAttributesCount(1)
                                            .addEvents(Span.Event.newBuilder().setName("e1").setTimeUnixNano(time + 2).build())
                                            .addEvents(Span.Event.newBuilder().setName("e2").setTimeUnixNano(time + 3).build())
                                            .setDroppedEventsCount(2)
                                            .setDroppedLinksCount(3)
                                            .build())
                                    .build()
                            )
                            .build()
                    )
                    .build();

            Collection<SpanData> result = converter.convert(data);

            assertThat(result).hasOnlyOneElementSatisfying(spanData -> {
                assertThat(spanData.getResource().getAttributes()).hasSize(1)
                        .containsEntry("resource-key", AttributeValue.doubleAttributeValue(11.2d));
                assertThat(spanData.getInstrumentationLibraryInfo().getName()).isEqualTo("test-inst");
                assertThat(spanData.getInstrumentationLibraryInfo().getVersion()).isEqualTo("0.1.0");
                assertThat(spanData.getTraceId()).isEqualTo(new TraceId(1, 2));
                assertThat(spanData.getSpanId()).isEqualTo(new SpanId(3));
                assertThat(spanData.getKind()).isEqualTo(Kind.CONSUMER);
                assertThat(spanData.getStartEpochNanos()).isEqualTo(time);
                assertThat(spanData.getEndEpochNanos()).isEqualTo(time + 1);
                assertThat(spanData.getEndEpochNanos()).isEqualTo(time + 1);
                assertThat(spanData.getHasEnded()).isEqualTo(true);
                assertThat(spanData.getHasRemoteParent()).isEqualTo(false);
                assertThat(spanData.getStatus()).isEqualTo(io.opentelemetry.trace.Status.ABORTED.withDescription("status-desc"));
                assertThat(spanData.getAttributes()).hasSize(1)
                        .containsEntry("a1", AttributeValue.longAttributeValue(11));
                assertThat(spanData.getTotalAttributeCount()).isEqualTo(2);
                assertThat(spanData.getTimedEvents()).hasSize(2)
                        .anySatisfy(event -> {
                            assertThat(event.getName()).isEqualTo("e1");
                            assertThat(event.getEpochNanos()).isEqualTo(time + 2);
                            assertThat(event.getAttributes()).isEmpty();
                            assertThat(event.getTotalAttributeCount()).isZero();
                        })
                        .anySatisfy(event -> {
                            assertThat(event.getName()).isEqualTo("e2");
                            assertThat(event.getEpochNanos()).isEqualTo(time + 3);
                            assertThat(event.getAttributes()).isEmpty();
                            assertThat(event.getTotalAttributeCount()).isZero();
                        });
                assertThat(spanData.getTotalRecordedEvents()).isEqualTo(4);
                assertThat(spanData.getTotalRecordedLinks()).isEqualTo(3);
            });
        }

        @Test
        public void emptyIgnored() {

            ExportTraceServiceRequest data = ExportTraceServiceRequest.newBuilder()
                    .addResourceSpans(ResourceSpans.newBuilder()
                            .setResource(Resource.newBuilder().build())
                            .addInstrumentationLibrarySpans(InstrumentationLibrarySpans.newBuilder()
                                    .setInstrumentationLibrary(InstrumentationLibrary.newBuilder().build())
                                    .addSpans(Span.newBuilder().build())
                                    .build()
                            )
                            .build()
                    )
                    .build();

            Collection<SpanData> result = converter.convert(data);

            assertThat(result).isEmpty();
        }

    }

    @Nested
    class ToOtTraceId {

        @Test
        public void notEnoughBytes() {
            byte[] bytes = new byte[15];

            Optional<TraceId> traceId = OpenTelemetryProtoConverter.toOtTraceId(ByteString.copyFrom(bytes));

            assertThat(traceId).isEmpty();
        }

    }

    @Nested
    class ToOtSpanId {

        @Test
        public void notEnoughBytes() {
            byte[] bytes = new byte[7];

            Optional<SpanId> traceId = OpenTelemetryProtoConverter.toOtSpanId(ByteString.copyFrom(bytes));

            assertThat(traceId).isEmpty();
        }

    }

    @Nested
    class ToOtSpanKind {

        @Test
        public void states() {
            assertThat(OpenTelemetryProtoConverter.toOtSpanKind(Span.SpanKind.INTERNAL)).isEqualTo(Kind.INTERNAL);
            assertThat(OpenTelemetryProtoConverter.toOtSpanKind(Span.SpanKind.CLIENT)).isEqualTo(Kind.CLIENT);
            assertThat(OpenTelemetryProtoConverter.toOtSpanKind(Span.SpanKind.SERVER)).isEqualTo(Kind.SERVER);
            assertThat(OpenTelemetryProtoConverter.toOtSpanKind(Span.SpanKind.PRODUCER)).isEqualTo(Kind.PRODUCER);
            assertThat(OpenTelemetryProtoConverter.toOtSpanKind(Span.SpanKind.CONSUMER)).isEqualTo(Kind.CONSUMER);
            assertThat(OpenTelemetryProtoConverter.toOtSpanKind(Span.SpanKind.UNRECOGNIZED)).isEqualTo(Kind.INTERNAL);
            assertThat(OpenTelemetryProtoConverter.toOtSpanKind(Span.SpanKind.SPAN_KIND_UNSPECIFIED)).isEqualTo(Kind.INTERNAL);

        }

    }

}