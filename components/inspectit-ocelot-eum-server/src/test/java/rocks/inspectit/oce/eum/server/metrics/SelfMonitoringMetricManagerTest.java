package rocks.inspectit.oce.eum.server.metrics;

import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.eum.server.configuration.model.EumSelfMonitoringSettings;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests {@link SelfMonitoringMetricManager}
 */
@ExtendWith(MockitoExtension.class)
public class SelfMonitoringMetricManagerTest {

    @InjectMocks
    SelfMonitoringMetricManager selfMonitoringMetricManager;

    @Mock
    EumServerConfiguration configuration;

    @Mock
    MeasuresAndViewsManager measuresAndViewsManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    StatsRecorder statsRecorder;

    @Mock
    ViewManager viewManager;

    @Mock
    MeasureMap measureMap;

    @Nested
    class record {

        private EumSelfMonitoringSettings eumSelfMonitoringSettings = new EumSelfMonitoringSettings();

        @BeforeEach
        void setupConfiguration() {
            ViewDefinitionSettings view = ViewDefinitionSettings.builder()
                    .aggregation(ViewDefinitionSettings.Aggregation.COUNT)
                    .tag("TAG_1", true)
                    .build();
            Map<String, ViewDefinitionSettings> views = new HashMap<>();
            views.put("inspectit-eum/self/beacons_received/COUNT", view);

            MetricDefinitionSettings dummyMetricDefinition = MetricDefinitionSettings.builder()
                    .description("Dummy description")
                    .type(rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings.MeasureType.DOUBLE)
                    .unit("number")
                    .enabled(true)
                    .views(views)
                    .build();

            Map<String, MetricDefinitionSettings> definitionMap = new HashMap<>();
            definitionMap.put("beacons_received", dummyMetricDefinition);
            eumSelfMonitoringSettings.setEnabled(true);
            eumSelfMonitoringSettings.setMetrics(definitionMap);
            eumSelfMonitoringSettings.setMetricPrefix("inspectit-eum/self/");
        }

        @Test
        void verifyNoViewIsGeneratedWithDisabledSelfMonitoring() {
            eumSelfMonitoringSettings.setEnabled(false);
            when(configuration.getSelfMonitoring()).thenReturn(eumSelfMonitoringSettings);

            selfMonitoringMetricManager.record("beacons_received", 1);

            verifyZeroInteractions(viewManager, statsRecorder);
        }

        @Test
        void verifyNoViewIsGeneratedWithNonExistentMetric() {
            when(configuration.getSelfMonitoring()).thenReturn(eumSelfMonitoringSettings);

            selfMonitoringMetricManager.record("apples_received", 1);

            verifyZeroInteractions(viewManager, statsRecorder);
        }

        @Test
        void verifySelfMonitoringMetricManagerIsCalled() {
            when(configuration.getSelfMonitoring()).thenReturn(eumSelfMonitoringSettings);

            selfMonitoringMetricManager.initMetrics();

            ArgumentCaptor<MetricDefinitionSettings> mdsCaptor = ArgumentCaptor.forClass(MetricDefinitionSettings.class);
            verify(measuresAndViewsManager).updateMetrics(eq("inspectit-eum/self/beacons_received"), mdsCaptor.capture());
            verifyZeroInteractions(viewManager, statsRecorder, measureMap);

            assertThat(mdsCaptor.getValue()
                    .getViews()
                    .keySet()).containsExactly("inspectit-eum/self/beacons_received/COUNT");
        }
    }
}
