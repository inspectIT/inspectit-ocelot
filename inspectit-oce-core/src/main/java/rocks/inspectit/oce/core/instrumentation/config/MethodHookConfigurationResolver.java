package rocks.inspectit.oce.core.instrumentation.config;

import net.bytebuddy.description.method.MethodDescription;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration;

import java.util.Set;

@Component
public class MethodHookConfigurationResolver {

    /**
     * Derives the configuration of the hook for the given method.
     *
     * @param method       The method to derive the hook for
     * @param matchedRules All enabled rules which have a scope which matches to this method, must contain at least one value
     * @return
     */
    public MethodHookConfiguration buildHookConfiguration(MethodDescription method, Set<InstrumentationRule> matchedRules) {
        return MethodHookConfiguration.builder().build();
    }
}
