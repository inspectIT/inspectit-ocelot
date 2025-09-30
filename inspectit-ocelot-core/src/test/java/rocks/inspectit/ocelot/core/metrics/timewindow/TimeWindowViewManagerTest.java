package rocks.inspectit.ocelot.core.metrics.timewindow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.attributes.AttributeSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.AggregationType;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.TimeWindowView;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TimeWindowViewManagerTest {

    @InjectMocks
    TimeWindowViewManager viewManager;

    @Mock
    InspectitEnvironment env;

    @BeforeEach
    void beforeEach() {
        lenient().when(env.getCurrentConfig().getAttributes()).thenReturn(new AttributeSettings());
    }

    @Test
    void shouldRegisterViewWhenQuantilesAggregation() {
        String metricName = "my/metric";
        String viewName = "my/view";
        String unit = "ms";
        String desc = "description";
        Duration timeWindow = Duration.ofSeconds(1);
        int bufferLimit = 1000;
        ViewDefinitionSettings settings = new ViewDefinitionSettings();
        settings.setDescription(desc);
        settings.setTimeWindow(timeWindow);
        settings.setMaxBufferedPoints(bufferLimit);
        settings.setAggregation(AggregationType.QUANTILES);
        settings.setAttributes(Collections.emptyMap());

        viewManager.createOrUpdateView(metricName, viewName, unit, settings);
        Collection<TimeWindowView> views = viewManager.getViews(metricName);

        assertThat(views).allMatch(view -> view.getViewName().equals(viewName));
        assertThat(views).allMatch(view -> view.getUnit().equals(unit));
        assertThat(views).allMatch(view -> view.getDescription().equals(desc));
        assertThat(views).allMatch(view -> view.getTimeWindow().equals(timeWindow));
        assertThat(views).allMatch(view -> view.getBufferLimit() == bufferLimit);
    }

    @Test
    void shouldRegisterViewWhenSmoothedAverageAggregation() {
        String metricName = "my/metric";
        String viewName = "my/view";
        String unit = "ms";
        String desc = "description";
        Duration timeWindow = Duration.ofSeconds(1);
        int bufferLimit = 1000;
        ViewDefinitionSettings settings = new ViewDefinitionSettings();
        settings.setDescription(desc);
        settings.setTimeWindow(timeWindow);
        settings.setMaxBufferedPoints(bufferLimit);
        settings.setAggregation(AggregationType.SMOOTHED_AVERAGE);
        settings.setAttributes(Collections.emptyMap());

        viewManager.createOrUpdateView(metricName, viewName, unit, settings);
        Collection<TimeWindowView> views = viewManager.getViews(metricName);

        assertThat(views).allMatch(view -> view.getViewName().equals(viewName));
        assertThat(views).allMatch(view -> view.getUnit().equals(unit));
        assertThat(views).allMatch(view -> view.getDescription().equals(desc));
        assertThat(views).allMatch(view -> view.getTimeWindow().equals(timeWindow));
        assertThat(views).allMatch(view -> view.getBufferLimit() == bufferLimit);
    }

    @Test
    void shouldNotRegisterViewWhenOpenTelemetryAggregation() {
        String metricName = "my/metric";
        String viewName = "my/view";
        String unit = "ms";
        ViewDefinitionSettings settings = new ViewDefinitionSettings();
        settings.setAggregation(AggregationType.HISTOGRAM);

        assertThrows(IllegalArgumentException.class, () -> viewManager.createOrUpdateView(metricName, viewName, unit, settings));
    }
}
