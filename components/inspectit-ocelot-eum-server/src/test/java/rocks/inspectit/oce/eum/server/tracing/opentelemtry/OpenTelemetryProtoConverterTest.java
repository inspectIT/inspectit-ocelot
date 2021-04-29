package rocks.inspectit.oce.eum.server.tracing.opentelemtry;

import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import static io.opentelemetry.api.common.AttributeKey.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenTelemetryProtoConverterTest {

    public static final String TRACE_REQUEST_FILE_SMALL = "/ot-trace-small-v0.18.2.json";

    public static final String TRACE_REQUEST_FILE_LARGE = "/ot-trace-large-v0.18.2.json";

    @InjectMocks
    OpenTelemetryProtoConverter converter;

    private ExportTraceServiceRequest getSmallTestRequest() throws Exception {
        return getTestRequest(TRACE_REQUEST_FILE_SMALL);
    }

    private ExportTraceServiceRequest getLargeTestRequest() throws Exception {
        return getTestRequest(TRACE_REQUEST_FILE_LARGE);
    }

    private ExportTraceServiceRequest getTestRequest(String file) throws Exception {
        InputStream resource = this.getClass().getResourceAsStream(file);
        String traceRequestJson = IOUtils.toString(resource, StandardCharsets.UTF_8);

        ExportTraceServiceRequest.Builder requestBuilder = ExportTraceServiceRequest.newBuilder();
        JsonFormat.parser().merge(traceRequestJson, requestBuilder);
        return requestBuilder.build();
    }

    @Nested
    class Convert {

        @Test
        public void convertSmallRequest() throws Exception {
            ExportTraceServiceRequest request = getSmallTestRequest();

            Collection<SpanData> result = converter.convert(request);

            assertThat(result).hasSize(1);
            SpanData spanData = result.iterator().next();

            assertThat(spanData.getTraceId()).isEqualTo("5f3d4ba1b6330649b4f84df35e1eab36");
            assertThat(spanData.getSpanId()).isEqualTo("3d6356351f8377ef");
            assertThat(spanData.getParentSpanId()).isEqualTo("0000000000000000");
            assertThat(spanData.getName()).isEqualTo("doSomething");
            assertThat(spanData.getKind()).isEqualTo(SpanKind.INTERNAL);
            assertThat(spanData.getStartEpochNanos()).isEqualTo(1619160764591125000L);
            assertThat(spanData.getEndEpochNanos()).isEqualTo(1619160764608015000L);
            assertThat(spanData.hasEnded()).isTrue();
            assertThat(spanData.getAttributes().isEmpty()).isTrue();
            assertThat(spanData.getTotalAttributeCount()).isEqualTo(0);
            assertThat(spanData.getEvents()).isEmpty();
            assertThat(spanData.getTotalRecordedEvents()).isEqualTo(0);
            assertThat(spanData.getStatus()).isEqualTo(StatusData.unset());
            assertThat(spanData.getLinks()).isEmpty();
            assertThat(spanData.getTotalRecordedLinks()).isEqualTo(0);

            InstrumentationLibraryInfo libraryInfo = spanData.getInstrumentationLibraryInfo();
            assertThat(libraryInfo.getName()).isEqualTo("my-library-name");
            assertThat(libraryInfo.getVersion()).isBlank();

            io.opentelemetry.sdk.resources.Resource resource = spanData.getResource();
            assertThat(resource.getAttributes()
                    .asMap()).containsExactly(entry(stringKey("service.name"), "my-application"), entry(stringKey("telemetry.sdk.language"), "webjs"), entry(stringKey("telemetry.sdk.name"), "opentelemetry"), entry(stringKey("telemetry.sdk.version"), "0.18.2"));
        }

        @Test
        public void convertLargeRequest() throws Exception {
            ExportTraceServiceRequest request = getLargeTestRequest();

            Collection<SpanData> result = converter.convert(request);

            assertThat(result).hasSize(1);
            SpanData spanData = result.iterator().next();

            //@formatter:off
            assertThat(spanData.getTraceId()).isEqualTo("03c2a546267d1e90d70269bdc02babef");
            assertThat(spanData.getSpanId()).isEqualTo("c29e6dd2a1e1e7ae");
            assertThat(spanData.getParentSpanId()).isEqualTo("915c20356ab50086");
            assertThat(spanData.getName()).isEqualTo("HTTP GET");
            assertThat(spanData.getKind()).isEqualTo(SpanKind.CLIENT);
            assertThat(spanData.getStartEpochNanos()).isEqualTo(1619166153906575000L);
            assertThat(spanData.getEndEpochNanos()).isEqualTo(1619166154225390000L);
            assertThat(spanData.hasEnded()).isTrue();
            assertThat(spanData.getAttributes()
                    .asMap()).containsOnly(entry(stringKey("http.method"), "GET"), entry(longKey("http.response_content_length"), 665L), entry(booleanKey("is.true"), true));

            assertThat(spanData.getTotalRecordedEvents()).isEqualTo(1);
            assertThat(spanData.getEvents()).hasSize(1);
            EventData event = spanData.getEvents().get(0);
            assertThat(event.getName()).isEqualTo("open");
            assertThat(event.getEpochNanos()).isEqualTo(1619166153906635000L);
            assertThat(event.getDroppedAttributesCount()).isEqualTo(0);
            assertThat(event.getTotalAttributeCount()).isEqualTo(1);
            assertThat(event.getAttributes().asMap()).containsOnly(entry(stringKey("event.name"), "test-name"));

            assertThat(spanData.getStatus()).isEqualTo(StatusData.ok());
            assertThat(spanData.getLinks()).isEmpty();
            assertThat(spanData.getTotalRecordedLinks()).isEqualTo(2);

            InstrumentationLibraryInfo libraryInfo = spanData.getInstrumentationLibraryInfo();
            assertThat(libraryInfo.getName()).isEqualTo("@opentelemetry/instrumentation-xml-http-request");
            assertThat(libraryInfo.getVersion()).isEqualTo("0.18.2");

            io.opentelemetry.sdk.resources.Resource resource = spanData.getResource();
            assertThat(resource.getAttributes()
                    .asMap()).containsExactly(entry(stringKey("service.name"), "my-application"), entry(stringKey("telemetry.sdk.language"), "webjs"), entry(stringKey("telemetry.sdk.name"), "opentelemetry"), entry(stringKey("telemetry.sdk.version"), "0.18.2"));
            //@formatter:on
        }

        @Test
        public void emptyIgnored() {
            ExportTraceServiceRequest data = ExportTraceServiceRequest.newBuilder()
                    .addResourceSpans(ResourceSpans.newBuilder()
                            .setResource(Resource.newBuilder().build())
                            .addInstrumentationLibrarySpans(InstrumentationLibrarySpans.newBuilder()
                                    .setInstrumentationLibrary(InstrumentationLibrary.newBuilder().build())
                                    .addSpans(Span.newBuilder().build())
                                    .build())
                            .build())
                    .build();

            Collection<SpanData> result = converter.convert(data);

            assertThat(result).isEmpty();
        }

    }

    @Nested
    class AnonymizeIpAddress {

        @Mock
        private HttpServletRequest mockRequest;

        @BeforeEach
        private void beforeEach() {
            converter.requestSupplier = () -> mockRequest;
        }

        @Test
        public void ipv4_singleDigit() {
            when(mockRequest.getRemoteAddr()).thenReturn("127.1.1.1");

            Map<String, String> result = converter.getCustomSpanAttributes();

            assertThat(result).containsExactly(entry("client.ip", "127.1.1.0"));
        }

        @Test
        public void ipv4_multipleDigits() {
            when(mockRequest.getRemoteAddr()).thenReturn("127.1.1.254");

            Map<String, String> result = converter.getCustomSpanAttributes();

            assertThat(result).containsExactly(entry("client.ip", "127.1.1.0"));
        }

        @Test
        public void ipv6_mask() {
            when(mockRequest.getRemoteAddr()).thenReturn("1:2:3:4:5:6:7:8");

            Map<String, String> result = converter.getCustomSpanAttributes();

            assertThat(result).containsExactly(entry("client.ip", "1:2:3:4:5:0:0:0"));
        }
    }
}