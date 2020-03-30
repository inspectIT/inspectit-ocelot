package rocks.inspectit.oce.eum.server.metrics;

import com.google.common.collect.ImmutableList;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.beacon.recorder.BeaconRecorder;
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
    MeasuresAndViewsManager measuresAndViewsManager;

    @Mock
    EumTagsSettings tagSettings;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    StatsRecorder statsRecorder;

    @Mock
    ViewManager viewManager;

    @Mock
    MeasureMap measureMap;

    @Spy
    List<BeaconRecorder> beaconRecorders = ImmutableList.of(
            mock(BeaconRecorder.class)
    );

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

        @BeforeEach
        public void setupMocks() {
            when(configuration.getTags()).thenReturn(tagSettings);
            when(tagSettings.getBeacon()).thenReturn(Collections.emptyMap());
            when(measuresAndViewsManager.getTagContext()).thenReturn(Tags.getTagger().emptyBuilder());
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
        void beaconRecordersProcessed() {
            when(configuration.getDefinitions()).thenReturn(definitionMap);
            HashMap<String, String> beaconMap = new HashMap<>();
            beaconMap.put("fake_beacon_field", "12d");
            Beacon beacon = Beacon.of(beaconMap);

            beaconMetricManager.processBeacon(beacon);

            assertThat(beaconRecorders).allSatisfy(beaconRecorder -> {
                verify(beaconRecorder).record(beacon);
                verifyNoMoreInteractions(beaconRecorder);
            });
            verifyZeroInteractions(viewManager, statsRecorder);
        }

    }
}
