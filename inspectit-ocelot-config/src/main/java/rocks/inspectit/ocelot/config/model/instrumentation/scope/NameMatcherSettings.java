package rocks.inspectit.ocelot.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;

/**
 * Data container which is used as basis for name matcher used in the {@link rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope}.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class NameMatcherSettings {

    /**
     * The pattern of the class or method name.
     */
    private String name;

    /**
     * The matcher mode.
     */
    @NotNull
    private MatcherMode matcherMode = MatcherMode.EQUALS_FULLY;

    /**
     * Returns whether this matcher will match basically anything.
     *
     * @return Returns true if it will match basically anything.
     */
    public boolean isAnyMatcher() {
        return (matcherMode == MatcherMode.MATCHES && ".*".equals(name))
                || (StringUtils.isEmpty(name) && (
                matcherMode == MatcherMode.STARTS_WITH
                        || matcherMode == MatcherMode.STARTS_WITH_IGNORE_CASE
                        || matcherMode == MatcherMode.CONTAINS
                        || matcherMode == MatcherMode.CONTAINS_IGNORE_CASE
                        || matcherMode == MatcherMode.ENDS_WITH
                        || matcherMode == MatcherMode.ENDS_WITH_IGNORE_CASE
                        || matcherMode == MatcherMode.NOT_EQUALS_FULLY
                        || matcherMode == MatcherMode.NOT_EQUALS_FULLY_IGNORE_CASE
        )
        );
    }
}
