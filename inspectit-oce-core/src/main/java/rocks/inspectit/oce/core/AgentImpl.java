package rocks.inspectit.oce.core;

import rocks.inspectit.oce.bootstrap.IAgent;

public class AgentImpl implements IAgent {

    @Override
    public void start() {
        System.out.println("Starting Agent");
    }

    @Override
    public void destroy() {
        System.out.println("Shutting down Agent");
    }
}
