package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class B3FormatTest {

    @Test
    void nullMap() {
        String result = B3Format.INSTANCE.getB3HeadersAsString(null);

        assertThat(result).isEqualTo("[]");
    }

    @Test
    void emptyMap() {
        Map<String, String> data = Collections.emptyMap();

        String result = B3Format.INSTANCE.getB3HeadersAsString(data);

        assertThat(result).isEqualTo("[]");
    }

    @Test
    void mapWithoutB3() {
        Map<String, String> data = ImmutableMap.of("key-one", "value-one");

        String result = B3Format.INSTANCE.getB3HeadersAsString(data);

        assertThat(result).isEqualTo("[]");
    }

    @Test
    void singleB3Header() {
        Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", "traceId");

        String result = B3Format.INSTANCE.getB3HeadersAsString(data);

        assertThat(result).isEqualTo("[\"X-B3-TraceId\": \"traceId\"]");
    }

    @Test
    void multipleB3Header() {
        Map<String, String> data = ImmutableMap.of("key-one", "value-one", "X-B3-TraceId", "traceId", "X-B3-SpanId", "spanId");

        String result = B3Format.INSTANCE.getB3HeadersAsString(data);

        assertThat(result).isEqualTo("[\"X-B3-TraceId\": \"traceId\", \"X-B3-SpanId\": \"spanId\"]");
    }
}
