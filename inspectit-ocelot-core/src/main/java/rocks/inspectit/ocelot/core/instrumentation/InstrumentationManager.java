package rocks.inspectit.ocelot.core.instrumentation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.event.ClassInstrumentedEvent;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;

/**
 * This class is responsible for (a) storing the active isntrumentatiuon configurations for each class
 * and (b) determining if a class requires an instrumentation change.
 */
@Service
@Slf4j
public class InstrumentationManager {

    @Autowired
    private InstrumentationConfigurationResolver configResolver;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    /**
     * For each class we remember the applied instrumentation.
     * This allows us to check if a retransformation is required.
     */
    private Cache<Class<?>, ClassInstrumentationConfiguration> activeInstrumentations =
            CacheBuilder.newBuilder().weakKeys().build();

    @EventListener
    private void classInstrumented(ClassInstrumentedEvent event) {
        ClassInstrumentationConfiguration config = event.getAppliedConfiguration();
        Class<?> clazz = event.getInstrumentedClass();

        if (ClassInstrumentationConfiguration.NO_INSTRUMENTATION.isSameAs(clazz, config)) {
            activeInstrumentations.invalidate(clazz);
        } else {
            activeInstrumentations.put(clazz, config);
        }
    }

    public boolean doesClassRequireRetransformation(Class<?> clazz) {
        try (val sm = selfMonitoring.withDurationSelfMonitoring("InstrumentationManager")) {
            ClassInstrumentationConfiguration requestedConfig = configResolver.getClassInstrumentationConfiguration(clazz);
            val activeConfig = activeInstrumentations.getIfPresent(clazz);
            if (activeConfig == null) {
                return !requestedConfig.isNoInstrumentation();
            } else {
                return !activeConfig.isSameAs(clazz, requestedConfig);
            }
        }
    }

}
