package rocks.inspectit.ocelot.core.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.PrometheusExporterSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Base class for services which can be dynamically enabled and disabled based on the {@link InspectitConfig}.
 * This class handles the waiting for changes in the configuration.
 * If relevant changes to the configuration occur, this class ensures that the service is properly restarted.
 */
public abstract class DynamicallyActivatableService {

    @Autowired
    protected InspectitEnvironment env;

    private List<Expression> configDependencies;

    /**
     * True if the service is currently enabled.
     */
    @Getter
    private boolean enabled = false;

    /**
     * Constructor.
     *
     * @param configDependencies The list of configuration properties in camelCase this service depends on.
     *                           For example "exporters.metrics.prometheus" specifies a dependency
     *                           to {@link PrometheusExporterSettings}
     *                           and all its children.
     */
    public DynamicallyActivatableService(String... configDependencies) {
        ExpressionParser parser = new SpelExpressionParser();
        this.configDependencies = Arrays.stream(configDependencies)
                .map(parser::parseExpression)
                .collect(Collectors.toList());
    }

    @PostConstruct
    void initialize() {
        init();
    }

    @EventListener(ContextRefreshedEvent.class)
    void startIfEnabled() {
        if (checkEnabledForConfig(env.getCurrentConfig())) {
            enabled = enable();
        } else {
            enabled = false;
        }
    }

    /**
     * Initialization, guaranteed to be called exactly once prior to the first invocation of doEnable() and checkEnabledForConfig();
     */
    protected void init() {
    }

    /**
     * Tries to enable the service if it is not already enabled.
     * Usually should not be called directly. Instead it is called automatically when configuration changes occur.
     *
     * @return true, if the service is now running
     */
    synchronized boolean enable() {
        if (!enabled) {
            enabled = doEnable(env.getCurrentConfig());
        }
        return enabled;
    }

    /**
     * Tries to disable the service if it is currently enabled.
     * Usually should not be called directly. Instead it is called automatically when configuration changes occur.
     *
     * @return true, if the service is now stopped
     */
    @PreDestroy
    synchronized boolean disable() {
        if (enabled) {
            enabled = !doDisable();
        }
        return !enabled;
    }

    @EventListener(InspectitConfigChangedEvent.class)
    synchronized void checkForUpdates(InspectitConfigChangedEvent ev) {
        boolean affected = false;
        for (Expression exp : configDependencies) {
            Object oldVal = exp.getValue(ev.getOldConfig());
            Object newVal = exp.getValue(ev.getNewConfig());
            boolean isEqual = Objects.equals(oldVal, newVal);
            if (!isEqual) {
                affected = true;
            }
        }
        if (affected) {
            if (enabled) {
                disable();
                if (enabled) {
                    return; // could not disable the service and therefore cannot reenable it
                }
            }
            if (checkEnabledForConfig(ev.getNewConfig())) {
                enable();
            }
        }
    }

    /**
     * The implementation of this method checks if the service should be enabled given a certain configuration.
     * When changes to the configuration occur, this method will be used to correctly invoke {@link #doDisable()} and {@link #doEnable()}.
     *
     * @param configuration the configuration to check
     *
     * @return true if the service should be enabled otherwise false
     */
    protected abstract boolean checkEnabledForConfig(InspectitConfig configuration);

    /**
     * Called when the service should start.
     * This performs the actual enabling logic.
     * If the enabling is not successful, this method has to perform the cleanup.
     *
     * @param configuration the configuration used to start the service. Is the same configuration as {@link InspectitEnvironment#getCurrentConfig()}.
     *
     * @return true if the enabling was successful, false otherwise.
     */
    protected abstract boolean doEnable(InspectitConfig configuration);

    /**
     * Called when the service is running and should be stopped.
     * This is guaranteed to be only called when previously {@link #doEnable(InspectitConfig)} was called and was successful.
     *
     * @return true, if the disabling was successful. false if the service could not be disabled and is still running.
     */
    protected abstract boolean doDisable();

}
