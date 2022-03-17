package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.logging.logback.InternalProcessingAppender;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Logback recorder which exposes the counts to the {@link SelfMonitoringService}
 */
@Component
public class LogMetricsRecorder implements InternalProcessingAppender.Observer {

    @Autowired
    private SelfMonitoringService selfMonitoringService;

    @Override
    public void onInstrumentationLoggingEvent(ILoggingEvent event) {
        recordLogEvent(event);
    }

    @Override
    public void onGeneralLoggingEvent(ILoggingEvent event) {
        recordLogEvent(event);
    }

    /**
     * Records an increment of one of the number of metrics.
     *
     * @param event The log event, which defines the logLevel (e.g. WARN or ERROR).
     */
    private void recordLogEvent(ILoggingEvent event) {
        Map<String, String> customTags = new HashMap<>();
        customTags.put("level", event.getLevel().toString());
        selfMonitoringService.recordMeasurement("logs", 1, customTags);
    }

    @PostConstruct
    private void subscribe() {
        InternalProcessingAppender.register(this);
    }
}