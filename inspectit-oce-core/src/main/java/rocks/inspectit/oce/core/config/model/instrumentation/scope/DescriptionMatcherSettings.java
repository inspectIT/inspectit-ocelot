package rocks.inspectit.oce.core.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DescriptionMatcherSettings extends NameMatcherSettings {

    /**
     * Matcher which have to match the used annotations.
     */
    @NotNull
    private List<NameMatcherSettings> annotations = Collections.emptyList();

    @Override
    public boolean isAnyMatcher() {
        return super.isAnyMatcher()
                && annotations != null
                && annotations.stream().anyMatch(NameMatcherSettings::isAnyMatcher);
    }
}
