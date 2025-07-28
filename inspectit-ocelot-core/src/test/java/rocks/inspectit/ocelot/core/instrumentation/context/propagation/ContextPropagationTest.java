package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.tracing.PropagationFormat;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContextPropagationTest {

    private static final String TRACE_ID = "7e3829a2f67371760000000000000000";

    private static final String TRACE_ID_DATADOG = "9095065227370918262";

    private static final String SPAN_ID = "65727da6f6eb87bf";

    private static final String SPAN_ID_DATADOG = "7310043301236410303";

    private static final TraceState TRACESTATE_DEFAULT = TraceState.getDefault();

    private static final TraceFlags TRACE_OPTIONS = TraceFlags.getDefault();

    private static final String X_B3_TRACE_ID = "X-B3-TraceId";

    private static final String X_B3_SPAN_ID = "X-B3-SpanId";

    private static final String TRACEPARENT = "traceparent";

    private static final String X_DATADOG_TRACE_ID = "X-Datadog-Trace-ID";

    private static final String X_DATADOG_PARENT_ID = "X-Datadog-Parent-ID";

    private final ContextPropagation contextPropagation = ContextPropagation.get();

    private final Predicate<String> ALWAYS_TRUE = (key) -> true;

    private final String BAGGAGE_HEADER = BaggagePropagation.BAGGAGE_HEADER;

    private final String ACCESS_CONTROL_EXPOSE_HEADERS = BaggagePropagation.ACCESS_CONTROL_EXPOSE_HEADERS;

    @Mock
    InspectitContextImpl inspectitContext;

    private String enc(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class ReadPropagatedDataFromHeaderMap {

        @Test
        public void testSingleString() {
            Map<String, String> headers = ImmutableMap.of(BAGGAGE_HEADER, enc("my_val\u00FC") + " =  " + enc("stra\u00DFe=15") + ";someprop=42");

            contextPropagation.readPropagatedDataFromHeaderMap(headers, inspectitContext, ALWAYS_TRUE);

            verify(inspectitContext).setData(eq("my_val\u00FC"), eq("stra\u00DFe=15"));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        public void testSingleLong() {
            Map<String, String> headers = ImmutableMap.of(BAGGAGE_HEADER, enc("x") + " =  " + enc("42") + "; type = l");

            contextPropagation.readPropagatedDataFromHeaderMap(headers, inspectitContext, ALWAYS_TRUE);

            verify(inspectitContext).setData(eq("x"), eq(42L));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        public void testSingleDouble() {
            Map<String, String> headers = ImmutableMap.of(BAGGAGE_HEADER, enc("pi") + " =  " + enc(String.valueOf(Math.PI)) + "; blub=halloooo; type = d");

            contextPropagation.readPropagatedDataFromHeaderMap(headers, inspectitContext, ALWAYS_TRUE);

            verify(inspectitContext).setData(eq("pi"), eq(Math.PI));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        public void testBooleanAndString() {
            Map<String, String> headers = ImmutableMap.of(BAGGAGE_HEADER, "is_something=true;type=b,hello=world");

            contextPropagation.readPropagatedDataFromHeaderMap(headers, inspectitContext, ALWAYS_TRUE);

            verify(inspectitContext).setData(eq("is_something"), eq(true));
            verify(inspectitContext).setData(eq("hello"), eq("world"));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        public void testInvalidTypeIgnored() {
            Map<String, String> headers = ImmutableMap.of(BAGGAGE_HEADER, "is_something=true;type=blub;type=x");

            contextPropagation.readPropagatedDataFromHeaderMap(headers, inspectitContext, ALWAYS_TRUE);

            verify(inspectitContext).setData(eq("is_something"), eq("true"));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        public void testNotConfiguredDataKeyIgnored() {
            Map<String, String> headers = ImmutableMap.of(BAGGAGE_HEADER, "is_something=true");

            contextPropagation.readPropagatedDataFromHeaderMap(headers, inspectitContext, (key) -> false);

            verifyNoInteractions(inspectitContext);
        }

        @Test
        public void testLowerCaseBaggageHeader() {
            String lowerCaseBaggage = BAGGAGE_HEADER.toLowerCase();
            Map<String, String> headers = ImmutableMap.of(lowerCaseBaggage, "someprop=42");

            contextPropagation.readPropagatedDataFromHeaderMap(headers, inspectitContext, ALWAYS_TRUE);

            verify(inspectitContext).setData(eq("someprop"), eq("42"));
            verifyNoMoreInteractions(inspectitContext);
        }
    }

    @Nested
    class BuildPropagationHeaderMap {

        @Test
        public void testSingleString() {

            Map<String, Object> data = ImmutableMap.of("my_val\u00FC", "stra\u00DFe=15");

            Map<String, String> result = contextPropagation.buildUpPropagationHeaderMap(data);

            assertThat(result).hasSize(2)
                    .containsEntry(BAGGAGE_HEADER, enc("my_val\u00FC") + "=" + enc("stra\u00DFe=15"))
                    .containsEntry(ACCESS_CONTROL_EXPOSE_HEADERS, BAGGAGE_HEADER);
        }

        @Test
        public void testSingleLong() {
            Map<String, Object> data = ImmutableMap.of("x", 42L);

            Map<String, String> result = contextPropagation.buildUpPropagationHeaderMap(data);

            assertThat(result).hasSize(2)
                    .containsEntry(BAGGAGE_HEADER, "x=42;type=l")
                    .containsEntry(ACCESS_CONTROL_EXPOSE_HEADERS, BAGGAGE_HEADER);
        }

        @Test
        public void testSingleDouble() {
            Map<String, Object> data = ImmutableMap.of("Pi", Math.PI);

            Map<String, String> result = contextPropagation.buildUpPropagationHeaderMap(data);

            assertThat(result).hasSize(2)
                    .containsEntry(BAGGAGE_HEADER, "Pi=" + Math.PI + ";type=d")
                    .containsEntry(ACCESS_CONTROL_EXPOSE_HEADERS, BAGGAGE_HEADER);
        }

        @Test
        public void testInvalidTypeIgnored() {
            Map<String, Object> data = ImmutableMap.of("Pi", new ArrayList<>());

            Map<String, String> result = contextPropagation.buildUpPropagationHeaderMap(data);

            assertThat(result).hasSize(0);
        }

        @Test
        public void testBooleanAndString() {
            Map<String, Object> data = ImmutableMap.of("hello", "world", "is_something", true);

            Map<String, String> result = contextPropagation.buildUpPropagationHeaderMap(data);

            assertThat(result).hasSize(2)
                    .containsEntry(BAGGAGE_HEADER, "hello=world,is_something=true;type=b")
                    .containsEntry(ACCESS_CONTROL_EXPOSE_HEADERS, BAGGAGE_HEADER);
        }

        @Test
        public void injectHeader_B3Format() {
            contextPropagation.setPropagationFormat(PropagationFormat.B3);

            Map<String, Object> data = Collections.emptyMap();
            SpanContext spanContext = SpanContext.create(TRACE_ID, SPAN_ID, TRACE_OPTIONS, TRACESTATE_DEFAULT);

            Map<String, String> result = contextPropagation.buildDownPropagationHeaderMap(data, spanContext);

            assertThat(result).contains(entry(X_B3_TRACE_ID, TRACE_ID));
            assertThat(result).contains(entry(X_B3_SPAN_ID, SPAN_ID));
        }

        @Test
        public void injectHeader_TraceContextFormat() {
            contextPropagation.setPropagationFormat(PropagationFormat.TRACE_CONTEXT);

            Map<String, Object> data = Collections.emptyMap();
            SpanContext spanContext = SpanContext.create(TRACE_ID, SPAN_ID, TRACE_OPTIONS, TRACESTATE_DEFAULT);

            Map<String, String> result = contextPropagation.buildDownPropagationHeaderMap(data, spanContext);

            String header = "00-" + TRACE_ID + "-" + SPAN_ID + "-00";
            assertThat(result).contains(entry(TRACEPARENT, header));

            contextPropagation.setPropagationFormat(PropagationFormat.TRACE_CONTEXT); // set back to default
        }

        @Test
        public void injectHeader_DatadogFormat() {
            contextPropagation.setPropagationFormat(PropagationFormat.DATADOG);

            Map<String, Object> data = Collections.emptyMap();
            SpanContext spanContext = SpanContext.create(TRACE_ID, SPAN_ID, TRACE_OPTIONS, TRACESTATE_DEFAULT);

            Map<String, String> result = contextPropagation.buildDownPropagationHeaderMap(data, spanContext);

            assertThat(result).contains(entry(X_DATADOG_TRACE_ID, TRACE_ID_DATADOG));
            assertThat(result).contains(entry(X_DATADOG_PARENT_ID, SPAN_ID_DATADOG));

            contextPropagation.setPropagationFormat(PropagationFormat.TRACE_CONTEXT); // set back to default
        }
    }

    @Nested
    public class ReadPropagatedSpanContextFromHeaderMap {

        @Test
        public void readB3Header_sampled() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", TRACE_ID, "X-B3-SpanId", SPAN_ID, "X-B3-Sampled", "1");

            SpanContext spanContext = contextPropagation.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceFlags().isSampled()).isTrue();
        }

        @Test
        public void readB3Header_notSampled_explicit() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", TRACE_ID, "X-B3-SpanId", SPAN_ID, "X-B3-Sampled", "0");

            SpanContext spanContext = contextPropagation.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceFlags().isSampled()).isFalse();
        }

        @Test
        public void readB3Header_notSampled_implicit() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", TRACE_ID, "X-B3-SpanId", SPAN_ID);

            SpanContext spanContext = contextPropagation.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceFlags().isSampled()).isFalse();
        }

        @Test
        public void readB3Header_lowercase() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "x-b3-traceid", TRACE_ID, "x-b3-spanid", SPAN_ID, "x-b3-sampled", "1");

            SpanContext spanContext = contextPropagation.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceFlags().isSampled()).isTrue();
        }

        @Test
        public void readTraceContextHeader_sampled() {
            String header = "00-" + TRACE_ID + "-" + SPAN_ID + "-01";
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "traceparent", header);

            SpanContext spanContext = contextPropagation.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceFlags().isSampled()).isTrue();
        }

        @Test
        public void readTraceContextHeader_notSampled() {
            String header = "00-" + TRACE_ID + "-" + SPAN_ID + "-00";
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "traceparent", header);

            SpanContext spanContext = contextPropagation.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceFlags().isSampled()).isFalse();
        }

        @Test
        public void readDatadogHeader_sampled() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-Datadog-Trace-ID", TRACE_ID_DATADOG, "X-Datadog-Parent-ID", SPAN_ID_DATADOG, "X-Datadog-Sampling-Priority", "1");

            SpanContext spanContext = contextPropagation.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceFlags().isSampled()).isTrue();
        }

        @Test
        public void readDatadogHeader_notSampled_explicit() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-Datadog-Trace-ID", TRACE_ID_DATADOG, "X-Datadog-Parent-ID", SPAN_ID_DATADOG, "X-Datadog-Sampling-Priority", "0");

            SpanContext spanContext = contextPropagation.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceFlags().isSampled()).isFalse();
        }

        @Test
        public void readDatadogHeader_notSampled_implicit() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-Datadog-Trace-ID", TRACE_ID_DATADOG, "X-Datadog-Parent-ID", SPAN_ID_DATADOG);

            SpanContext spanContext = contextPropagation.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceFlags().isSampled()).isFalse();
        }

        @Test
        public void readDatadogHeader_lowercase() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "x-datadog-trace-id", TRACE_ID_DATADOG, "x-datadog-parent-id", SPAN_ID_DATADOG, "x-datadog-sampling-priority", "1");

            SpanContext spanContext = contextPropagation.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceFlags().isSampled()).isTrue();
        }
    }
}
