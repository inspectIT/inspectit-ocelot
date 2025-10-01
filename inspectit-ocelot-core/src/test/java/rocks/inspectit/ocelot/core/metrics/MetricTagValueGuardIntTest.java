package rocks.inspectit.ocelot.core.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.metrics.InstrumentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.AggregationType;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.exporter.ExporterServiceIntegrationTestBase;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.context.session.PropagationSessionStorage;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction.ExecutionContext;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.metrics.MetricsRecorder;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;

import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

/**
 * Integration Test Class for {@link MetricTagValueGuard}
 */
@DirtiesContext
class MetricTagValueGuardIntTest extends ExporterServiceIntegrationTestBase {

    @Autowired
    InspectitEnvironment env;

    @Autowired
    CommonAttributesManager commonAttributes;

    @Autowired
    InstrumentManager instrumentManager;

    @Autowired
    MetricTagValueGuard tagValueGuard;

    static final String METRIC_NAME = "my-counter";

    static final int VALUE = 42;

    static final String ATTRIBUTE_KEY = "test-key";

    static final String VALUE_1 = "test-value-1";

    static final String VALUE_2 = "test-value-2";

    static final String OVERFLOW = "overflow";

    private ExecutionContext createExecutionContext() {
        InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), PropagationMetaData.builder().build(),
                new PropagationSessionStorage(), false);
        return new ExecutionContext(null, this, "return", null, null,
                ctx, null);
    }

    /**
     * Update properties for OpenTelemetry-Collector & Tag-Guard
     * Create metric-definition for METRIC_NAME
     */
    @BeforeEach
    void updateProperties() {
        ViewDefinitionSettings viewDefinition = new ViewDefinitionSettings();
        viewDefinition.setAggregation(AggregationType.SUM);
        viewDefinition.setAttributes(Collections.singletonMap(ATTRIBUTE_KEY, true));

        MetricDefinitionSettings metricDefinition = new MetricDefinitionSettings();
        metricDefinition.setUnit("1");
        metricDefinition.setInstrumentType(InstrumentType.COUNTER);
        metricDefinition.setViews(Collections.singletonMap(METRIC_NAME, viewDefinition));

        MetricsSettings metricsSettings = new MetricsSettings();
        metricsSettings.setDefinitions(Collections.singletonMap(METRIC_NAME, metricDefinition));

        updateProperties(mps -> {
            mps.setProperty("inspectit.exporters.metrics.otlp.endpoint", getEndpoint(COLLECTOR_OTLP_GRPC_PORT));
            mps.setProperty("inspectit.exporters.metrics.otlp.export-interval", "500ms");
            mps.setProperty("inspectit.exporters.metrics.otlp.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.metrics.otlp.protocol", TransportProtocol.GRPC);
            mps.setProperty("inspectit.metrics.tag-guard.enabled", true);
            mps.setProperty("inspectit.metrics.tag-guard.max-values-per-attribute", 1);
            mps.setProperty("inspectit.metrics.tag-guard.schedule-delay", Duration.ofMillis(500));
            mps.setProperty("inspectit.metrics.tag-guard.overflow-replacement", OVERFLOW);
            mps.setProperty("inspectit.metrics.definitions." + METRIC_NAME, metricDefinition);
        });
    }

    @AfterEach
    void cleanUp() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    void verifyTagValueOverflowReplacement() {
        VariableAccessor variableAccessor = (context) -> VALUE;
        Map<String, VariableAccessor> data = new HashMap<>();
        data.put(ATTRIBUTE_KEY, (context) -> VALUE_1);
        MetricAccessor metricAccessor = new MetricAccessor(METRIC_NAME, variableAccessor, emptyMap(), data);
        List<MetricAccessor> metrics = singletonList(metricAccessor);

        MetricsRecorder metricsRecorder = new MetricsRecorder(metrics, tagValueGuard, instrumentManager);
        ExecutionContext executionContext = createExecutionContext();

        metricsRecorder.execute(executionContext);
        awaitMetricsExported(METRIC_NAME, VALUE, ATTRIBUTE_KEY, VALUE_1);

        // for some reason, the ScheduledExecutorService is not working inside tests
        tagValueGuard.blockAttributeValuesTask.run();

        data.put(ATTRIBUTE_KEY, (context) -> VALUE_2);
        metricsRecorder.execute(executionContext);
        // tag should have been replaced, due to overflow
        awaitMetricsExported(METRIC_NAME, VALUE, ATTRIBUTE_KEY, OVERFLOW);
    }
}
