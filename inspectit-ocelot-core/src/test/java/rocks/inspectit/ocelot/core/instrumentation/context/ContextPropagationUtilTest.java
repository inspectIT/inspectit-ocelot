package rocks.inspectit.ocelot.core.instrumentation.context;

import com.google.common.collect.ImmutableMap;
import io.opencensus.trace.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.tracing.PropagationFormat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static rocks.inspectit.ocelot.core.instrumentation.context.ContextPropagationUtil.CORRELATION_CONTEXT_HEADER;

@ExtendWith(MockitoExtension.class)
public class ContextPropagationUtilTest {

    private static final String TRACE_ID = "7e3829a2f67371760000000000000000";

    private static final String TRACE_ID_DATADOG = "9095065227370918262";

    private static final String SPAN_ID = "65727da6f6eb87bf";

    private static final String SPAN_ID_DATADOG = "7310043301236410303";

    private static final Tracestate TRACESTATE_DEFAULT = Tracestate.builder().build();
    private static final TraceOptions TRACE_OPTIONS = TraceOptions.DEFAULT;

    private static final String X_B3_TRACE_ID = "X-B3-TraceId";
    private static final String X_B3_SPAN_ID = "X-B3-SpanId";

    private static final String TRACEPARENT = "traceparent";

    private static final String X_DATADOG_TRACE_ID = "X-Datadog-Trace-ID";
    private static final String X_DATADOG_PARENT_ID = "X-Datadog-Parent-ID";

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
            Map<String, String> headers = ImmutableMap.of(CORRELATION_CONTEXT_HEADER,
                    enc("my_val\u00FC") + " =  " + enc("stra\u00DFe=15") + ";someprop=42");

            ContextPropagationUtil.readPropagatedDataFromHeaderMap(headers, inspectitContext);

            verify(inspectitContext).setData(eq("my_val\u00FC"), eq("stra\u00DFe=15"));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        public void testSingleLong() {
            Map<String, String> headers = ImmutableMap.of(CORRELATION_CONTEXT_HEADER,
                    enc("x") + " =  " + enc("42") + "; type = l");

            ContextPropagationUtil.readPropagatedDataFromHeaderMap(headers, inspectitContext);

            verify(inspectitContext).setData(eq("x"), eq(42L));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        public void testSingleDouble() {
            Map<String, String> headers = ImmutableMap.of(CORRELATION_CONTEXT_HEADER,
                    enc("pi") + " =  " + enc(String.valueOf(Math.PI)) + "; blub=halloooo; type = d");

            ContextPropagationUtil.readPropagatedDataFromHeaderMap(headers, inspectitContext);

            verify(inspectitContext).setData(eq("pi"), eq(Math.PI));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        public void testBooleanAndString() {
            Map<String, String> headers = ImmutableMap.of(CORRELATION_CONTEXT_HEADER,
                    "is_something=true;type=b,hello=world");

            ContextPropagationUtil.readPropagatedDataFromHeaderMap(headers, inspectitContext);

            verify(inspectitContext).setData(eq("is_something"), eq(true));
            verify(inspectitContext).setData(eq("hello"), eq("world"));
            verifyNoMoreInteractions(inspectitContext);
        }


        @Test
        public void testInvalidTypeIgnored() {
            Map<String, String> headers = ImmutableMap.of(CORRELATION_CONTEXT_HEADER,
                    "is_something=true;type=blub;type=x");

            ContextPropagationUtil.readPropagatedDataFromHeaderMap(headers, inspectitContext);

            verify(inspectitContext).setData(eq("is_something"), eq("true"));
            verifyNoMoreInteractions(inspectitContext);
        }

    }


    @Nested
    class BuildPropagationHeaderMap {

        @Test
        public void testSingleString() {

            Map<String, Object> data = ImmutableMap.of("my_val\u00FC", "stra\u00DFe=15");

            Map<String, String> result = ContextPropagationUtil.buildPropagationHeaderMap(data.entrySet().stream(), null);

            assertThat(result)
                    .hasSize(1)
                    .containsEntry(CORRELATION_CONTEXT_HEADER, enc("my_val\u00FC") + "=" + enc("stra\u00DFe=15"));
        }

        @Test
        public void testSingleLong() {
            Map<String, Object> data = ImmutableMap.of("x", 42L);

            Map<String, String> result = ContextPropagationUtil.buildPropagationHeaderMap(data.entrySet().stream(), null);

            assertThat(result)
                    .hasSize(1)
                    .containsEntry(CORRELATION_CONTEXT_HEADER, "x=42;type=l");
        }

        @Test
        public void testSingleDouble() {
            Map<String, Object> data = ImmutableMap.of("Pi", Math.PI);

            Map<String, String> result = ContextPropagationUtil.buildPropagationHeaderMap(data.entrySet().stream(), null);

            assertThat(result)
                    .hasSize(1)
                    .containsEntry(CORRELATION_CONTEXT_HEADER, "Pi=" + Math.PI + ";type=d");
        }

        @Test
        public void testInvalidTypeIgnored() {
            Map<String, Object> data = ImmutableMap.of("Pi", new ArrayList<>());

            Map<String, String> result = ContextPropagationUtil.buildPropagationHeaderMap(data.entrySet().stream(), null);

            assertThat(result).hasSize(0);
        }

        @Test
        public void testBooleanAndString() {
            Map<String, Object> data = ImmutableMap.of("hello", "world", "is_something", true);

            Map<String, String> result = ContextPropagationUtil.buildPropagationHeaderMap(data.entrySet().stream(), null);

            assertThat(result)
                    .hasSize(1)
                    .containsEntry(CORRELATION_CONTEXT_HEADER, "hello=world,is_something=true;type=b");
        }

        @Test
        public void injectHeader_B3Format() {
            ContextPropagationUtil.setPropagationFormat(PropagationFormat.B3);

            Map<String, Object> data = Collections.emptyMap();
            SpanContext spanContext = SpanContext.create(TraceId.fromLowerBase16(TRACE_ID), SpanId.fromLowerBase16(SPAN_ID), TRACE_OPTIONS, TRACESTATE_DEFAULT);

            Map<String, String> result = ContextPropagationUtil.buildPropagationHeaderMap(data.entrySet().stream(), spanContext);

            assertThat(result).contains(entry(X_B3_TRACE_ID, TRACE_ID));
            assertThat(result).contains(entry(X_B3_SPAN_ID, SPAN_ID));
        }

        @Test
        public void injectHeader_TraceContextFormat() {
            ContextPropagationUtil.setPropagationFormat(PropagationFormat.TRACE_CONTEXT);

            Map<String, Object> data = Collections.emptyMap();
            SpanContext spanContext = SpanContext.create(TraceId.fromLowerBase16(TRACE_ID), SpanId.fromLowerBase16(SPAN_ID), TRACE_OPTIONS, TRACESTATE_DEFAULT);

            Map<String, String> result = ContextPropagationUtil.buildPropagationHeaderMap(data.entrySet().stream(), spanContext);

            String header = "00-" + TRACE_ID + "-" + SPAN_ID + "-00";
            assertThat(result).contains(entry(TRACEPARENT, header));

            ContextPropagationUtil.setPropagationFormat(PropagationFormat.B3); // set back to default
        }

        @Test
        public void injectHeader_DatadogFormat() {
            ContextPropagationUtil.setPropagationFormat(PropagationFormat.DATADOG);

            Map<String, Object> data = Collections.emptyMap();
            SpanContext spanContext = SpanContext.create(TraceId.fromLowerBase16(TRACE_ID), SpanId.fromLowerBase16(SPAN_ID), TRACE_OPTIONS, TRACESTATE_DEFAULT);

            Map<String, String> result = ContextPropagationUtil.buildPropagationHeaderMap(data.entrySet().stream(), spanContext);

            assertThat(result).contains(entry(X_DATADOG_TRACE_ID, TRACE_ID_DATADOG));
            assertThat(result).contains(entry(X_DATADOG_PARENT_ID, SPAN_ID_DATADOG));

            ContextPropagationUtil.setPropagationFormat(PropagationFormat.B3); // set back to default
        }
    }

    @Nested
    public class GetB3HeadersAsString {

        @Test
        public void nullMap() {
            String result = ContextPropagationUtil.getB3HeadersAsString(null);

            assertThat(result).isEqualTo("[]");
        }

        @Test
        public void emptyMap() {
            Map<String, String> data = Collections.emptyMap();

            String result = ContextPropagationUtil.getB3HeadersAsString(data);

            assertThat(result).isEqualTo("[]");
        }

        @Test
        public void mapWithoutB3() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one");

            String result = ContextPropagationUtil.getB3HeadersAsString(data);

            assertThat(result).isEqualTo("[]");
        }

        @Test
        public void singleB3Header() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", "traceId");

            String result = ContextPropagationUtil.getB3HeadersAsString(data);

            assertThat(result).isEqualTo("[\"X-B3-TraceId\": \"traceId\"]");
        }

        @Test
        public void multipleB3Header() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", "traceId", "X-B3-SpanId", "spanId");

            String result = ContextPropagationUtil.getB3HeadersAsString(data);

            assertThat(result).isEqualTo("[\"X-B3-TraceId\": \"traceId\", \"X-B3-SpanId\": \"spanId\"]");
        }
    }

    @Nested
    public class ReadPropagatedSpanContextFromHeaderMap {

        @Test
        public void readB3Header_sampled() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", TRACE_ID, "X-B3-SpanId", SPAN_ID, "X-B3-Sampled", "1");

            SpanContext spanContext = ContextPropagationUtil.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId().toLowerBase16()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId().toLowerBase16()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceOptions().isSampled()).isTrue();
        }

        @Test
        public void readB3Header_notSampled_explicit() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", TRACE_ID, "X-B3-SpanId", SPAN_ID, "X-B3-Sampled", "0");

            SpanContext spanContext = ContextPropagationUtil.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId().toLowerBase16()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId().toLowerBase16()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceOptions().isSampled()).isFalse();
        }

        @Test
        public void readB3Header_notSampled_implicit() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", TRACE_ID, "X-B3-SpanId", SPAN_ID);

            SpanContext spanContext = ContextPropagationUtil.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId().toLowerBase16()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId().toLowerBase16()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceOptions().isSampled()).isFalse();
        }

        @Test
        public void readTraceContextHeader_sampled() {
            String header = "00-" + TRACE_ID + "-" + SPAN_ID + "-01";
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "traceparent", header);

            SpanContext spanContext = ContextPropagationUtil.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId().toLowerBase16()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId().toLowerBase16()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceOptions().isSampled()).isTrue();
        }

        @Test
        public void readTraceContextHeader_notSampled() {
            String header = "00-" + TRACE_ID + "-" + SPAN_ID + "-00";
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "traceparent", header);

            SpanContext spanContext = ContextPropagationUtil.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId().toLowerBase16()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId().toLowerBase16()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceOptions().isSampled()).isFalse();
        }

        @Test
        public void readDatadogHeader_sampled() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-Datadog-Trace-ID", TRACE_ID_DATADOG, "X-Datadog-Parent-ID", SPAN_ID_DATADOG, "X-Datadog-Sampling-Priority", "1");

            SpanContext spanContext = ContextPropagationUtil.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId().toLowerBase16()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId().toLowerBase16()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceOptions().isSampled()).isTrue();
        }

        @Test
        public void readDatadogHeader_notSampled_explicit() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-Datadog-Trace-ID", TRACE_ID_DATADOG, "X-Datadog-Parent-ID", SPAN_ID_DATADOG, "X-Datadog-Sampling-Priority", "0");

            SpanContext spanContext = ContextPropagationUtil.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId().toLowerBase16()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId().toLowerBase16()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceOptions().isSampled()).isFalse();
        }

        @Test
        public void readDatadogHeader_notSampled_implicit() {
            Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-Datadog-Trace-ID", TRACE_ID_DATADOG, "X-Datadog-Parent-ID", SPAN_ID_DATADOG);

            SpanContext spanContext = ContextPropagationUtil.readPropagatedSpanContextFromHeaderMap(data);

            assertThat(spanContext.getTraceId().toLowerBase16()).isEqualTo(TRACE_ID);
            assertThat(spanContext.getSpanId().toLowerBase16()).isEqualTo(SPAN_ID);
            assertThat(spanContext.getTraceOptions().isSampled()).isFalse();
        }
    }
}
