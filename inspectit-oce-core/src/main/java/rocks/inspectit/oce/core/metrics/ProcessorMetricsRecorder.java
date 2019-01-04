package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.MeasureMap;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;

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
    private static final String CPU_COUNT_METRIC_DESCRIPTION = "The number of processors available to the JVM";
    private static final String CPU_COUNT_METRIC_UNIT = "cores";

    private static final String AVERAGE_LOAD_METRIC_NAME = "system.average";
    private static final String AVERAGE_LOAD_METRIC_FULL_NAME = "system/load/average/1m";
    private static final String AVERAGE_LOAD_METRIC_DESCRIPTION =
            "The sum of the number of runnable entities queued to available processors and the number" +
                    "of runnable entities running on the available processors averaged over a period of time";
    private static final String AVERAGE_LOAD_METRIC_UNIT = "percentage";

    private static final String SYSTEM_USAGE_METRIC_NAME = "system.usage";
    private static final String SYSTEM_USAGE_METRIC_FULL_NAME = "system/cpu/usage";
    private static final String SYSTEM_USAGE_METRIC_DESCRIPTION = "The recent cpu usage for the whole system";
    private static final String SYSTEM_USAGE_METRIC_UNIT = "percentage";

    private static final String PROCESS_USAGE_METRIC_NAME = "process.usage";
    private static final String PROCESS_USAGE_METRIC_FULL_NAME = "process/cpu/usage";
    private static final String PROCESS_USAGE_METRIC_DESCRIPTION = "The recent cpu usage for the JVM's process";
    private static final String PROCESS_USAGE_METRIC_UNIT = "percentage";

    private static final List<String> OPERATING_SYSTEM_BEAN_CLASS_NAMES = Arrays.asList(
            "com.sun.management.OperatingSystemMXBean", // HotSpot
            "com.ibm.lang.management.OperatingSystemMXBean" // J9
    );

    private Runtime runtime;
    private OperatingSystemMXBean operatingSystemBean;
    private Optional<Method> systemCpuUsage;
    private Optional<Method> processCpuUsage;
    boolean averageLoadAvailable;

    public ProcessorMetricsRecorder() {
        super("metrics.processor");
    }

    @Override
    protected void init() {
        super.init();
        runtime = Runtime.getRuntime();
        operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
        systemCpuUsage = findOSBeanMethod("getSystemCpuLoad");
        processCpuUsage = findOSBeanMethod("getProcessCpuLoad");
        //returns negative values if unavailable
        averageLoadAvailable = operatingSystemBean.getSystemLoadAverage() >= 0;
        if (!systemCpuUsage.isPresent()) {
            log.info("Unable to locate 'getSystemCpuLoad' on operation system bean. Metric " + SYSTEM_USAGE_METRIC_FULL_NAME + " is unavailable.");
        }
        if (!systemCpuUsage.isPresent()) {
            log.info("Unable to locate 'getProcessCpuLoad' on operation system bean. Metric " + PROCESS_USAGE_METRIC_FULL_NAME + " is unavailable.");
        }
        if (!averageLoadAvailable) {
            log.info("'getAverageLoad()' is not available on this system. Metric " + AVERAGE_LOAD_METRIC_FULL_NAME + " is unavailable.");
        }

    }

    @Override
    protected void takeMeasurement(MetricsSettings config, MeasureMap mm) {
        Map<String, Boolean> enabled = config.getProcessor().getEnabled();
        if (enabled.getOrDefault(CPU_COUNT_METRIC_NAME, false)) {
            val measure = getOrCreateMeasureLongWithView(CPU_COUNT_METRIC_FULL_NAME, CPU_COUNT_METRIC_DESCRIPTION,
                    CPU_COUNT_METRIC_UNIT, Aggregation.LastValue::create);
            mm.put(measure, runtime.availableProcessors());
        }
        if (enabled.getOrDefault(AVERAGE_LOAD_METRIC_NAME, false) && averageLoadAvailable) {
            val measure = getOrCreateMeasureDoubleWithView(AVERAGE_LOAD_METRIC_FULL_NAME, AVERAGE_LOAD_METRIC_DESCRIPTION,
                    AVERAGE_LOAD_METRIC_UNIT, Aggregation.LastValue::create);
            mm.put(measure, operatingSystemBean.getSystemLoadAverage());
        }
        if (enabled.getOrDefault(SYSTEM_USAGE_METRIC_NAME, false) && systemCpuUsage.isPresent()) {
            val measure = getOrCreateMeasureDoubleWithView(SYSTEM_USAGE_METRIC_FULL_NAME, SYSTEM_USAGE_METRIC_DESCRIPTION,
                    SYSTEM_USAGE_METRIC_UNIT, Aggregation.LastValue::create);
            try {
                mm.put(measure, (Double) systemCpuUsage.get().invoke(operatingSystemBean));
            } catch (Exception e) {
                log.error("Error reading system cpu usage", e);
            }
        }
        if (enabled.getOrDefault(PROCESS_USAGE_METRIC_NAME, false) && processCpuUsage.isPresent()) {
            val measure = getOrCreateMeasureDoubleWithView(PROCESS_USAGE_METRIC_FULL_NAME, PROCESS_USAGE_METRIC_DESCRIPTION,
                    PROCESS_USAGE_METRIC_UNIT, Aggregation.LastValue::create);
            try {
                mm.put(measure, (Double) processCpuUsage.get().invoke(operatingSystemBean));
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
