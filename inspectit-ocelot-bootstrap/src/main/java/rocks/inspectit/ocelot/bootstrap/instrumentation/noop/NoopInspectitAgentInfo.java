package rocks.inspectit.ocelot.bootstrap.instrumentation.noop;

import rocks.inspectit.ocelot.bootstrap.exposed.InspectitAgentInfo;

public class NoopInspectitAgentInfo implements InspectitAgentInfo {

    public static final NoopInspectitAgentInfo INSTANCE = new NoopInspectitAgentInfo();

    @Override
    public String currentVersion() {
        return "UNKNOWN";
    }

    @Override
    public boolean isAtLeastVersion(String requiredVersion) {
        return false;
    }
}
