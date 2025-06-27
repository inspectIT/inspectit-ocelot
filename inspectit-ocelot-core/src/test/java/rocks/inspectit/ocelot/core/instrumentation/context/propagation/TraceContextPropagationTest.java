package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TraceContextPropagationTest {

    TraceContextPropagation contextPropagation = new TraceContextPropagation();

    @Test
    public void nullMap() {
        String result = contextPropagation.getB3HeadersAsString(null);

        assertThat(result).isEqualTo("[]");
    }

    @Test
    public void emptyMap() {
        Map<String, String> data = Collections.emptyMap();

        String result = contextPropagation.getB3HeadersAsString(data);

        assertThat(result).isEqualTo("[]");
    }

    @Test
    public void mapWithoutB3() {
        Map<String, String> data = ImmutableMap.of("key-one", "value-one");

        String result = contextPropagation.getB3HeadersAsString(data);

        assertThat(result).isEqualTo("[]");
    }

    @Test
    public void singleB3Header() {
        Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", "traceId");

        String result = contextPropagation.getB3HeadersAsString(data);

        assertThat(result).isEqualTo("[\"X-B3-TraceId\": \"traceId\"]");
    }

    @Test
    public void multipleB3Header() {
        Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", "traceId", "X-B3-SpanId", "spanId");

        String result = contextPropagation.getB3HeadersAsString(data);

        assertThat(result).isEqualTo("[\"X-B3-TraceId\": \"traceId\", \"X-B3-SpanId\": \"spanId\"]");
    }
}
