package rocks.inspectit.ocelot.core.opentelemetry.metrics;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.internal.view.AttributesProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.AggregationType;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.metrics.timewindow.TimeWindowViewManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewManagerTest {

    @InjectMocks
    ViewManager viewManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitEnvironment env;

    @Mock
    CommonAttributesManager commonAttributes;

    @Mock
    TimeWindowViewManager timeWindowViewManager;

    static final String metricName = "my_metric";

    static final String viewName = "my_view";

    @BeforeEach
    void setUp() {
        lenient().when(env.getCurrentConfig().getMetrics().isEnabled()).thenReturn(true);
    }

    @Nested
    class ShouldUpdate {

        @Test
        void shouldUpdateViewsIfNothingCached() {
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(createDefaultMetric());

            boolean result = viewManager.shouldUpdateViews();

            assertThat(result).isTrue();
        }

        @Test
        void shouldNotUpdateViewsIfCached() {
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(createDefaultMetric());

            boolean result1 = viewManager.shouldUpdateViews();
            Map<InstrumentSelector, View> views1 = viewManager.processViews(result1);
            boolean result2 = viewManager.shouldUpdateViews();
            Map<InstrumentSelector, View> views2 = viewManager.processViews(result2);

            assertThat(result1).isTrue();
            assertThat(result2).isFalse();
            assertThat(viewManager.currentViews.size()).isEqualTo(1);
            assertThat(views1).isEqualTo(views2);
        }

        @Test
        void shouldUpdateViewsIfConfigurationChanged() {
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(createDefaultMetric());

            boolean result1 = viewManager.shouldUpdateViews();
            viewManager.processViews(result1);

            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(createDefaultMetric(AggregationType.SUM));
            boolean result2 = viewManager.shouldUpdateViews();

            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
            assertThat(viewManager.currentViews.size()).isEqualTo(1);
        }
    }

    @Nested
    class ProcessViews {

        @Test
        void shouldReturnEmptyViews() {
            Map<InstrumentSelector, View> views = viewManager.processViews(true);

            assertThat(views).isEmpty();
            assertThat(viewManager.currentViews).isEmpty();
        }

        @Test
        void shouldReturnViewForOpenTelemetryAggregation() {
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(createDefaultMetric());

            Map<InstrumentSelector, View> views = viewManager.processViews(true);
            View view = views.values().stream().findFirst().get();

            assertThat(viewManager.currentViews.size()).isEqualTo(1);
            assertThat(views.size()).isEqualTo(1);
            assertThat(view.getName()).isEqualTo(viewName);
            assertThat(view.getAggregation()).isEqualTo(Aggregation.lastValue());
        }

        @Test
        void shouldReturnNoViewForTimeWindowAggregation() {
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(createDefaultTimeWindowMetric());

            Map<InstrumentSelector, View> views = viewManager.processViews(true);

            assertThat(viewManager.currentViews.size()).isEqualTo(1);
            assertThat(views).isEmpty();
            verify(timeWindowViewManager).createOrUpdateView(anyString(), anyString(), anyString(), any(ViewDefinitionSettings.class));
        }

        @Test
        void shouldCreateViewsForBothAggregationTypes() {
            Map<String, MetricDefinitionSettings> metrics = new HashMap<>();
            metrics.put(metricName + "_test", createDefaultMetricSettings()); // we need a different metric name
            metrics.putAll(createDefaultTimeWindowMetric());
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(metrics);

            Map<InstrumentSelector, View> views = viewManager.processViews(true);

            assertThat(viewManager.currentViews.size()).isEqualTo(2);
            assertThat(views.size()).isEqualTo(1);
            verify(timeWindowViewManager).createOrUpdateView(anyString(), anyString(), anyString(), any(ViewDefinitionSettings.class));
        }

        @Test
        void shouldRemoveViewsWhenConfigurationIsMissing() {
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(createDefaultMetric());

            Map<InstrumentSelector, View> views1 = viewManager.processViews(true);

            assertThat(viewManager.currentViews.size()).isEqualTo(1);
            assertThat(views1.size()).isEqualTo(1);

            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(emptyMap());

            Map<InstrumentSelector, View> views2 = viewManager.processViews(true);

            assertThat(viewManager.currentViews).isEmpty();
            assertThat(views2).isEmpty();
        }
    }

    @Nested
    class ViewAttributesProcessor {

        @BeforeEach
        void setUp() {
            lenient().when(commonAttributes.getCommonAttributeKeys()).thenReturn(singletonList("common"));
        }

        @Test
        void shouldAllowCommonAttributesByDefault() throws Exception {
            Attributes attributes = Attributes.of(
                    stringKey("common"), "value",
                    stringKey("myTag"), "value"
            );
            MetricDefinitionSettings metric = createDefaultMetricSettings();
            Map<String, MetricDefinitionSettings> metrics = singletonMap(metricName, metric);
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(metrics);

            Map<InstrumentSelector, View> views = viewManager.processViews(true);
            View view = views.values().stream().findFirst().get();
            AttributesProcessor processor = getAttributesProcessor(view);

            Attributes result = processor.process(attributes, Context.current());

            assertThat(result.asMap()).containsOnlyKeys(stringKey("common"));
        }

        @Test
        void shouldAllowOnlySpecifiedAttributesWithCommonAttributes() throws Exception {
            Attributes attributes = Attributes.of(
                    stringKey("common"), "value1",
                    stringKey("myTag"), "value2",
                    stringKey("yourTag"), "value3"
            );
            MetricDefinitionSettings metric = createDefaultMetricSettings();
            ViewDefinitionSettings viewSettings = metric.getViews().get(viewName);
            viewSettings.setAttributes(singletonMap("myTag", true));
            Map<String, MetricDefinitionSettings> metrics = singletonMap(metricName, metric);
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(metrics);

            Map<InstrumentSelector, View> views = viewManager.processViews(true);
            View view = views.values().stream().findFirst().get();
            AttributesProcessor processor = getAttributesProcessor(view);

            Attributes result = processor.process(attributes, Context.current());

            assertThat(result.asMap()).containsOnlyKeys(stringKey("common"), stringKey("myTag"));
        }

        @Test
        void shouldAllowOnlySpecifiedAttributesWithoutCommonAttributes() throws Exception {
            Attributes attributes = Attributes.of(
                    stringKey("common"), "value1",
                    stringKey("myTag"), "value2",
                    stringKey("yourTag"), "value3"
            );
            MetricDefinitionSettings metric = createDefaultMetricSettings();
            ViewDefinitionSettings viewSettings = metric.getViews().get(viewName);
            viewSettings.setWithCommonAttributes(false);
            viewSettings.setAttributes(singletonMap("myTag", true)); // only allow this attribute
            Map<String, MetricDefinitionSettings> metrics = singletonMap(metricName, metric);
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(metrics);

            Map<InstrumentSelector, View> views = viewManager.processViews(true);
            View view = views.values().stream().findFirst().get();
            AttributesProcessor processor = getAttributesProcessor(view);

            Attributes result = processor.process(attributes, Context.current());

            assertThat(result.asMap()).containsOnlyKeys(stringKey("myTag"));
        }

        @Test
        void shouldAllowOnlySpecifiedAttributes() throws Exception {
            Attributes attributes = Attributes.of(
                    stringKey("common"), "value1",
                    stringKey("myTag"), "value2",
                    stringKey("yourTag"), "value3"
            );
            MetricDefinitionSettings metric = createDefaultMetricSettings();
            ViewDefinitionSettings viewSettings = metric.getViews().get(viewName);
            viewSettings.setAttributes(ImmutableMap.of("myTag", true, "common", false));
            Map<String, MetricDefinitionSettings> metrics = singletonMap(metricName, metric);
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(metrics);

            Map<InstrumentSelector, View> views = viewManager.processViews(true);
            View view = views.values().stream().findFirst().get();
            AttributesProcessor processor = getAttributesProcessor(view);

            Attributes result = processor.process(attributes, Context.current());

            assertThat(result.asMap()).containsOnlyKeys(stringKey("myTag"));
        }

        @Test
        void shouldAllowNoAttributes() throws Exception {
            Attributes attributes = Attributes.of(
                    stringKey("common"), "value1",
                    stringKey("myTag"), "value2",
                    stringKey("yourTag"), "value3"
            );
            MetricDefinitionSettings metric = createDefaultMetricSettings();
            ViewDefinitionSettings viewSettings = metric.getViews().get(viewName);
            viewSettings.setWithCommonAttributes(false);
            Map<String, MetricDefinitionSettings> metrics = singletonMap(metricName, metric);
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(metrics);

            Map<InstrumentSelector, View> views = viewManager.processViews(true);
            View view = views.values().stream().findFirst().get();
            AttributesProcessor processor = getAttributesProcessor(view);

            Attributes result = processor.process(attributes, Context.current());

            assertThat(result.size()).isEqualTo(0);
        }
    }

    private static MetricDefinitionSettings createDefaultMetricSettings() {
        MetricDefinitionSettings metric = new MetricDefinitionSettings();
        ViewDefinitionSettings view = new ViewDefinitionSettings();
        metric.setViews(singletonMap(viewName, view));
        metric.setUnit("1");

        return metric;
    }

    private static Map<String, MetricDefinitionSettings> createDefaultMetric() {
        return createDefaultMetric(AggregationType.LAST_VALUE);
    }

    private static Map<String, MetricDefinitionSettings> createDefaultTimeWindowMetric() {
        return createDefaultMetric(AggregationType.QUANTILES);
    }

    private static Map<String, MetricDefinitionSettings> createDefaultMetric(AggregationType aggregation) {
        MetricDefinitionSettings metric = new MetricDefinitionSettings();
        ViewDefinitionSettings view = new ViewDefinitionSettings();
        view.setAggregation(aggregation);
        metric.setViews(singletonMap(viewName, view));
        metric.setUnit("1");

        return singletonMap(metricName, metric);
    }

    /**
     * Since the attributes processor of a view is private, we use reflection to access and validate it.
     */
    private static AttributesProcessor getAttributesProcessor(View view) throws NoSuchFieldException, IllegalAccessException {
        Field field = view.getClass().getDeclaredField("attributesProcessor");
        field.setAccessible(true);
        return (AttributesProcessor) field.get(view);
    }
}
