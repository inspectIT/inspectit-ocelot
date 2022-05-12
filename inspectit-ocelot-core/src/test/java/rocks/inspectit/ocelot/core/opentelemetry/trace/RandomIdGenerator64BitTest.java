package rocks.inspectit.ocelot.core.opentelemetry.trace;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class RandomIdGenerator64BitTest {
    int getBitSize (String s) {
        return s.getBytes(StandardCharsets.UTF_8).length * 8;
    }

    @Nested
    class InvalidStringId {
        @Test
        public void is64Bit() {
            String id = RandomIdGenerator64Bit.INVALID_STRING_ID;
            assertThat(getBitSize(id)).isEqualTo(64);
        }

    }

    @Nested
    class TestGenerateSpanId {

        @Test
        public void is64Bit() {
            String id = RandomIdGenerator64Bit.INSTANCE.generateSpanId();
            assertThat(getBitSize(id)).isEqualTo(64);
        }
    }

    @Nested
    class TestGenerateTraceId {

        @Test
        public void is64Bit() {
            String id = RandomIdGenerator64Bit.INSTANCE.generateTraceId();
            assertThat(getBitSize(id)).isEqualTo(64);
        }
    }

}