package rocks.inspectit.oce.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.logging.logback.LogbackInitializer;

import java.util.Objects;

/**
 * Listener for the logging configuration changes.
 */
@Component
@Slf4j
public class LoggingConfigurationChangeListener {

    /**
     * If {@link rocks.inspectit.oce.core.config.model.logging.LoggingSettings} or service-name config has changed, re-initializes the logback configuration.
     *
     * @param event InspectitConfigChangedEvent
     */
    @EventListener(InspectitConfigChangedEvent.class)
    public void on(InspectitConfigChangedEvent event) {
        // get configs
        InspectitConfig newConfig = event.getNewConfig();
        InspectitConfig oldConfig = event.getOldConfig();

        // only react if logging configuration has changed or our
        if (Objects.equals(newConfig.getLogging(), oldConfig.getLogging()) && Objects.equals(newConfig.getServiceName(), oldConfig.getServiceName())) {
            return;
        }

        // simply call initLogging again with new config
        LogbackInitializer.initLogging(newConfig);
        if (log.isDebugEnabled()) {
            log.debug("Logback configuration updated.");
        }
    }

}
