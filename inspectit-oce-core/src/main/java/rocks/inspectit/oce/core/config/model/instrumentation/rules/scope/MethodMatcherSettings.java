package rocks.inspectit.oce.core.config.model.instrumentation.rules.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MethodMatcherSettings extends NameMatcherSettings {

    private Boolean isConstructor = false;

    private Boolean isSynchronized;

    private String[] arguments;

    private AccessModifier[] visibility;
}
