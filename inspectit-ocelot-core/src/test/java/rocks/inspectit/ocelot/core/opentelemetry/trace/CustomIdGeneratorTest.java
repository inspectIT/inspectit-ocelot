package rocks.inspectit.ocelot.core.opentelemetry.trace;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CustomIdGeneratorTest {

    int getBitSize(String string) {
        return string.getBytes(StandardCharsets.UTF_8).length * 8;
    }

    @InjectMocks
    CustomIdGenerator idGenerator;

    @Nested
    class TestGenerateSpanId {

        @Test
        public void is64Bit() {
            String id = idGenerator.generateSpanId();
            assertThat(getBitSize(id)).isEqualTo(128);
        }
    }

    @Nested
    class TestGenerateTraceId {

        @Test
        public void is64Bit() {
            idGenerator.setMode(true);

            String id = idGenerator.generateTraceId();

            assertThat(id.substring(0,16)).isEqualTo("0000000000000000");
            assertThat(getBitSize(id)).isEqualTo(256);
        }


        @Test
        public void is128Bit() {
            idGenerator.setMode(false);

            String id = idGenerator.generateTraceId();

            assertThat(id.substring(0,16)).isNotEqualTo("0000000000000000");
            assertThat(getBitSize(id)).isEqualTo(256);
        }

        @Test
        public void defaultMode() {
            String id = idGenerator.generateTraceId();

            assertThat(id.substring(0,16)).isNotEqualTo("0000000000000000");
            assertThat(getBitSize(id)).isEqualTo(256);
        }
    }

}