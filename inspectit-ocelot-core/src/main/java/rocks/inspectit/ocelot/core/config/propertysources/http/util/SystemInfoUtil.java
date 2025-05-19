package rocks.inspectit.ocelot.core.config.propertysources.http.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import rocks.inspectit.ocelot.commons.models.info.AgentSystemInformation;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to collect relevant system information about OS, processor etc.
 */
@Slf4j
public class SystemInfoUtil {

    private static AgentSystemInformation systemInformation;

    /**
     * @return the system information as JSON string
     */
    public static String collect() {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{}";

        // only collect infos if requested
        if(systemInformation == null) collectSystemInfo();

        try {
            json = objectMapper.writeValueAsString(systemInformation);
        } catch (Exception e) {
            log.error("Couldn't serialize system info: {}", e.getMessage());
        }

        return json;
    }

    /**
     * Collects and stores system information
     */
    private static void collectSystemInfo() {
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
        agentSystemInfo.setOsHost(os.getNetworkParams().getHostName());

        ComputerSystem computerSystem = systemInfo.getHardware().getComputerSystem();
        String csModel = computerSystem.getManufacturer() + " " + computerSystem.getModel();
        agentSystemInfo.setCsModel(csModel);
        agentSystemInfo.setCsSerialNumber(computerSystem.getSerialNumber());

        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        agentSystemInfo.setLogicalProcessors(processor.getLogicalProcessorCount());
        agentSystemInfo.setPhysicalProcessors(processor.getPhysicalProcessorCount());

        CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();
        agentSystemInfo.setProcessorName(processorIdentifier.getName());
        agentSystemInfo.setProcessorIdentifier(processorIdentifier.getIdentifier());

        systemInformation = agentSystemInfo;
    }

    /**
     * Converts the up-time to the start-time.
     */
    private static long convertToStartTime(long upTime) {
        long now = System.currentTimeMillis();
        return now - (upTime * 1000);
    }
}
