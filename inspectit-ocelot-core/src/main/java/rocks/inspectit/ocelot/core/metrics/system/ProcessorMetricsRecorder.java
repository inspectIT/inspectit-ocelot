package rocks.inspectit.ocelot.core.metrics.system;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
public class ProcessorMetricsRecorder extends AbstractPollingMetricsRecorder {

    private static final String CPU_COUNT_METRIC_NAME = "count";

    private static final String CPU_COUNT_METRIC_FULL_NAME = "system/cpu/count";

    private static final String AVERAGE_LOAD_METRIC_NAME = "system.average";

    private static final String AVERAGE_LOAD_METRIC_FULL_NAME = "system/load/average/1m";

    private static final String SYSTEM_USAGE_METRIC_NAME = "system.usage";

    private static final String SYSTEM_USAGE_METRIC_FULL_NAME = "system/cpu/usage";

    private static final String PROCESS_USAGE_METRIC_NAME = "process.usage";

    private static final String PROCESS_USAGE_METRIC_FULL_NAME = "process/cpu/usage";

    private static final String METRIC_UNAVAILABLE = "Unable to locate '{}' on operation system bean. Metric \" {} \" is unavailable.";

    private static final List<String> OPERATING_SYSTEM_BEAN_CLASS_NAMES = Arrays.asList(
            "com.sun.management.OperatingSystemMXBean", // HotSpot
            "com.ibm.lang.management.OperatingSystemMXBean" // J9
    );

    private Runtime runtime;

    private OperatingSystemMXBean operatingSystemBean;

    private Optional<Method> systemCpuUsage;

    private Optional<Method> processCpuUsage;

    private boolean averageLoadAvailable;

    public ProcessorMetricsRecorder() {
        super("metrics.processor");
    }

    @Override
    protected void init() {
        super.init();
        final String METH_SYS_CPU_LOAD = "getSystemCpuLoad";
        final String METH_PROC_CPU_LOAD = "getProcessCpuLoad";
        runtime = Runtime.getRuntime();
        operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
        systemCpuUsage = findOSBeanMethod(METH_SYS_CPU_LOAD);
        processCpuUsage = findOSBeanMethod(METH_PROC_CPU_LOAD);
        //returns negative values if unavailable
        averageLoadAvailable = operatingSystemBean.getSystemLoadAverage() >= 0;
        if (!systemCpuUsage.isPresent()) {
            log.info(METRIC_UNAVAILABLE, METH_SYS_CPU_LOAD, SYSTEM_USAGE_METRIC_FULL_NAME);
        }
        if (!systemCpuUsage.isPresent()) {
            log.info(METRIC_UNAVAILABLE, METH_PROC_CPU_LOAD, PROCESS_USAGE_METRIC_FULL_NAME);
        }
        if (!averageLoadAvailable) {
            log.info(METRIC_UNAVAILABLE, "getAverageLoad", AVERAGE_LOAD_METRIC_FULL_NAME);
        }

    }

    @Override
    protected void takeMeasurement(MetricsSettings config) {
        Map<String, Boolean> enabled = config.getProcessor().getEnabled();
        if (enabled.getOrDefault(CPU_COUNT_METRIC_NAME, false)) {
            measureManager.tryRecordingMeasurement(CPU_COUNT_METRIC_FULL_NAME, runtime.availableProcessors());
        }
        if (enabled.getOrDefault(AVERAGE_LOAD_METRIC_NAME, false) && averageLoadAvailable) {
            measureManager.tryRecordingMeasurement(AVERAGE_LOAD_METRIC_FULL_NAME, operatingSystemBean.getSystemLoadAverage());
        }
        if (enabled.getOrDefault(SYSTEM_USAGE_METRIC_NAME, false) && systemCpuUsage.isPresent()) {
            try {
                double value = (double) systemCpuUsage.get().invoke(operatingSystemBean);
                if (value >= 0D) {
                    measureManager.tryRecordingMeasurement(SYSTEM_USAGE_METRIC_FULL_NAME, value);
                }
            } catch (Exception e) {
                log.error("Error reading system cpu usage", e);
            }
        }
        if (enabled.getOrDefault(PROCESS_USAGE_METRIC_NAME, false) && processCpuUsage.isPresent()) {
            try {
                double value = (double) processCpuUsage.get().invoke(operatingSystemBean);
                if (value >= 0D) {
                    measureManager.tryRecordingMeasurement(PROCESS_USAGE_METRIC_FULL_NAME, value);
                }
            } catch (Exception e) {
                log.error("Error reading system cpu usage", e);
            }
        }
    }

    @Override
    protected Duration getFrequency(MetricsSettings config) {
        return config.getProcessor().getFrequency();
    }

    @Override
    protected boolean checkEnabledForConfig(MetricsSettings ms) {
        val enabled = new HashMap<>(ms.getProcessor().getEnabled());
        if (!systemCpuUsage.isPresent()) {
            enabled.remove(SYSTEM_USAGE_METRIC_NAME);
        }
        if (!processCpuUsage.isPresent()) {
            enabled.remove(PROCESS_USAGE_METRIC_NAME);
        }
        if (!averageLoadAvailable) {
            enabled.remove(AVERAGE_LOAD_METRIC_NAME);
        }
        return enabled.containsValue(true);
    }

    private Optional<Method> findOSBeanMethod(String methodName) {
        return OPERATING_SYSTEM_BEAN_CLASS_NAMES.stream().flatMap((cn) -> {
            try {
                return Stream.of(Class.forName(cn));
            } catch (ClassNotFoundException e) {
                return Stream.<Class<?>>empty();
            }
        }).flatMap(clazz -> {
            try {
                clazz.cast(operatingSystemBean);
                return Stream.of(clazz.getDeclaredMethod(methodName));
            } catch (ClassCastException | NoSuchMethodException | SecurityException e) {
                return Stream.empty();
            }
        }).findFirst();
    }

}
