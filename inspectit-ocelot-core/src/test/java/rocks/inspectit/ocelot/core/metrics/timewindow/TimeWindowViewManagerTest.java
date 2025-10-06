package rocks.inspectit.ocelot.core.metrics.timewindow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;
import rocks.inspectit.ocelot.config.model.attributes.AttributeSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.AggregationType;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.QuantilesView;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.SmoothedAverageView;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.TimeWindowView;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeWindowViewManagerTest {

    @InjectMocks
    TimeWindowViewManager viewManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitEnvironment env;

    @Mock
    CommonAttributesManager commonAttributes;

    static final String metricName = "my_metric";

    static final String viewName = "my_view";

    static final String unit = "ms";

    static final String desc = "description";

    static final Duration timeWindow = Duration.ofSeconds(1);

    static final int bufferLimit = 1000;

    @BeforeEach
    void beforeEach() {
        lenient().when(env.getCurrentConfig().getAttributes()).thenReturn(new AttributeSettings());
    }

    @Nested
    class CreateOrUpdateView {

        @Test
        void shouldCreateViewWhenQuantilesAggregation() {
            ViewDefinitionSettings settings = createView(AggregationType.QUANTILES);

            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            Collection<TimeWindowView> views = viewManager.getViews(metricName);

            verify(commonAttributes).getAttributeKeysForView(settings);
            assertThat(views.size()).isEqualTo(1);
            assertThat(views).allMatch(view -> view.getViewName().equals(viewName));
            assertThat(views).allMatch(view -> view.getUnit().equals(unit));
            assertThat(views).allMatch(view -> view.getDescription().equals(desc));
            assertThat(views).allMatch(view -> view.getTimeWindow().equals(timeWindow));
            assertThat(views).allMatch(view -> view.getBufferLimit() == bufferLimit);
            assertThat(views).allMatch(view -> view instanceof QuantilesView);
        }

        @Test
        void shouldCreateViewWhenSmoothedAverageAggregation() {
            ViewDefinitionSettings settings = createView(AggregationType.SMOOTHED_AVERAGE);

            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            Collection<TimeWindowView> views = viewManager.getViews(metricName);

            verify(commonAttributes).getAttributeKeysForView(settings);
            assertThat(views.size()).isEqualTo(1);
            assertThat(views).allMatch(view -> view.getViewName().equals(viewName));
            assertThat(views).allMatch(view -> view.getUnit().equals(unit));
            assertThat(views).allMatch(view -> view.getDescription().equals(desc));
            assertThat(views).allMatch(view -> view.getTimeWindow().equals(timeWindow));
            assertThat(views).allMatch(view -> view.getBufferLimit() == bufferLimit);
            assertThat(views).allMatch(view -> view instanceof SmoothedAverageView);
        }

        @Test
        void shouldNotCreateViewWhenOpenTelemetryAggregation() {
            ViewDefinitionSettings settings = createView(AggregationType.HISTOGRAM);

            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            Collection<TimeWindowView> views = viewManager.getViews(metricName);

            assertThat(views).isEmpty();
        }

        @Test
        void shouldChangeViewTypeWhenUpdated() {
            ViewDefinitionSettings settings1 = createView(AggregationType.SMOOTHED_AVERAGE);

            viewManager.createOrUpdateView(metricName, viewName, unit, settings1);
            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            assertThat(views1.size()).isEqualTo(1);
            assertThat(views1).allMatch(view -> view instanceof SmoothedAverageView);

            ViewDefinitionSettings settings2 = createView(AggregationType.QUANTILES);

            viewManager.createOrUpdateView(metricName, viewName, unit, settings2);
            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views2.size()).isEqualTo(1);
            assertThat(views2).allMatch(view -> view instanceof QuantilesView);

            viewManager.createOrUpdateView(metricName, viewName, unit, settings1);
            Collection<TimeWindowView> views3 = viewManager.getViews(metricName);

            assertThat(views3.size()).isEqualTo(1);
            assertThat(views3).allMatch(view -> view instanceof SmoothedAverageView);
        }

        @Test
        void shouldNotUpdateViewWhenOpenTelemetryAggregation() {
            ViewDefinitionSettings settings1 = createView(AggregationType.SMOOTHED_AVERAGE);

            viewManager.createOrUpdateView(metricName, viewName, unit, settings1);
            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            assertThat(views1.size()).isEqualTo(1);
            assertThat(views1).allMatch(view -> view instanceof SmoothedAverageView);

            ViewDefinitionSettings settings2 = createView(AggregationType.HISTOGRAM);

            viewManager.createOrUpdateView(metricName, viewName, unit, settings2);
            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views2.size()).isEqualTo(1);
            assertThat(views2).allMatch(view -> view instanceof SmoothedAverageView);
        }

        @Test
        void shouldNotUpdateWhenNothingChanged() {
            ViewDefinitionSettings settings1 = createView(AggregationType.SMOOTHED_AVERAGE);
            ViewDefinitionSettings settings2 = createView(AggregationType.QUANTILES);

            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views1).isEqualTo(views2);
        }

        @Test
        void shouldUpdateViewWhenUnitChanged() {
            ViewDefinitionSettings settings1 = createView(AggregationType.SMOOTHED_AVERAGE);
            ViewDefinitionSettings settings2 = createView(AggregationType.QUANTILES);

            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            viewManager.createOrUpdateView(metricName, viewName + "_avg", "2", settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", "2", settings2);

            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views1).noneMatch(views2::contains);
        }

        @Test
        void shouldUpdateViewWhenDescriptionChanged() {
            ViewDefinitionSettings settings1 = createView(AggregationType.SMOOTHED_AVERAGE);
            ViewDefinitionSettings settings2 = createView(AggregationType.QUANTILES);

            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            settings1.setDescription("updated");
            settings2.setDescription("updated");
            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views1).noneMatch(views2::contains);
        }

        @Test
        void shouldUpdateViewWhenTimeWindowChanged() {
            ViewDefinitionSettings settings1 = createView(AggregationType.SMOOTHED_AVERAGE);
            ViewDefinitionSettings settings2 = createView(AggregationType.QUANTILES);

            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            settings1.setTimeWindow(Duration.ofMillis(100));
            settings2.setTimeWindow(Duration.ofMillis(100));
            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views1).noneMatch(views2::contains);
        }

        @Test
        void shouldUpdateViewWhenBufferLimitChanged() {
            ViewDefinitionSettings settings1 = createView(AggregationType.SMOOTHED_AVERAGE);
            ViewDefinitionSettings settings2 = createView(AggregationType.QUANTILES);

            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            settings1.setMaxBufferedPoints(1);
            settings2.setMaxBufferedPoints(1);
            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views1).noneMatch(views2::contains);
        }

        @Test
        void shouldUpdateViewWhenAttributesChanged() {
            ViewDefinitionSettings settings1 = createView(AggregationType.SMOOTHED_AVERAGE);
            ViewDefinitionSettings settings2 = createView(AggregationType.QUANTILES);

            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            when(commonAttributes.getAttributeKeysForView(any())).thenReturn(ImmutableSet.of("attr"));
            settings1.setAttributes(ImmutableMap.of("attr", true));
            settings2.setAttributes(ImmutableMap.of("attr", true));
            viewManager.createOrUpdateView(metricName, viewName + "_avg", unit, settings1);
            viewManager.createOrUpdateView(metricName, viewName + "_quantiles", unit, settings2);

            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views1).noneMatch(views2::contains);
        }

        @Test
        void shouldUpdateViewWhenQuantilesChanged() {
            ViewDefinitionSettings settings = createView(AggregationType.QUANTILES);
            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            settings.setQuantiles(Arrays.asList(0.11, 0.55));
            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views1).noneMatch(views2::contains);
        }

        @Test
        void shouldUpdateViewWhenDropUpperChanged() {
            ViewDefinitionSettings settings = createView(AggregationType.SMOOTHED_AVERAGE);
            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            settings.setDropUpper(0.422);
            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views1).noneMatch(views2::contains);
        }

        @Test
        void shouldUpdateViewWhenDropLowerChanged() {
            ViewDefinitionSettings settings = createView(AggregationType.SMOOTHED_AVERAGE);
            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            Collection<TimeWindowView> views1 = viewManager.getViews(metricName);

            settings.setDropLower(0.422);
            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            Collection<TimeWindowView> views2 = viewManager.getViews(metricName);

            assertThat(views1).noneMatch(views2::contains);
        }
    }

    @Nested
    class RemoveView {

        @Test
        void shouldRemoveAllViewsForMetric() {
            ViewDefinitionSettings settings = createView(AggregationType.SMOOTHED_AVERAGE);
            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            viewManager.createOrUpdateView(metricName, viewName + "_2", unit, settings);

            viewManager.removeViews(metricName);
            Collection<TimeWindowView> views = viewManager.getViews(metricName);
            boolean result = viewManager.areAnyViewsRegistered(metricName);

            assertThat(views).isEmpty();
            assertThat(result).isFalse();
        }

        @Test
        void shouldRemoveSingleViewForMetric() {
            ViewDefinitionSettings settings = createView(AggregationType.SMOOTHED_AVERAGE);
            viewManager.createOrUpdateView(metricName, viewName, unit, settings);
            viewManager.createOrUpdateView(metricName, viewName + "_2", unit, settings);

            viewManager.removeView(metricName, viewName);
            Collection<TimeWindowView> views = viewManager.getViews(metricName);
            boolean result = viewManager.areAnyViewsRegistered(metricName);

            assertThat(views.size()).isEqualTo(1);
            assertThat(result).isTrue();
            assertThat(views).allMatch(view -> view.getViewName().equals(viewName + "_2"));
        }
    }

    private static ViewDefinitionSettings createView(AggregationType aggregation) {
        Duration timeWindow = Duration.ofSeconds(1);
        int bufferLimit = 1000;
        ViewDefinitionSettings settings = new ViewDefinitionSettings();
        settings.setDescription(desc);
        settings.setTimeWindow(timeWindow);
        settings.setMaxBufferedPoints(bufferLimit);
        settings.setAggregation(aggregation);
        settings.setAttributes(Collections.emptyMap());

        return settings;
    }
}
