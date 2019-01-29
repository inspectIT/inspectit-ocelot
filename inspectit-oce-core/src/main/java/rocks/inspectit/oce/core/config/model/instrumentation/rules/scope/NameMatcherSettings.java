package rocks.inspectit.oce.core.config.model.instrumentation.rules.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.bytebuddy.matcher.StringMatcher;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class NameMatcherSettings {

    private String namePattern;

    private StringMatcher.Mode matcherMode = StringMatcher.Mode.EQUALS_FULLY;

}
