package inspectit.ocelot.configdocsgenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings;

/**
 * Enum with possible method names in {@link InstrumentationRuleSettings} for entry, exit, etc. ActionCall settings.
 */
@Getter
@RequiredArgsConstructor
public enum RuleLifecycleState {

    PRE_ENTRY("getPreEntry", "preEntry"),
    ENTRY("getEntry", "entry"),
    POST_ENTRY("getPostEntry", "postEntry"),
    PRE_EXIT("getPreExit", "preExit"),
    EXIT("getExit", "exit"),
    POST_EXIT("getPostExit", "postExit"),;

    private final String methodName;

    private final String propertyName;
}
