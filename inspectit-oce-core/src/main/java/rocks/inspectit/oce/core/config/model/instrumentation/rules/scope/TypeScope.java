package rocks.inspectit.oce.core.config.model.instrumentation.rules.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class TypeScope {

    private List<NameMatcherSettings> interfaces;

    private NameMatcherSettings superclass;

    private List<NameMatcherSettings> classes;

}
