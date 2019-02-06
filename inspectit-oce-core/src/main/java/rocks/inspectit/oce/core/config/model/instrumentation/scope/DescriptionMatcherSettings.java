package rocks.inspectit.oce.core.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DescriptionMatcherSettings extends NameMatcherSettings {

    /**
     * Matcher which have to match the used annotations.
     */
    private NameMatcherSettings annotation;

    @Override
    public boolean isAnyMatcher() {
        return super.isAnyMatcher() && annotation.isAnyMatcher();
    }
}
