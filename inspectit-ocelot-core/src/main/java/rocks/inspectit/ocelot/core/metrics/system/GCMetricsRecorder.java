package rocks.inspectit.ocelot.core.metrics.system;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.StandardMetricsSettings;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GCMetricsRecorder extends AbstractMetricsRecorder {

    private static final String METRIC_NAME_PREFIX = "jvm/gc/";

    private static final String CONCURRENT_PHASE_TIME_METRIC_NAME = "concurrent.phase.time";
    private static final String CONCURRENT_PHASE_TIME_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "concurrent/phase/time";

    private static final String PAUSE_METRIC_NAME = "pause";
    private static final String PAUSE_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "pause";

    private static final String MEMORY_PROMOTED_METRIC_NAME = "memory.promoted";
    private static final String MEMORY_PROMOTED_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "memory/promoted";

    private static final String MAX_DATA_SIZE_METRIC_NAME = "max.data.size";
    private static final String MAX_DATA_SIZE_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "max/data/size";

    private static final String LIVE_DATA_SIZE_METRIC_NAME = "live.data.size";
    private static final String LIVE_DATA_SIZE_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "live/data/size";

    private static final String MEMORY_ALLOCATED_METRIC_NAME = "memory.allocated";
    private static final String MEMORY_ALLOCATED_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "memory/allocated";

    private static final boolean MANAGEMENT_EXTENSIONS_PRESENT = isManagementExtensionsPresent();

    private final NotificationListener notificationListener = this::handleNotification;
    private StandardMetricsSettings config;

    private final TagKey actionTagKey = TagKey.create("action");
    private final TagKey causeTagKey = TagKey.create("cause");

    private String youngGenPoolName;
    private String oldGenPoolName;
    private long youngGenSizeAfter = 0L;

    @Autowired
    private Tagger tagger;

    @Autowired
    private SelfMonitoringService selfMonitoringService;

    public GCMetricsRecorder() {
        super("metrics.gc");
        if (!MANAGEMENT_EXTENSIONS_PRESENT) {
            log.info("com.sun.management.GarbageCollectionNotificationInfo is not available on this system, gc metrics are unavailable.");
        }
    }

    @Override
    protected boolean checkEnabledForConfig(MetricsSettings ms) {
        return ms.getGc().getEnabled().containsValue(true) && MANAGEMENT_EXTENSIONS_PRESENT;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        log.info("Enabling GC metrics recorder");
        config = configuration.getMetrics().getGc();
        initPoolProperties();
        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (mbean instanceof NotificationEmitter) {
                ((NotificationEmitter) mbean).addNotificationListener(notificationListener, null, null);
            }
        }
        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Disabling GC metrics recorder");
        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (mbean instanceof NotificationEmitter) {
                try {
                    ((NotificationEmitter) mbean).removeNotificationListener(notificationListener);
                } catch (ListenerNotFoundException e) {
                    log.error("Error disabling jmx listener", e);
                }
            }
        }
        return true;
    }

    private void initPoolProperties() {
        for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (isYoungGenPool(mbean.getName())) {
                youngGenPoolName = mbean.getName();
            }
            if (isOldGenPool(mbean.getName())) {
                oldGenPoolName = mbean.getName();
            }
        }
    }

    private void handleNotification(Notification notification, Object handback) {
        if (!isEnabled()) {
            return;
        }
        try (val sm = selfMonitoringService.withDurationSelfMonitoring(getClass().getSimpleName())) {

            String type = notification.getType();
            if (type.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                CompositeData cd = (CompositeData) notification.getUserData();
                GarbageCollectionNotificationInfo notificationInfo = GarbageCollectionNotificationInfo.from(cd);

                recordGcTimes(notificationInfo);

                GcInfo gcInfo = notificationInfo.getGcInfo();

                Map<String, MemoryUsage> before = gcInfo.getMemoryUsageBeforeGc();
                Map<String, MemoryUsage> after = gcInfo.getMemoryUsageAfterGc();

                recordOldGenSizes(notificationInfo, before, after);

                recordYoungGenSizes(before, after);
            }
        } catch (Exception e) {
            log.error("Error handling GC event", e);
        }

    }

    private void recordGcTimes(GarbageCollectionNotificationInfo notificationInfo) {
        if (isConcurrentPhase(notificationInfo.getGcCause())) {
            if (config.getEnabled().getOrDefault(CONCURRENT_PHASE_TIME_METRIC_NAME, false)) {
                recordConcurrentPhaseTime(notificationInfo);
            }
        } else {
            if (config.getEnabled().getOrDefault(PAUSE_METRIC_NAME, false)) {
                recordGCPause(notificationInfo);
            }
        }
    }

    private void recordOldGenSizes(GarbageCollectionNotificationInfo notificationInfo, Map<String, MemoryUsage> before, Map<String, MemoryUsage> after) {
        if (oldGenPoolName != null) {
            long oldBefore = before.get(oldGenPoolName).getUsed();
            long oldAfter = after.get(oldGenPoolName).getUsed();
            if (config.getEnabled().getOrDefault(MEMORY_PROMOTED_METRIC_NAME, false)) {
                long delta = oldAfter - oldBefore;
                if (delta > 0L) {
                    recordPromotedBytes(delta);
                }
            }

            // Some GC implementations such as G1 can reduce the old gen size as part of a minor GC. To track the
            // live data size we record the value if we see a reduction in the old gen heap size or
            // after a major GC.
            if (oldAfter < oldBefore || GcGenerationAge.fromName(notificationInfo.getGcName()) == GcGenerationAge.OLD) {
                if (config.getEnabled().getOrDefault(LIVE_DATA_SIZE_METRIC_NAME, false)) {
                    recordLiveDataSize(oldAfter);
                }
                if (config.getEnabled().getOrDefault(MAX_DATA_SIZE_METRIC_NAME, false)) {
                    recordMaxDataSize(Math.max(after.get(oldGenPoolName).getMax(), 0L));
                }
            }
        }
    }

    private void recordYoungGenSizes(Map<String, MemoryUsage> before, Map<String, MemoryUsage> after) {
        if (youngGenPoolName != null) {
            long youngBefore = before.get(youngGenPoolName).getUsed();
            long youngAfter = after.get(youngGenPoolName).getUsed();
            long delta = youngBefore - youngGenSizeAfter;
            youngGenSizeAfter = youngAfter;
            if (delta > 0L) {
                if (config.getEnabled().getOrDefault(MEMORY_ALLOCATED_METRIC_NAME, false)) {
                    recordAllocatedBytes(delta);
                }
            }
        }
    }

    private void recordConcurrentPhaseTime(GarbageCollectionNotificationInfo notificationInfo) {
        measureManager.getMeasureLong(CONCURRENT_PHASE_TIME_METRIC_FULL_NAME)
                .ifPresent(measure -> {
                    TagContext tags = tagger.toBuilder(commonTags.getCommonTagContext())
                            .putLocal(actionTagKey, TagValue.create(notificationInfo.getGcAction()))
                            .putLocal(causeTagKey, TagValue.create(notificationInfo.getGcCause()))
                            .build();

                    recorder.newMeasureMap()
                            .put(measure, notificationInfo.getGcInfo().getDuration())
                            .record(tags);
                });
    }

    private void recordGCPause(GarbageCollectionNotificationInfo notificationInfo) {
        measureManager.getMeasureLong(PAUSE_METRIC_FULL_NAME)
                .ifPresent(measure -> {
                    TagContext tags = tagger.toBuilder(commonTags.getCommonTagContext())
                            .putLocal(actionTagKey, TagValue.create(notificationInfo.getGcAction()))
                            .putLocal(causeTagKey, TagValue.create(notificationInfo.getGcCause()))
                            .build();

                    recorder.newMeasureMap()
                            .put(measure, notificationInfo.getGcInfo().getDuration())
                            .record(tags);

                });
    }

    private void recordPromotedBytes(long bytes) {
        measureManager.getMeasureLong(MEMORY_PROMOTED_METRIC_FULL_NAME)
                .ifPresent(measure ->
                        recorder.newMeasureMap()
                                .put(measure, bytes)
                                .record(commonTags.getCommonTagContext())
                );
    }

    private void recordAllocatedBytes(long bytes) {
        measureManager.getMeasureLong(MEMORY_ALLOCATED_METRIC_FULL_NAME)
                .ifPresent(measure ->
                        recorder.newMeasureMap()
                                .put(measure, bytes)
                                .record(commonTags.getCommonTagContext())
                );
    }

    private void recordLiveDataSize(long bytes) {
        measureManager.getMeasureLong(LIVE_DATA_SIZE_METRIC_FULL_NAME)
                .ifPresent(measure ->
                        recorder.newMeasureMap()
                                .put(measure, bytes)
                                .record(commonTags.getCommonTagContext())
                );
    }

    private void recordMaxDataSize(long bytes) {
        measureManager.getMeasureLong(MAX_DATA_SIZE_METRIC_FULL_NAME)
                .ifPresent(measure ->
                        recorder.newMeasureMap()
                                .put(measure, bytes)
                                .record(commonTags.getCommonTagContext())
                );
    }

    private static boolean isManagementExtensionsPresent() {
        try {
            Class.forName("com.sun.management.GarbageCollectionNotificationInfo", false, GCMetricsRecorder.class.getClassLoader());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean isConcurrentPhase(String cause) {
        return "No GC".equals(cause);
    }

    private boolean isOldGenPool(String name) {
        return name.endsWith("Old Gen") || name.endsWith("Tenured Gen");
    }

    private boolean isYoungGenPool(String name) {
        return name.endsWith("Eden Space");
    }
}

/**
 * Generalization of which parts of the heap are considered "young" or "old" for multiple GC implementations
 */
enum GcGenerationAge {
    OLD,
    YOUNG,
    UNKNOWN;

    private static Map<String, GcGenerationAge> knownCollectors = new HashMap<String, GcGenerationAge>() {{
        put("ConcurrentMarkSweep", OLD);
        put("Copy", YOUNG);
        put("G1 Old Generation", OLD);
        put("G1 Young Generation", YOUNG);
        put("MarkSweepCompact", OLD);
        put("PS MarkSweep", OLD);
        put("PS Scavenge", YOUNG);
        put("ParNew", YOUNG);
    }};

    static GcGenerationAge fromName(String name) {
        GcGenerationAge t = knownCollectors.get(name);
        return (t == null) ? UNKNOWN : t;
    }
}