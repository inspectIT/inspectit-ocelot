package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.Value;

import java.util.Set;

@Value
public class InstrumentationRule {

    private String name;

    private Set<InstrumentationScope> scopes;
}
