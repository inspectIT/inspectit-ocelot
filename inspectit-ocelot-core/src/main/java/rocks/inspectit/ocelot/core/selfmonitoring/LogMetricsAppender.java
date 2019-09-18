package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Logback appender which counts the amount of log events for all existing log levels.
 */
public class LogMetricsAppender extends AppenderBase<ILoggingEvent> {

    /**
     * Map that holds the number of log events per log level.
     */
    private static Map<String, Long> logCounts = new HashMap();

    /**
     * Recorder, which exposes the number of log events to OpenCensus
     */
    private static LogMetricsRecorder logMetricsRecorder;


    @Override
    protected void append(ILoggingEvent logEvent) {
        if (logMetricsRecorder == null) {
            logCounts.merge(logEvent.getLevel().toString(), 1L, Long::sum);
        } else {
            logMetricsRecorder.increment(logEvent.getLevel().toString(), 1);
        }
    }

    /**
     * Registers {@link LogMetricsRecorder} and pushes all buffered counts to the recorder.
     */
    public static void registerRecorder(LogMetricsRecorder logMetricsRecorder) {
        LogMetricsAppender.logMetricsRecorder = logMetricsRecorder;
        logCounts.entrySet().stream().forEach(entry -> logMetricsRecorder.increment(entry.getKey(), entry.getValue()));
        logCounts.clear();
    }
}