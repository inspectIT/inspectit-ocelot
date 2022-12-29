package rocks.inspectit.ocelot.instrumentation.special.logging;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.AbstractMessageFactory;
import org.apache.logging.log4j.message.MessageFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.logging.Log4J2LoggingRecorder;
import rocks.inspectit.ocelot.utils.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class Log4J2TraceIdAutoInjectorTest extends InstrumentationSysTestBase {

    private static final Logger LOGGER = LogManager.getLogger(Log4J2TraceIdAutoInjectorTest.class);

    private static Tracer tracer;

    @BeforeAll
    public static void waitForInstrumentation() {
        TestUtils.waitForClassInstrumentations(AbstractMessageFactory.class, MessageFactory.class);
        tracer = GlobalOpenTelemetry.getTracer("rocks.inspectit.ocelot.test", "0.0.1");
    }

    @Test
    public void logStringAndTraceExists() {
        Log4J2LoggingRecorder.loggingEvents.clear();
        String message = "This is a traced String in {}.";
        String traceId;

        Span span = tracer.spanBuilder("test").startSpan();
        try (Scope scope = span.makeCurrent()) {
            traceId = Span.current().getSpanContext().getTraceId();
            LOGGER.info(message, "Log4J2");
        } finally {
            span.end();
        }

        assertThat(Log4J2LoggingRecorder.loggingEvents).hasSize(1);

        assertThat(Log4J2LoggingRecorder.loggingEvents.get(0)).extracting(event -> event.getMessage()
                .getFormattedMessage()).isEqualTo("[TraceID: " + traceId + "] This is a traced String in Log4J2.");
    }

    @Test
    public void logCharSequenceAndTraceExists() {
        Log4J2LoggingRecorder.loggingEvents.clear();
        CharSequence message = "This is a traced CharSequence in Log4J2.";
        String traceId;
        Span span = tracer.spanBuilder("test").startSpan();
        try (Scope scope = span.makeCurrent()) {
            traceId = Span.current().getSpanContext().getTraceId();
            LOGGER.info(message);
        } finally {
            span.end();
        }

        assertThat(Log4J2LoggingRecorder.loggingEvents).hasSize(1);

        assertThat(Log4J2LoggingRecorder.loggingEvents.get(0)).extracting(event -> event.getMessage()
                        .getFormattedMessage())
                .isEqualTo("[TraceID: " + traceId + "] This is a traced CharSequence in Log4J2.");
    }

    @Test
    public void logObjectAndTraceExists() {
        Log4J2LoggingRecorder.loggingEvents.clear();
        Object message = "This is a traced Object in Log4J2.";
        String traceId;

        Span span = tracer.spanBuilder("test").startSpan();
        try (Scope scope = span.makeCurrent()) {
            traceId = Span.current().getSpanContext().getTraceId();
            LOGGER.info(message);
        } finally {
            span.end();
        }

        assertThat(Log4J2LoggingRecorder.loggingEvents).hasSize(1);

        assertThat(Log4J2LoggingRecorder.loggingEvents.get(0)).extracting(event -> event.getMessage()
                .getFormattedMessage()).isEqualTo("[TraceID: " + traceId + "] This is a traced Object in Log4J2.");
    }

    @Test
    public void logNullAndTraceExists() {
        Log4J2LoggingRecorder.loggingEvents.clear();
        Object message = null;
        String traceId;

        Span span = tracer.spanBuilder("test").startSpan();
        try (Scope scope = span.makeCurrent()) {
            traceId = Span.current().getSpanContext().getTraceId();
            LOGGER.info(message);
        } finally {
            span.end();
        }

        assertThat(Log4J2LoggingRecorder.loggingEvents).hasSize(1);

        assertThat(Log4J2LoggingRecorder.loggingEvents.get(0)).extracting(event -> event.getMessage()
                .getFormattedMessage()).isEqualTo("[TraceID: " + traceId + "] null");
    }

    @Test
    public void traceNotExists() {
        Log4J2LoggingRecorder.loggingEvents.clear();
        String message = "This is {}.";

        LOGGER.info(message, "Log4J2");

        assertThat(Log4J2LoggingRecorder.loggingEvents).hasSize(1);

        assertThat(Log4J2LoggingRecorder.loggingEvents.get(0)).extracting(event -> event.getMessage()
                .getFormattedMessage()).isEqualTo("This is Log4J2.");
    }
}
