package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Set;

/**
 * This class represents an instrumentation rule defining a bunch of classes to instrument and to inject the dispatcher
 * hook which is used for generic data collection.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class InstrumentationRule {

    /**
     * The rule's name.
     */
    private String name;

    /**
     * The scope of this rule. This represents a matcher of types and methods that should be instrumented.
     */
    private Set<InstrumentationScope> scopes;
}
