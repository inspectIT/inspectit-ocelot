package rocks.inspectit.ocelot.agentstatus;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;

@Value
@EqualsAndHashCode
public class AgentMetaInformation {

    private static final String HEADER_PREFIX = "x-ocelot-";

    private static final String HEADER_AGENT_ID = HEADER_PREFIX + "agent-id";

    private static final String HEADER_AGENT_VERSION = HEADER_PREFIX + "agent-version";

    private static final String HEADER_JAVA_VERSION = HEADER_PREFIX + "java-version";

    private static final String HEADER_START_TIME = HEADER_PREFIX + "start-time";

    private static final String HEADER_VM_NAME = HEADER_PREFIX + "vm-name";

    private static final String HEADER_VM_VENDOR = HEADER_PREFIX + "vm-vendor";

    public static AgentMetaInformation of(Map<String, String> headers) {
        if (headers.containsKey("x-ocelot-agent-id")) {
            return new AgentMetaInformation(headers);
        } else {
            return null;
        }
    }

    private String agentId;

    private String agentVersion;

    private String javaVersion;

    private String startTime;

    private String vmName;

    private String vmVendor;

    private AgentMetaInformation(Map<String, String> headers) {
        agentId = headers.get(HEADER_AGENT_ID);
        agentVersion = headers.get(HEADER_AGENT_VERSION);
        javaVersion = headers.get(HEADER_JAVA_VERSION);
        startTime = headers.get(HEADER_START_TIME);
        vmName = headers.get(HEADER_VM_NAME);
        vmVendor = headers.get(HEADER_VM_VENDOR);
    }
}
