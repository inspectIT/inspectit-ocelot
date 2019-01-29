package rocks.inspectit.oce.core.config.model.instrumentation.rules.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class InstrumentationScopeSettings {

    private TypeScope typeScope;

    private List<MethodMatcherSettings> methodScope;

    private AdvancedScopeSettings advanced;

}

