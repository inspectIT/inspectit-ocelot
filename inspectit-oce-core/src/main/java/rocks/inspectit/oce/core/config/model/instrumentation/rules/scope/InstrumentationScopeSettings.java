package rocks.inspectit.oce.core.config.model.instrumentation.rules.scope;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class InstrumentationScopeSettings {

    private TypeScope typeScope;

    private List<MethodMatcherSettings> methodScope;

    private AdvancedScopeSettings advanced;

}

