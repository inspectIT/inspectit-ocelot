package rocks.inspectit.ocelot.agentstatus;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;

/**
 * Container class for storing meta information about an agent that fetched a HTTP configuration.
 */
@Value
@EqualsAndHashCode
public class AgentMetaInformation {

    /**
     * The used header prefix.
     */
    private static final String HEADER_PREFIX = "x-ocelot-";

    /**
     * Name of the agent id header.
     */
    private static final String HEADER_AGENT_ID = HEADER_PREFIX + "agent-id";

    /**
     * Name of the agent version header.
     */
    private static final String HEADER_AGENT_VERSION = HEADER_PREFIX + "agent-version";

    /**
     * Name of the Java version header.
     */
    private static final String HEADER_JAVA_VERSION = HEADER_PREFIX + "java-version";

    /**
     * Name of the start time header.
     */
    private static final String HEADER_START_TIME = HEADER_PREFIX + "start-time";

    /**
     * Name of the VM name header.
     */
    private static final String HEADER_VM_NAME = HEADER_PREFIX + "vm-name";

    /**
     * Name of the VM vendor header.
     */
    private static final String HEADER_VM_VENDOR = HEADER_PREFIX + "vm-vendor";

    /**
     * Generates a {@link AgentMetaInformation} instance based on the given map which represents the used HTTP headers.
     * The headers will be considered to be sent by an agent if the give map contains an entry with key
     * <code>x-ocelot-agent-id</code>. In case the given headers are not belonging to an agent (e.g. the configuration
     * was fetched via CURL), <code>null</code> will be returned.
     *
     * @param headers the header values of the potential agent
     * @return {@link AgentMetaInformation} instance or <code>null</code>
     */
    public static AgentMetaInformation of(Map<String, String> headers) {
        if (headers != null && headers.containsKey("x-ocelot-agent-id")) {
            return new AgentMetaInformation(headers);
        } else {
            return null;
        }
    }

    /**
     * The agent id.
     */
    private String agentId;

    /**
     * The agent version.
     */
    private String agentVersion;

    /**
     * The used Java version.
     */
    private String javaVersion;

    /**
     * When the agent was starting up.
     */
    private String startTime;

    /**
     * The name of the JVM.
     */
    private String vmName;

    /**
     * The name of the JVM vendor.
     */
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
