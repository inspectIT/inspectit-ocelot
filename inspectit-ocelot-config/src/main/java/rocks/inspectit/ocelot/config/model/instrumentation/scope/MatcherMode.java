package rocks.inspectit.ocelot.config.model.instrumentation.scope;

/**
 * MatcherModes corresponding to ByteBuddy's matcher modes.
 */
public enum MatcherMode {

    EQUALS_FULLY,
    EQUALS_FULLY_IGNORE_CASE,
    STARTS_WITH,
    STARTS_WITH_IGNORE_CASE,
    ENDS_WITH,
    ENDS_WITH_IGNORE_CASE,
    CONTAINS,
    CONTAINS_IGNORE_CASE,
    MATCHES,
    NOT_EQUALS_FULLY,
    NOT_EQUALS_FULLY_IGNORE_CASE
}
