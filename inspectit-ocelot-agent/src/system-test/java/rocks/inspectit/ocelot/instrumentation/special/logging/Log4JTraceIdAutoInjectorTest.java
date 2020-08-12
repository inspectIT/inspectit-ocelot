package rocks.inspectit.ocelot.instrumentation.special.logging;

import io.opencensus.common.Scope;
import io.opencensus.trace.Tracing;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.logging.Log4JLoggingRecorder;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class Log4JTraceIdAutoInjectorTest extends InstrumentationSysTestBase {

    private static final Logger LOGGER = Logger.getLogger(Log4JTraceIdAutoInjectorTest.class.getName());

    @BeforeAll
    public static void waitForInstrumentation() {
        TestUtils.waitForClassInstrumentation(Category.class, false, 15, TimeUnit.SECONDS);
    }

    @Test
    public void traceExists() {
        Log4JLoggingRecorder.loggingEvents.clear();
        String message = "This is a test.";
        String traceId;

        try (Scope scope = Tracing.getTracer().spanBuilder("test").startScopedSpan()) {
            traceId = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();

            LOGGER.error(message);
        }

        assertThat(Log4JLoggingRecorder.loggingEvents)
                .hasSize(1);

        assertThat(Log4JLoggingRecorder.loggingEvents.get(0))
                .extracting(LoggingEvent::getMessage)
                .isEqualTo("[TraceID: " + traceId + "] " + message);
    }

    @Test
    public void traceNotExists() {
        Log4JLoggingRecorder.loggingEvents.clear();
        String message = "This is a test.";

        LOGGER.error(message);

        assertThat(Log4JLoggingRecorder.loggingEvents)
                .hasSize(1);

        assertThat(Log4JLoggingRecorder.loggingEvents.get(0))
                .extracting(LoggingEvent::getMessage)
                .isEqualTo(message);
    }
}
