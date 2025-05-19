package rocks.inspectit.ocelot.commons.models.info;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Container class for storing system information about an agent.
 */
@Data
@NoArgsConstructor
public class AgentSystemInformation {

    /**
     * The name of the JVM
     */
    private String vmName;

    /**
     * The vendor of the JVM
     */
    private String vmVendor;

    /**
     * The name of the OS
     */
    private String osName;

    /**
     * The arh of the OS
     */
    private String osArch;

    /**
     * The start time of the OS in epoch time
     */
    private long osStartTime;

    /**
     * The host name
     */
    private String osHost;

    /**
     * The model of the computer system
     */
    private String csModel;

    /**
     * The serial number of the computer system
     */
    private String csSerialNumber;

    /**
     * The amount of logical processors
     */
    private int logicalProcessors;

    /**
     * The amount of physical processors
     */
    private int physicalProcessors;

    /**
     * The name of the processor
     */
    private String processorName;

    /**
     * The identifier of the processor
     */
    private String processorIdentifier;
}
