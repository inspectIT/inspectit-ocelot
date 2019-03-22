package rocks.inspectit.ocelot.core.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.bytebuddy.matcher.StringMatcher;
import org.apache.commons.lang3.StringUtils;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope;

import javax.validation.constraints.NotNull;

/**
 * Data container which is used as basis for name matcher used in the {@link InstrumentationScope}.
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
    private StringMatcher.Mode matcherMode = StringMatcher.Mode.EQUALS_FULLY;

    /**
     * Returns whether this matcher will match basically anything.
     *
     * @return Returns true if it will match basically anything.
     */
    public boolean isAnyMatcher() {
        return (matcherMode == StringMatcher.Mode.MATCHES && StringUtils.equals(name, ".*"))
                || (StringUtils.isEmpty(name) && (
                matcherMode == StringMatcher.Mode.STARTS_WITH
                        || matcherMode == StringMatcher.Mode.STARTS_WITH_IGNORE_CASE
                        || matcherMode == StringMatcher.Mode.CONTAINS
                        || matcherMode == StringMatcher.Mode.CONTAINS_IGNORE_CASE
                        || matcherMode == StringMatcher.Mode.ENDS_WITH
                        || matcherMode == StringMatcher.Mode.ENDS_WITH_IGNORE_CASE
        )
        );
    }
}
