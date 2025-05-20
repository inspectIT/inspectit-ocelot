package rocks.inspectit.ocelot.core.config.propertysources.http.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.software.os.OperatingSystem;
import rocks.inspectit.ocelot.commons.models.info.AgentSystemInformation;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * Helper class to collect relevant system information about OS, processor etc.
 */
@Slf4j
public class SystemInfoCollector {

    private static SystemInfoCollector instance;

    private final AgentSystemInformation systemInformation;

    private SystemInfoCollector() {
        this.systemInformation = collectSystemInfo();
    }

    /**
     * @return the singleton instance of the collector
     */
    public static SystemInfoCollector get() {
        if (instance == null) instance = new SystemInfoCollector();

        return instance;
    }

    /**
     * @return the agent system information as JSON string
     */
    public String collect() {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{}";

        try {
            json = objectMapper.writeValueAsString(systemInformation);
        } catch (Exception e) {
            log.error("Couldn't serialize system info: {}", e.getMessage());
        }

        return json;
    }

    /**
     * Collects and stores system information
     *
     * @return the agent system information
     */
    private AgentSystemInformation collectSystemInfo() {
        AgentSystemInformation agentSystemInfo = new AgentSystemInformation();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        agentSystemInfo.setVmName(runtime.getVmName());
        agentSystemInfo.setVmVendor(runtime.getVmVendor());

        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem os = systemInfo.getOperatingSystem();
        String osName = os.getFamily() + " " + os.getVersionInfo().getVersion();
        agentSystemInfo.setOsName(osName);
        agentSystemInfo.setOsArch(System.getProperty("os.arch"));
        agentSystemInfo.setOsStartTime(convertToStartTime(os.getSystemUptime()));

        ComputerSystem computerSystem = systemInfo.getHardware().getComputerSystem();
        String csModel = computerSystem.getManufacturer() + " " + computerSystem.getModel();
        agentSystemInfo.setCsModel(csModel);

        CentralProcessor.ProcessorIdentifier processorId = systemInfo.getHardware().getProcessor().getProcessorIdentifier();
        agentSystemInfo.setProcessorName(processorId.getName());
        agentSystemInfo.setProcessorIdentifier(processorId.getIdentifier());

        return agentSystemInfo;
    }

    /**
     * Converts the up-time to the start-time
     */
    private long convertToStartTime(long upTime) {
        long now = System.currentTimeMillis();
        return now - (upTime * 1000);
    }
}
