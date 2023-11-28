package rocks.inspectit.ocelot.core.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings.MeasureType;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings.Aggregation;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.exporter.ExporterServiceIntegrationTestBase;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction.ExecutionContext;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.MetricsRecorder;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.percentiles.PercentileViewManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.time.Duration;
import java.util.*;

/**
 * Integration Test Class for {@link MeasureTagValueGuard}
 */
@DirtiesContext
public class MeasureTagValueGuardIntTest extends ExporterServiceIntegrationTestBase {

    @Autowired
    InspectitEnvironment env;
    @Autowired
    CommonTagsManager commonTagsManager;
    @Autowired
    MeasuresAndViewsManager metricsManager;
    @Autowired
    MeasureTagValueGuard tagValueGuard;
    @Autowired
    PercentileViewManager percentileViewManager;

    static final String MEASURE_NAME = "my-counter";
    static final int VALUE = 42;
    static final String TAG_KEY = "test-tag-key";
    static final String TAG_VALUE_1 = "test-tag-value-1";
    static final String TAG_VALUE_2 = "test-tag-value-2";
    static final String OVERFLOW = "overflow";

    private ExecutionContext createExecutionContext() {
        InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), PropagationMetaData.builder().build(), false);
        return new ExecutionContext(null, this, "return", null, null,
                ctx, null);
    }

    /**
     * Update properties for OpenTelemetry-Collector & Tag-Guard
     * Create metric-definition for MEASURE_NAME
     */
    @BeforeEach
    void updateProperties() {
        ViewDefinitionSettings viewDefinition = new ViewDefinitionSettings();
        viewDefinition.setAggregation(Aggregation.SUM);
        viewDefinition.setTags(Collections.singletonMap(TAG_KEY, true));

        MetricDefinitionSettings metricDefinition = new MetricDefinitionSettings();
        metricDefinition.setEnabled(true);
        metricDefinition.setUnit("1");
        metricDefinition.setType(MeasureType.LONG);
        metricDefinition.setViews(Collections.singletonMap(MEASURE_NAME, viewDefinition));

        MetricsSettings metricsSettings = new MetricsSettings();
        metricsSettings.setDefinitions(Collections.singletonMap(MEASURE_NAME, metricDefinition));

        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.metrics.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT));
            mps.setProperty("inspectit.exporters.metrics.otlp.export-interval", "500ms");
            mps.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.GRPC);
            mps.setProperty("inspectit.metrics.tag-guard.enabled", true);
            mps.setProperty("inspectit.metrics.tag-guard.max-values-per-tag", 1);
            mps.setProperty("inspectit.metrics.tag-guard.schedule-delay", Duration.ofMillis(500));
            mps.setProperty("inspectit.metrics.tag-guard.overflow-replacement", OVERFLOW);
            mps.setProperty("inspectit.metrics.definitions." + MEASURE_NAME, metricDefinition);
        });
    }

    @Test
    void verifyTagValueOverflowReplacement() {
        VariableAccessor variableAccessor = (context) -> VALUE;
        Map<String, VariableAccessor> dataTags = new HashMap<>();
        dataTags.put(TAG_KEY, (context) -> TAG_VALUE_1);
        MetricAccessor metricAccessor = new MetricAccessor(MEASURE_NAME, variableAccessor, new HashMap<>(), dataTags);
        List<MetricAccessor> metrics = new LinkedList<>();
        metrics.add(metricAccessor);

        MetricsRecorder metricsRecorder = new MetricsRecorder(metrics, commonTagsManager, metricsManager, tagValueGuard);
        ExecutionContext executionContext = createExecutionContext();

        metricsRecorder.execute(executionContext);
        awaitMetricsExported(MEASURE_NAME, VALUE, TAG_KEY, TAG_VALUE_1);

        // for some reason, the ScheduledExecutorService is not working inside tests
        tagValueGuard.blockTagValuesTask.run();

        dataTags.put(TAG_KEY, (context) -> TAG_VALUE_2);
        metricsRecorder.execute(executionContext);
        // tag should have been replaced, due to overflow
        awaitMetricsExported(MEASURE_NAME, VALUE, TAG_KEY, OVERFLOW);
    }
}
