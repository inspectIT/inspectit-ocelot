package rocks.inspectit.oce.core.instrumentation.special;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatcher;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;

public interface SpecialSensor {

    /**
     * Specifies whether this {@link SpecialSensor} should be invoked given the configuration.
     *
     * @param conf the configuration to check for
     * @return true, if the {@link #instrument(InstrumentationSettings, AgentBuilder)} method should be invoked given the configuration
     */
    boolean isEnabledForConfig(InstrumentationSettings conf);

    /**
     * Performs the instrumentation of this special sensor.
     * !! Note that {@link AgentBuilder#ignore(ElementMatcher, ElementMatcher)} must not be called !!
     * Ignores are set up by the {@link rocks.inspectit.oce.core.instrumentation.InstrumentationManager}
     *
     * @param conf         the current configuration
     * @param agentBuilder the builder t oextend
     * @return
     */
    AgentBuilder instrument(InstrumentationSettings conf, AgentBuilder agentBuilder);
}
