package rocks.inspectit.oce.eum.server.metrics;

import io.opencensus.stats.Stats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.eum.server.model.config.BeaconMetricDefinition;
import rocks.inspectit.oce.eum.server.model.config.Configuration;
import rocks.inspectit.oce.eum.server.model.config.EUMTagsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests {@link MeasuresAndViewsManager}
 */
@ExtendWith(MockitoExtension.class)
public class MeasuresAndViewsManagerTest {


    @Mock
    Configuration configuration = new Configuration();

    @InjectMocks
    MeasuresAndViewsManager measuresAndViewsManager = new MeasuresAndViewsManager();

    Map<String, BeaconMetricDefinition> definitionMap;

    @Mock
    EUMTagsSettings tagSettings;

    @Nested
    class ProcessBeacon{
        @BeforeEach
        void setupConfiguration(){
            ViewDefinitionSettings view1 = ViewDefinitionSettings.builder().bucketBoundaries(Arrays.asList(0d, 1d))
                    .aggregation(ViewDefinitionSettings.Aggregation.HISTOGRAM)
                    .tag("TAG_1", true)
                    .tag("TAG_2",true)
                    .build();
            ViewDefinitionSettings view2 = ViewDefinitionSettings.builder()
                    .aggregation(ViewDefinitionSettings.Aggregation.COUNT)
                    .tag("TAG_1", true)
                    .tag("TAG_2",true)
                    .build();
            ViewDefinitionSettings view3 = ViewDefinitionSettings.builder()
                    .aggregation(ViewDefinitionSettings.Aggregation.SUM)
                    .tag("TAG_1", true)
                    .tag("TAG_2",true)
                    .build();
            Map<String, ViewDefinitionSettings> views = new HashMap<String, ViewDefinitionSettings>();
            views.put("Dummy metric name/HISTOGRAM", view1);
            views.put("Dummy metric name/COUNT", view2);
            views.put("Dummy metric name/SUM", view3);

            BeaconMetricDefinition dummyMetricDefinition = BeaconMetricDefinition.beaconMetricBuilder().beaconField("dummy_beacon_field").description("Dummy description").type(rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings.MeasureType.DOUBLE).unit("ms").enabled(true).views(views).build();

            Map<String, String> extraTags = new HashMap<>();
            extraTags.put("TAG_1", "tag_value_1");
            extraTags.put("TAG_2", "tag_value_2");

            Map<String, String> beaconTags = new HashMap<>();
            beaconTags.put("URL", "u");

            Set<String> globalTags = new HashSet<>();
            globalTags.add("URL");

            when(tagSettings.getExtra()).thenReturn(extraTags);
            when(tagSettings.getBeacon()).thenReturn(beaconTags);
            when(tagSettings.getGlobal()).thenReturn(globalTags);

            definitionMap = new HashMap<>();
            definitionMap.put("Dummy metric name", dummyMetricDefinition);
        }

        @Test
        void verifyNoViewIsGeneratedWithEmptyBeacon(){
            when(configuration.getDefinitions()).thenReturn(definitionMap);
            HashMap<String, String> emptyBeacon =  new HashMap<>();
            measuresAndViewsManager.processBeacon(emptyBeacon);

            assertThat(Stats.getViewManager().getAllExportedViews()).isEmpty();
        }

        @Test
        void verifyNoViewIsGeneratedWithFullBeacon(){
            when(configuration.getDefinitions()).thenReturn(definitionMap);
            HashMap<String, String> beacon =  new HashMap<String, String>();
            beacon.put("fake_ beacon_field", "12d");
            measuresAndViewsManager.processBeacon(beacon);

            assertThat(Stats.getViewManager().getAllExportedViews()).isEmpty();
        }

        @Test
        void verifyViewsAreGeneratedGlobalTagIsSet(){
            when(configuration.getDefinitions()).thenReturn(definitionMap);
            when(configuration.getTags()).thenReturn(tagSettings);
            HashMap<String, String> beacon =  new HashMap<String, String>();
            beacon.put("dummy_beacon_field", "12d");
            measuresAndViewsManager.processBeacon(beacon);

            assertThat(Stats.getViewManager().getAllExportedViews()).hasSize(3);
            assertThat(Stats.getViewManager().getAllExportedViews()).allMatch(view -> view.getMeasure().getName().equals("Dummy metric name"));
            assertThat(Stats.getViewManager().getAllExportedViews()).anyMatch(view -> view.getColumns().size() == 3);
        }
    }
}
