package rocks.inspectit.oce.core.selfmonitoring;

import io.opencensus.common.Scope;
import io.opencensus.stats.*;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.service.DynamicallyActivatableService;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class SelfMonitoringService extends DynamicallyActivatableService {

    private static final TagKey COMPONENT_TAG_KEY = TagKey.create("component-name");

    private static final AtomicReference<Measure.MeasureDouble> MEASURE_ATOMIC_REFERENCE = new AtomicReference<>();

    @Autowired
    private StatsRecorder statsRecorder;

    @Autowired
    private ViewManager viewManager;

    /**
     * Measure name.
     */
    private String measureName;

    /**
     * Created measure.
     */
    private Measure.MeasureDouble measure;

    /**
     * Default constructor.
     */
    public SelfMonitoringService() {
        super("selfMonitoring");
    }

    /**
     * Provides an auto-closable that can be used in try-with-resource form.
     * <p>
     * If self monitoring is enabled the {@link SelfMonitoringScope} instance is create that handles time measuring and measure recording.
     * If self monitoring is disabled, return no-ops closable.
     *
     * @param componentName
     * @return
     */
    public Scope withSelfMonitoring(String componentName) {
        if (super.isEnabled()) {
            return new SelfMonitoringScope(componentName, System.nanoTime());
        } else {
            return () -> {
            };
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        return conf.getSelfMonitoring().isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        measureName = configuration.getSelfMonitoring().getMeasureName();
        log.info("Enabling self monitoring.");
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doDisable() {
        log.info("Disabling self monitoring.");
        return true;
    }

    /**
     * Create the self monitoring measure lazy.
     * <p>
     * We use the help of the atomic reference to make sure only one thread creates the measure and registerer the view.
     * Later on, the access to the measure is non-atomic as the measure should be immutable.
     *
     * @return Measure.MeasureDouble
     */
    private Measure.MeasureDouble getSelfMonitoringMeasure() {
        if (null == measure) {
            Measure.MeasureDouble measureDouble = Measure.MeasureDouble.create(measureName, "inspectIT OCE self-monitoring duration", "μs");
            if (MEASURE_ATOMIC_REFERENCE.compareAndSet(null, measureDouble)) {
                viewManager.registerView(View.create(
                        View.Name.create(measureDouble.getName()),
                        measureDouble.getDescription() + String.format(" [%s]", measureDouble.getUnit()),
                        measureDouble,
                        Aggregation.Sum.create(),
                        Collections.singletonList(COMPONENT_TAG_KEY)
                ));
                measure = measureDouble;
                return measureDouble;
            } else {
                return MEASURE_ATOMIC_REFERENCE.get();
            }
        }
        return measure;
    }

    @Data
    public class SelfMonitoringScope implements Scope {

        private final String componentName;
        private final long start;

        @Override
        public void close() {
            double durationInMicros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
            statsRecorder.newMeasureMap()
                    .put(getSelfMonitoringMeasure(), durationInMicros)
                    .record(Tags.getTagger().emptyBuilder().put(COMPONENT_TAG_KEY, TagValue.create(componentName)).build());

            if (log.isTraceEnabled()) {
                log.trace(String.format("%s reported %.1fμs", componentName, durationInMicros));
            }
        }
    }


}
