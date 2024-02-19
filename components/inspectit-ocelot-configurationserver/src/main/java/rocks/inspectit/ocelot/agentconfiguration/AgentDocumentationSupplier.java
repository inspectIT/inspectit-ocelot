package rocks.inspectit.ocelot.agentconfiguration;

import inspectit.ocelot.configdocsgenerator.model.AgentDocumentation;

import java.util.function.Supplier;

/**
 * Supplier to load an agent documentation lazy.
 * The documentation will be persisted after initial loading.
 */
public class AgentDocumentationSupplier implements Supplier<AgentDocumentation> {

    private final Supplier<AgentDocumentation> supplier;

    private AgentDocumentation documentation;

    public AgentDocumentationSupplier(Supplier<AgentDocumentation> supplier) {
        this.supplier = supplier;
    }

    @Override
    public AgentDocumentation get() {
        if(documentation == null) documentation = supplier.get();
        return documentation;
    }
}
