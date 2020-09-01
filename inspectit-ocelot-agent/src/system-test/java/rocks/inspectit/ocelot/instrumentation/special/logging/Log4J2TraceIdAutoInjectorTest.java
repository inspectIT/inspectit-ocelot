package rocks.inspectit.ocelot.instrumentation.special.logging;

import io.opencensus.common.Scope;
import io.opencensus.trace.Tracing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.AbstractMessageFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.logging.Log4J2LoggingRecorder;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class Log4J2TraceIdAutoInjectorTest extends InstrumentationSysTestBase {

    private static final Logger LOGGER = LogManager.getLogger(Log4J2TraceIdAutoInjectorTest.class);

    @BeforeAll
    public static void waitForInstrumentation() {
        TestUtils.waitForClassInstrumentation(AbstractMessageFactory.class, false, 15, TimeUnit.SECONDS);
    }

    @Test
    public void logStringAndTraceExists() {
        Log4J2LoggingRecorder.loggingEvents.clear();
        String message = "This is a traced String in {}.";
        String traceId;

        try (Scope scope = Tracing.getTracer().spanBuilder("test").startScopedSpan()) {
            traceId = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();

            LOGGER.info(message, "Log4J2");
        }

        assertThat(Log4J2LoggingRecorder.loggingEvents)
                .hasSize(1);

        assertThat(Log4J2LoggingRecorder.loggingEvents.get(0))
                .extracting(event -> event.getMessage().getFormattedMessage())
                .isEqualTo("[TraceID: " + traceId + "] This is a traced String in Log4J2.");
    }

    @Test
    public void logCharSequenceAndTraceExists() {
        Log4J2LoggingRecorder.loggingEvents.clear();
        CharSequence message = "This is a traced CharSequence in Log4J2.";
        String traceId;

        try (Scope scope = Tracing.getTracer().spanBuilder("test").startScopedSpan()) {
            traceId = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();

            LOGGER.info(message);
        }

        assertThat(Log4J2LoggingRecorder.loggingEvents)
                .hasSize(1);

        assertThat(Log4J2LoggingRecorder.loggingEvents.get(0))
                .extracting(event -> event.getMessage().getFormattedMessage())
                .isEqualTo("[TraceID: " + traceId + "] This is a traced CharSequence in Log4J2.");
    }

    @Test
    public void logObjectAndTraceExists() {
        Log4J2LoggingRecorder.loggingEvents.clear();
        Object message = "This is a traced Object in Log4J2.";
        String traceId;

        try (Scope scope = Tracing.getTracer().spanBuilder("test").startScopedSpan()) {
            traceId = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();

            LOGGER.info(message);
        }

        assertThat(Log4J2LoggingRecorder.loggingEvents)
                .hasSize(1);

        assertThat(Log4J2LoggingRecorder.loggingEvents.get(0))
                .extracting(event -> event.getMessage().getFormattedMessage())
                .isEqualTo("[TraceID: " + traceId + "] This is a traced Object in Log4J2.");
    }

    @Test
    public void logNullAndTraceExists() {
        Log4J2LoggingRecorder.loggingEvents.clear();
        Object message = null;
        String traceId;

        try (Scope scope = Tracing.getTracer().spanBuilder("test").startScopedSpan()) {
            traceId = Tracing.getTracer().getCurrentSpan().getContext().getTraceId().toLowerBase16();

            LOGGER.info(message);
        }

        assertThat(Log4J2LoggingRecorder.loggingEvents)
                .hasSize(1);

        assertThat(Log4J2LoggingRecorder.loggingEvents.get(0))
                .extracting(event -> event.getMessage().getFormattedMessage())
                .isEqualTo("[TraceID: " + traceId + "] null");
    }

    @Test
    public void traceNotExists() {
        Log4J2LoggingRecorder.loggingEvents.clear();
        String message = "This is {}.";

        LOGGER.info(message, "Log4J2");

        assertThat(Log4J2LoggingRecorder.loggingEvents)
                .hasSize(1);

        assertThat(Log4J2LoggingRecorder.loggingEvents.get(0))
                .extracting(event -> event.getMessage().getFormattedMessage())
                .isEqualTo("This is Log4J2.");
    }
}
