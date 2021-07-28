package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

/**
 * Constants for the completer.
 *
 */
class Constants {

    static final String SCOPES = "scopes";

    static final String INSPECTIT = "inspectit";

    static final String INSTRUMENTATION = "instrumentation";

    static final String METRIC_S = "metrics"; // plural

    static final String METRIC = "metric"; // single

    static final String ACTION = "action"; // single

    static final String ACTION_S = "actions"; //plural

    static final String RULES = "rules";

    static final String STAR = "*";

    /**
     * No instance.
     */
    private Constants() {
        throw new IllegalStateException("No instance");
    }

}
