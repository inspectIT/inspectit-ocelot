package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * TODO: description
 * TODO: make sure logs are only collected when/as configured using a filter (see {@link rocks.inspectit.ocelot.core.logging.logback.LogbackInitializer.SelfMonitoringFilter}.
 */
public class LogPreloadingAppender extends AppenderBase<ILoggingEvent> {

    private static LogPreloader preloader;

    @Override
    protected void append(ILoggingEvent logEvent) {
        if (preloader != null) {
            preloader.record(logEvent);
        }
    }

    /**
     * Registers {@link LogPreloader} to push all log events to the preloader.
     */
    public static void registerPreloader(LogPreloader preloader) {
        LogPreloadingAppender.preloader = preloader;
    }

}