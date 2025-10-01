package rocks.inspectit.ocelot.core.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentManagerTest {

    @InjectMocks
    InstrumentManager manager;

    @Mock
    InstrumentFactory factory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitEnvironment env;

    final String metricName = "my-metric";

    @BeforeEach
    void setUp() {
        LongCounter counter = OpenTelemetry.noop()
                .getMeter("inspectit.test")
                .counterBuilder("noop-test-counter")
                .build();
        lenient().when(factory.createInstrument(anyString(), any())).thenReturn(counter);
    }

    @Test
    void shouldCreateInstrumentAndProcessAttributes() {
        MetricDefinitionSettings metric = new MetricDefinitionSettings();
        ViewDefinitionSettings view = new ViewDefinitionSettings();
        metric.setViews(singletonMap("my-view", view));
        Map<String, MetricDefinitionSettings> metrics = singletonMap(metricName, metric);

        manager.processInstrumentUpdates(metrics);

        verify(factory).createInstrument(anyString(), any(MetricDefinitionSettings.class));
    }

    @Test
    void shouldCreateInstrumentAndProcessAttributesWhenNoViews() {
        MetricDefinitionSettings metric = new MetricDefinitionSettings();
        Map<String, MetricDefinitionSettings> metrics = singletonMap(metricName, metric);

        manager.processInstrumentUpdates(metrics);

        verify(factory).createInstrument(anyString(), any(MetricDefinitionSettings.class));
    }

    @Test
    void shouldNotCreateInstrumentAndProcessAttributesWhenTimeWindowView() {
        MetricDefinitionSettings metric = new MetricDefinitionSettings();
        ViewDefinitionSettings view = new ViewDefinitionSettings();
        view.setAggregation(AggregationType.QUANTILES);
        metric.setViews(singletonMap("my-view", view));
        Map<String, MetricDefinitionSettings> metrics = singletonMap(metricName, metric);

        manager.processInstrumentUpdates(metrics);

        verifyNoInteractions(factory);
    }

    // TODO Test runtime updates
}
