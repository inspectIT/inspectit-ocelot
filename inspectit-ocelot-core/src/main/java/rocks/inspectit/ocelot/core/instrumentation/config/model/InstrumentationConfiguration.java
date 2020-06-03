package rocks.inspectit.ocelot.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;
import rocks.inspectit.ocelot.config.model.events.EventSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contains the resolved instrumentation settings.
 * The difference between resolved and unresolved configurations (= {@link InstrumentationSettings}
 * is that preprocessing by the {@link InstrumentationConfigurationResolver} has occurred.
 * For example, in the resolved InstrumentationConfiguration profiles have been unrolled, so that a complete set of rules is available.
 */
@Value
@NonFinal //for testing
public class InstrumentationConfiguration {

    /**
     * Corresponds to {@link MetricsSettings#isEnabled()}
     */
    private boolean metricsEnabled;

    /**
     * Corresponds to {@link TracingSettings#isEnabled()}
     */
    private boolean tracingEnabled;

    /**
     * Corresponds to {@link EventSettings#isEnabled()}
     */
    private boolean eventsEnabled;

    /**
     * The instrumentation settings which have been used to derive this configuration.
     */
    private InstrumentationSettings source;

    private PropagationMetaData propagationMetaData;

    private TracingSettings tracingSettings;

    /**
     * The currently active instrumentation rules.
     */
    private Map<String, InstrumentationRule> rulesMap;

    /**
     * Constructor
     *
     * @param metricsEnabled      corresponds to {@link MetricsSettings#isEnabled()}, true if null (for testing)
     * @param tracingEnabled      corresponds to {@link TracingSettings#isEnabled()}, true if null (for testing)
     * @param eventsEnabled       corresponds to {@link EventSettings#isEnabled()}
     * @param source              the settings used for building this instrumentation configuration
     * @param propagationMetaData the propagation meta data
     * @param tracingSettings     the tracing settings
     * @param rules               the set of active rules
     */
    @Builder(toBuilder = true)
    public InstrumentationConfiguration(Boolean metricsEnabled,
                                        Boolean tracingEnabled,
                                        Boolean eventsEnabled,
                                        InstrumentationSettings source,
                                        PropagationMetaData propagationMetaData,
                                        TracingSettings tracingSettings,
                                        @Singular @Builder.ObtainVia(method = "getRules") Collection<InstrumentationRule> rules) {
        this.metricsEnabled = Optional.ofNullable(metricsEnabled).orElse(true);
        this.tracingEnabled = Optional.ofNullable(tracingEnabled).orElse(true);
        this.eventsEnabled = Optional.ofNullable(eventsEnabled).orElse(true);
        this.source = source;
        this.propagationMetaData = propagationMetaData;
        this.tracingSettings = tracingSettings;
        rulesMap = rules.stream().collect(Collectors.toMap(InstrumentationRule::getName, rule -> rule));
    }

    /**
     * Returns a collection of all active rules.
     *
     * @return the active rules
     */
    public Collection<InstrumentationRule> getRules() {
        return rulesMap.values();
    }

    /**
     * Returns the rule with the given name, if it exists and is enabled.
     *
     * @param name the name of the rule
     * @return an Optional containing the rule or an empty optional if it doesn't exist or is not active
     */
    public Optional<InstrumentationRule> getRuleByName(String name) {
        return Optional.ofNullable(rulesMap.get(name));
    }
}
