package rocks.inspectit.oce.core.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data container which is used as basis for {@link rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope}.
 * Instances of this class will result in a matcher specifying which types a targeted by an instrumentation.
 * <p>
 * Note: the conjunction of all defined matchers will be used to for matching.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class TypeScope {

    /**
     * Interfaces which have to be implemented.
     */
    private List<NameMatcherSettings> interfaces;

    /**
     * Superclass which has to be extended.
     */
    private NameMatcherSettings superclass;

    /**
     * Matcher which have to match the class name.
     */
    private List<NameMatcherSettings> classes;

}
