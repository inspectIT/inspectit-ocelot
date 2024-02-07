package rocks.inspectit.ocelot.agentconfiguration;

import java.util.function.Supplier;

/**
 * Supplies the logic to provide an agent documentation.
 * The agent documentation will be created lazy.
 */
public class AgentDocumentationSupplier implements Supplier<AgentDocumentation> {

    /**
     * The supplier containing the logic to provide an agent documentation
     */
    private final Supplier<AgentDocumentation> supplier;

    /**
     * The objects, which should be supplied
     */
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
