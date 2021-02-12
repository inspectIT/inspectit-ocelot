package rocks.inspectit.ocelot.rest.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.DurationStyle;

import java.time.Duration;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class DurationUtilTest {

    @Nested
    class PrettyPrintDuration {

        @Test
        void zeroDuration() {
            Duration dur = Duration.ofSeconds(0);

            String printed = DurationUtil.prettyPrintDuration(dur);

            assertThat(printed).isEqualTo("0s");
            assertThat(DurationStyle.SIMPLE.parse(printed)).isEqualTo(dur);
        }

        @Test
        void nanosOnly() {
            Duration dur = Duration.ofNanos(123456789);

            String printed = DurationUtil.prettyPrintDuration(dur);

            assertThat(printed).isEqualTo("123456789ns");
            assertThat(DurationStyle.SIMPLE.parse(printed)).isEqualTo(dur);
        }

        @Test
        void millisOnly() {
            Duration dur = Duration.ofMillis(123456);

            String printed = DurationUtil.prettyPrintDuration(dur);

            assertThat(printed).isEqualTo("123456ms");
            assertThat(DurationStyle.SIMPLE.parse(printed)).isEqualTo(dur);
        }

        @Test
        void secondsOnly() {
            Duration dur = Duration.ofSeconds(123456);

            String printed = DurationUtil.prettyPrintDuration(dur);

            assertThat(printed).isEqualTo("123456s");
            assertThat(DurationStyle.SIMPLE.parse(printed)).isEqualTo(dur);
        }

        @Test
        void minutesOnly() {
            Duration dur = Duration.ofMinutes(123456);

            String printed = DurationUtil.prettyPrintDuration(dur);

            assertThat(printed).isEqualTo("123456m");
            assertThat(DurationStyle.SIMPLE.parse(printed)).isEqualTo(dur);
        }

        @Test
        void hoursOnly() {
            Duration dur = Duration.ofHours(12345);

            String printed = DurationUtil.prettyPrintDuration(dur);

            assertThat(printed).isEqualTo("12345h");
            assertThat(DurationStyle.SIMPLE.parse(printed)).isEqualTo(dur);
        }

        @Test
        void daysOnly() {
            Duration dur = Duration.ofDays(1234);

            String printed = DurationUtil.prettyPrintDuration(dur);

            assertThat(printed).isEqualTo("1234d");
            assertThat(DurationStyle.SIMPLE.parse(printed)).isEqualTo(dur);
        }

    }

}
