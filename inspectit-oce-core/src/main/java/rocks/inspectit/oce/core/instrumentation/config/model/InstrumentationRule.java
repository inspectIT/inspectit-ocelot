package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.*;

import java.util.Map;
import java.util.Set;

/**
 * This class represents an instrumentation rule defining a bunch of classes to instrument and to inject the dispatcher
 * hook which is used for generic data collection.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Builder(toBuilder = true)
public class InstrumentationRule {

    /**
     * The rule's name.
     */
    private String name;

    /**
     * The scope of this rule. This represents a matcher of types and methods that should be instrumented.
     */
    @Singular
    private Set<InstrumentationScope> scopes;

    /**
     * Maps data keys to the data provider call defining the value.
     * These assignments are performed in the method-entry part.
     */
    @Singular
    private Map<String, DataProviderCallConfig> entryProviders;

    /**
     * Maps data keys to the data provider call defining the value.
     * These assignments are performed in the method-exit part.
     */
    @Singular
    private Map<String, DataProviderCallConfig> exitProviders;
}
