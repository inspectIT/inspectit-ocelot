package rocks.inspectit.ocelot.core.selfmonitoring.logs;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.logging.logback.InternalProcessingAppender;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

/**
 * Logback recorder which exposes the counts to the {@link SelfMonitoringService}
 */
@Component
public class LogMetricsRecorder implements InternalProcessingAppender.LogEventConsumer {

    @Autowired
    private SelfMonitoringService selfMonitoringService;

    /**
     * Records an increment of one of the number of metrics.
     *
     * @param event       The log event, which defines the logLevel (e.g. WARN or ERROR).
     * @param invalidator Ignored.
     */
    @Override
    public void onLoggingEvent(ILoggingEvent event, Class<?> invalidator) {
        Map<String, String> customTags = new HashMap<>();
        customTags.put("level", event.getLevel().toString());
        selfMonitoringService.recordMeasurement("logs", 1, customTags);
    }

    @PostConstruct
    private void subscribe() {
        InternalProcessingAppender.register(this);
    }

    @PreDestroy
    private void unsubscribe() {
        InternalProcessingAppender.unregister(this);
    }
}