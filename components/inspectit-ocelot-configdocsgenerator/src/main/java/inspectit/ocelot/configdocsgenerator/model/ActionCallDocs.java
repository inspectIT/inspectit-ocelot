package inspectit.ocelot.configdocsgenerator.model;

import lombok.Getter;

/**
 * Data container for documentation of a single ActionCall's
 * {@link rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings} in Config Documentation.
 */
@Getter
public class ActionCallDocs {

    /**
     * The ActionCall's name.
     */
    private final String name;

    /**
     * The name of the action the ActionCall executes.
     */
    private final String actionName;

    /**
     * If the ActionCall comes from an included Rule, inheritedFrom is the name of that parent-Rule.
     */
    private String inheritedFrom;

    public ActionCallDocs(String name, String actionName) {
        this.name = name;
        this.actionName = actionName;
    }

    /**
     * A copying constructor that is used to add ActionCalls from other rules to a rule.
     *
     * @param fromIncludedRule The ActionCall from the other rule that should be copied.
     * @param inheritedFrom    The name of the other rule the ActionCall comes from.
     */
    public ActionCallDocs(ActionCallDocs fromIncludedRule, String inheritedFrom) {
        name = fromIncludedRule.getName();
        actionName = fromIncludedRule.getActionName();
        this.inheritedFrom = inheritedFrom;
    }

}
