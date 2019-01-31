package rocks.inspectit.oce.core.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.bytebuddy.matcher.StringMatcher;

/**
 * Data container which is used as basis for name matcher used in the {@link rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope}.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class NameMatcherSettings {

    /**
     * The pattern of the class or method name.
     */
    private String namePattern;

    /**
     * The matcher mode.
     */
    private StringMatcher.Mode matcherMode = StringMatcher.Mode.EQUALS_FULLY;

}
