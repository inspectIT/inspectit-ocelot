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
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconMetricDefinitionSettings;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.configuration.model.EumTagsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests {@link BeaconMetricManager}
 */
@ExtendWith(MockitoExtension.class)
public class BeaconMetricManagerTest {

    @InjectMocks
    BeaconMetricManager beaconMetricManager;

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
    class ProcessBeacon {

        private Map<String, BeaconMetricDefinitionSettings> definitionMap;

        @BeforeEach
        void setupConfiguration() {
            ViewDefinitionSettings view = ViewDefinitionSettings.builder().bucketBoundaries(Arrays.asList(0d, 1d))
                    .aggregation(ViewDefinitionSettings.Aggregation.HISTOGRAM)
                    .tag("TAG_1", true)
                    .tag("TAG_2", true)
                    .build();
            Map<String, ViewDefinitionSettings> views = new HashMap<>();
            views.put("Dummy metric name/HISTOGRAM", view);

            BeaconMetricDefinitionSettings dummyMetricDefinition = BeaconMetricDefinitionSettings
                    .beaconMetricBuilder()
                    .valueExpression("{dummy_beacon_field}")
                    .description("Dummy description")
                    .type(rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings.MeasureType.DOUBLE)
                    .unit("ms")
                    .enabled(true)
                    .views(views)
                    .build();

            definitionMap = new HashMap<>();
            definitionMap.put("Dummy metric name", dummyMetricDefinition);
        }

        @Test
        void verifyNoViewIsGeneratedWithEmptyBeacon() {
            when(configuration.getDefinitions()).thenReturn(definitionMap);
            HashMap<String, String> beaconMap = new HashMap<>();

            beaconMetricManager.processBeacon(Beacon.of(beaconMap));

            verifyZeroInteractions(viewManager, statsRecorder);
        }

        @Test
        void verifyNoViewIsGeneratedWithFullBeacon() {
            when(configuration.getDefinitions()).thenReturn(definitionMap);
            HashMap<String, String> beaconMap = new HashMap<>();
            beaconMap.put("fake_beacon_field", "12d");

            beaconMetricManager.processBeacon(Beacon.of(beaconMap));

            verifyZeroInteractions(viewManager, statsRecorder);
        }

        @Test
        void verifyViewsAreGeneratedGlobalTagIsSet() {
            Map<String, String> extraTags = new HashMap<>();
            extraTags.put("TAG_1", "tag_value_1");
            extraTags.put("TAG_2", "tag_value_2");

            Map<String, String> beaconTags = new HashMap<>();
            beaconTags.put("URL", "http://localhost");

            Set<String> globalTags = new HashSet<>();
            globalTags.add("URL");

            when(tagSettings.getExtra()).thenReturn(extraTags);
            when(tagSettings.getBeacon()).thenReturn(beaconTags);
            when(tagSettings.getDefineAsGlobal()).thenReturn(globalTags);

            when(configuration.getDefinitions()).thenReturn(definitionMap);
            when(configuration.getTags()).thenReturn(tagSettings);
            HashMap<String, String> beaconMap = new HashMap<>();
            beaconMap.put("dummy_beacon_field", "12d");

            when(statsRecorder.newMeasureMap()).thenReturn(measureMap);
            doReturn(measureMap).when(measureMap).put(any(), anyDouble());

            beaconMetricManager.processBeacon(Beacon.of(beaconMap));

            ArgumentCaptor<View> viewCaptor = ArgumentCaptor.forClass(View.class);
            ArgumentCaptor<Measure.MeasureDouble> measureCaptor = ArgumentCaptor.forClass(Measure.MeasureDouble.class);
            ArgumentCaptor<Double> doubleCaptor = ArgumentCaptor.forClass(Double.class);
            verify(viewManager).getAllExportedViews();
            verify(viewManager).registerView(viewCaptor.capture());
            verify(statsRecorder).newMeasureMap();
            verify(measureMap).put(measureCaptor.capture(), doubleCaptor.capture());
            verify(measureMap).record();
            verifyNoMoreInteractions(viewManager, statsRecorder, measureMap);

            assertThat(viewCaptor.getValue().getName().asString()).isEqualTo("Dummy metric name/HISTOGRAM");
            assertThat(viewCaptor.getValue().getColumns()).extracting(TagKey::getName).containsExactly("TAG_1", "TAG_2", "URL");
            assertThat(viewCaptor.getValue().getDescription()).isEqualTo("Dummy description");
            assertThat(measureCaptor.getValue().getName()).isEqualTo("Dummy metric name");
            assertThat(measureCaptor.getValue().getUnit()).isEqualTo("ms");
            assertThat(measureCaptor.getValue().getDescription()).isEqualTo("Dummy description");
            assertThat(doubleCaptor.getValue()).isEqualTo(12D);
        }
    }
}
