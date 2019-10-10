package rocks.inspectit.oce.eum.server.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagKey;
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
import rocks.inspectit.oce.eum.server.configuration.model.EumTagsSettings;
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
    EumTagsSettings tagSettings;

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
        void verifyViewsAreGeneratedExtraTagIsSet() {
            Map<String, String> extraTags = new HashMap<>();
            extraTags.put("TAG_1", "tag_value_1");
            extraTags.put("TAG_2", "tag_value_2");

            when(tagSettings.getExtra()).thenReturn(extraTags);

            when(configuration.getSelfMonitoring()).thenReturn(eumSelfMonitoringSettings);
            when(configuration.getTags()).thenReturn(tagSettings);
            HashMap<String, String> beaconMap = new HashMap<>();

            when(statsRecorder.newMeasureMap()).thenReturn(measureMap);
            doReturn(measureMap).when(measureMap).put(any(), anyDouble());

            selfMonitoringMetricManager.record("beacons_received", 1);

            ArgumentCaptor<View> viewCaptor = ArgumentCaptor.forClass(View.class);
            ArgumentCaptor<Measure.MeasureDouble> measureCaptor = ArgumentCaptor.forClass(Measure.MeasureDouble.class);
            ArgumentCaptor<Double> doubleCaptor = ArgumentCaptor.forClass(Double.class);
            verify(viewManager).getAllExportedViews();
            verify(viewManager).registerView(viewCaptor.capture());
            verify(statsRecorder).newMeasureMap();
            verify(measureMap).put(measureCaptor.capture(), doubleCaptor.capture());
            verify(measureMap).record();
            verifyNoMoreInteractions(viewManager, statsRecorder, measureMap);

            assertThat(viewCaptor.getValue().getName().asString()).isEqualTo("inspectit-eum/self/beacons_received/COUNT");
            assertThat(viewCaptor.getValue().getColumns()).extracting(TagKey::getName).containsExactly("TAG_1");
            assertThat(viewCaptor.getValue().getDescription()).isEqualTo("Dummy description");
            assertThat(measureCaptor.getValue().getName()).isEqualTo("inspectit-eum/self/beacons_received");
            assertThat(measureCaptor.getValue().getUnit()).isEqualTo("number");
            assertThat(measureCaptor.getValue().getDescription()).isEqualTo("Dummy description");
            assertThat(doubleCaptor.getValue()).isEqualTo(1);
        }
    }
}
