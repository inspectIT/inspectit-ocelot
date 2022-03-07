package inspectit.ocelot.configdocsgenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings;

/**
 * Enum with possible field names in {@link InstrumentationRuleSettings} for entry, exit, etc. ActionCall settings.
 */
@Getter
@RequiredArgsConstructor
public enum RuleLifecycleState {
    PRE_ENTRY("preEntry"), ENTRY("entry"), POST_ENTRY("postEntry"), PRE_EXIT("preExit"), EXIT("exit"), POST_EXIT("postExit");

    private final String key;
}
