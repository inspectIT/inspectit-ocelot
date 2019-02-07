package rocks.inspectit.oce.core.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * Extending the {@link NameMatcherSettings} by adding the ability to specify a list of {@link NameMatcherSettings} which
 * represents annotations.
 */
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
                && (CollectionUtils.isEmpty(annotations) || annotations.stream().anyMatch(NameMatcherSettings::isAnyMatcher));
    }
}
