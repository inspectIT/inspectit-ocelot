package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class InstrumentationRule {

    private String name;

    private Set<InstrumentationScope> scopes;
}
