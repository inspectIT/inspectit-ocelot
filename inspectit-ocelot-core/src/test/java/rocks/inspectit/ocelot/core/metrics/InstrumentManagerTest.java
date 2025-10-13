package rocks.inspectit.ocelot.core.metrics;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongGauge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.AggregationType;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.metrics.timewindow.worker.TimeWindowRecorder;
import rocks.inspectit.ocelot.core.opentelemetry.events.OpenTelemetryConfiguredEvent;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentManagerTest {

    @InjectMocks
    InstrumentManager manager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitEnvironment env;

    @Mock
    InstrumentFactory factory;

    @Mock
    TimeWindowRecorder timeWindowRecorder;

    @Mock
    OpenTelemetryConfiguredEvent event;

    @Mock
    LongGauge gauge;

    static final String metricName = "my_metric";

    static final String viewName = "my_view";

    @BeforeEach
    void setUp() {
        lenient().when(factory.createInstrument(anyString(), any())).thenReturn(Optional.ofNullable(gauge));
        lenient().when(env.getCurrentConfig().getMetrics().isEnabled()).thenReturn(true);
    }

    @Nested
    class CreateInstruments {

        @Test
        void shouldCreateInstrument() {
            Map<String, MetricDefinitionSettings> metrics = createDefaultMetric();

            manager.processInstrumentUpdates(metrics, false);

            assertThat(manager.isInstrumentRegistered(metricName)).isTrue();
            verify(factory).createInstrument(anyString(), any(MetricDefinitionSettings.class));
        }

        @Test
        void shouldNotCreateInstrumentWhenTimeWindowView() {
            Map<String, MetricDefinitionSettings> metrics = createDefaultTimeWindowMetric();

            manager.processInstrumentUpdates(metrics, false);

            assertThat(manager.isInstrumentRegistered(metricName)).isFalse();
            verifyNoInteractions(factory);
        }
    }

    @Nested
    class UpdateInstruments {

        @Test
        void shouldRemoveInstrumentWhenConfigurationIsMissing() {
            when(event.isSuccess()).thenReturn(true);
            when(event.isUpdateMetrics()).thenReturn(false);
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(createDefaultMetric());

            manager.updateInstruments(event);
            assertThat(manager.isInstrumentRegistered(metricName));
            verify(factory).createInstrument(anyString(), any());

            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(emptyMap());

            manager.updateInstruments(event);
            assertThat(manager.isInstrumentRegistered(metricName)).isFalse();
            verifyNoMoreInteractions(factory);
        }

        @Test
        void shouldRemoveInstrumentWhenChangedToTimeWindowMetric() {
            when(event.isSuccess()).thenReturn(true);
            when(event.isUpdateMetrics()).thenReturn(false);
            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(createDefaultMetric());

            manager.updateInstruments(event);
            assertThat(manager.isInstrumentRegistered(metricName));
            verify(factory).createInstrument(anyString(), any());

            when(env.getCurrentConfig().getMetrics().getDefinitions()).thenReturn(createDefaultTimeWindowMetric());

            manager.updateInstruments(event);
            assertThat(manager.isInstrumentRegistered(metricName)).isFalse();
            verifyNoMoreInteractions(factory);
        }

        @Test
        void shouldNotUpdateInstrumentWhenMetricsDisabled() {
            when(event.isSuccess()).thenReturn(true);
            when(env.getCurrentConfig().getMetrics().isEnabled()).thenReturn(false);

            manager.updateInstruments(event);
            assertThat(manager.isInstrumentRegistered(metricName)).isFalse();
            verifyNoInteractions(factory);
        }

        @Test
        void shouldReturnNothingToRemoveWhenCreated() {
            Map<String, MetricDefinitionSettings> metrics = createDefaultMetric();

            Set<String> toBeRemoved = manager.processInstrumentUpdates(metrics, false);

            assertThat(toBeRemoved).isEmpty();
        }

        @Test
        void shouldReturnInstrumentToRemoveWhenConfigurationMissing() {
            Map<String, MetricDefinitionSettings> metrics = createDefaultMetric();

            manager.processInstrumentUpdates(metrics, false);
            Set<String> toBeRemoved = manager.processInstrumentUpdates(emptyMap(), false);

            assertThat(toBeRemoved).isEqualTo(metrics.keySet());
        }

        @Test
        void shouldReturnInstrumentToRemoveWhenChangedToTimeWindowMetric() {
            Map<String, MetricDefinitionSettings> metrics = createDefaultMetric();

            manager.processInstrumentUpdates(metrics, false);
            Set<String> toBeRemoved = manager.processInstrumentUpdates(createDefaultTimeWindowMetric(), false);

            assertThat(toBeRemoved).isEqualTo(metrics.keySet());
        }

        @Test
        void shouldReturnNothingToRemoveWhenNothingRegistered() {
            Set<String> toBeRemoved = manager.processInstrumentUpdates(emptyMap(), false);

            assertThat(toBeRemoved).isEmpty();
        }

        @Test
        void shouldReturnNothingToRemoveWhenConfigurationChanged() {
            Map<String, MetricDefinitionSettings> metrics = createDefaultMetric();
            Map<String, MetricDefinitionSettings> changedMetrics = createDefaultMetric(AggregationType.SUM);

            manager.processInstrumentUpdates(metrics, false);
            Set<String> toBeRemoved = manager.processInstrumentUpdates(changedMetrics, false);

            assertThat(toBeRemoved).isEmpty();
        }
    }

    @Nested
    class RecordMetric {

        final static int VALUE = 42;

        @Test
        void shouldRecordValueForRegisteredInstrument() {
            manager.processInstrumentUpdates(createDefaultMetric(), false);

            boolean recorded = manager.tryRecordingMetric(metricName, VALUE, Baggage.empty());

            assertThat(recorded).isTrue();
            verify(gauge).set(VALUE, Attributes.empty());
        }

        @Test
        void shouldNotRecordValueForUnknownInstrument() {
            boolean recorded = manager.tryRecordingMetric(metricName, VALUE, Baggage.empty());

            assertThat(recorded).isFalse();
        }

        @Test
        void shouldRecordTimeWindowMetric() {
            when(timeWindowRecorder.recordMetric(metricName, VALUE, Baggage.empty())).thenReturn(true);

            boolean recorded = manager.tryRecordingMetric(metricName, VALUE, Baggage.empty());

            assertThat(recorded).isTrue();
        }

        @Test
        void shouldNotRecordTimeWindowMetric() {
            when(timeWindowRecorder.recordMetric(metricName, VALUE, Baggage.empty())).thenReturn(false);

            boolean recorded = manager.tryRecordingMetric(metricName, VALUE, Baggage.empty());

            assertThat(recorded).isFalse();
        }
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

        return singletonMap(metricName, metric);
    }
}
