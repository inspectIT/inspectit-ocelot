package rocks.inspectit.oce.core.config.model.instrumentation.rules.scope;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TypeScope {

    private List<NameMatcherSettings> interfaces;

    private NameMatcherSettings superclass;

    private List<NameMatcherSettings> classes;

}
