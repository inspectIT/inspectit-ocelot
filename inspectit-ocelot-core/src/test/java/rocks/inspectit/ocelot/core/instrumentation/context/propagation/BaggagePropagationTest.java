package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static rocks.inspectit.ocelot.core.instrumentation.context.propagation.BaggagePropagation.MAX_BAGGAGE_HEADER_SIZE;

public class BaggagePropagationTest {

    BaggagePropagation baggagePropagation = new BaggagePropagation();

    @Test
    void testBaggageDoesNotExceedMaxSize() {
        Map<String, Object> data = new HashMap<>();

        // Add entries until we definitely exceed 4096 bytes
        for (int i = 0; i < 1000; i++) {
            data.put("key" + i, "value" + i + "_1234567890,");
        }

        String baggage = baggagePropagation.buildBaggageHeader(data);

        assertThat(baggage.length()).isGreaterThan(0);
        assertThat(baggage.length()).isLessThanOrEqualTo(MAX_BAGGAGE_HEADER_SIZE);
    }
}
