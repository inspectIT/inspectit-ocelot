package rocks.inspectit.ocelot.core.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.TagGuardSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasureTagValueGuardTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitEnvironment environment;

    @Mock
    private InspectitConfig inspectitConfig;

    @Mock
    private MetricsSettings metricsSettings;

    @InjectMocks
    private MeasureTagValueGuard guard = new MeasureTagValueGuard();

    private Map<String, Map<String, Set<String>>> tagValues;

    private static Map<String, Map<String, Set<String>>> createTagValues() {
        Set<String> tagValue = new HashSet<>();
        tagValue.add("value1");
        tagValue.add("value2");
        tagValue.add("value3");

        Map<String, Set<String>> tagKeys2Values = Maps.newHashMap();
        tagKeys2Values.put("tagKey_1", tagValue);

        Map<String, Map<String, Set<String>>> measure2TagKeys = Maps.newHashMap();
        measure2TagKeys.put("measure_1", tagKeys2Values);

        return measure2TagKeys;
    }

    private static String generateTempFilePath() {
        try {
            Path tempFile = Files.createTempFile("inspectit", "");
            System.out.println(tempFile);
            Files.delete(tempFile);
            tempFile.toFile().deleteOnExit();
            return tempFile.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    public class ReaderWrite {

        @Test
        public void testReadWriteTagsFromDisk() {

            String tempFileName = generateTempFilePath();

            //when(environment.getCurrentConfig()).thenReturn(inspectitConfig);
            //when(inspectitConfig.getMetrics()).thenReturn(metricsSettings);
            //when(metricsSettings.getTagGuardScheduleDelay()).thenReturn(Duration.of(1, ChronoUnit.MILLIS));
            //when(metricsSettings.getTagGuardDatabaseFile()).thenReturn(generateTempFilePath());

            MeasureTagValueGuard.PersistedTagsReaderWriter readerWriter = new MeasureTagValueGuard.PersistedTagsReaderWriter(tempFileName, new ObjectMapper());
            Map<String, Map<String, Set<String>>> tagValues = createTagValues();
            readerWriter.write(tagValues);
            Map<String, Map<String, Set<String>>> loaded = readerWriter.read();

            //Assertions.assertThat(tagValues).isEqualTo(loaded);
            assertThat(loaded).flatExtracting("measure_1")
                    .flatExtracting("tagKey_1")
                    .containsExactlyInAnyOrder("value1", "value2", "value3");

        }
    }

    @Nested
    public class getMaxValuesPerTag {

        final private static int defaultMaxValuePerTag = 42;

        private void setupTagGuard(Map<String, Integer> maxValuesPerTagByMeasure, MetricDefinitionSettings settings) {
            TagGuardSettings tagGuardSettings = new TagGuardSettings();
            tagGuardSettings.setEnabled(true);
            tagGuardSettings.setMaxValuesPerTag(defaultMaxValuePerTag);
            if (maxValuesPerTagByMeasure != null) {
                tagGuardSettings.setMaxValuesPerTagByMeasure(maxValuesPerTagByMeasure);
            }

            when(environment.getCurrentConfig().getMetrics().getTagGuard()).thenReturn(tagGuardSettings);
            if (settings != null) {
                when(environment.getCurrentConfig()
                        .getMetrics()
                        .getDefinitions()
                        .get("measure")).thenReturn(settings);
            }
        }

        @Test
        public void getMaxValuesPerTagByDefault() {
            setupTagGuard(null, null);

            assertThat(guard.getMaxValuesPerTag("measure", environment.getCurrentConfig())).isEqualTo(defaultMaxValuePerTag);
        }

        @Test
        public void getMaxValuesPerTagByMeasure() {
            Map<String, Integer> maxValuesPerTagByMeasure = new HashMap<>();
            maxValuesPerTagByMeasure.put("measure", 43);
            setupTagGuard(maxValuesPerTagByMeasure, null);

            assertThat(guard.getMaxValuesPerTag("measure", environment.getCurrentConfig())).isEqualTo(43);
            assertThat(guard.getMaxValuesPerTag("measure1", environment.getCurrentConfig())).isEqualTo(defaultMaxValuePerTag);
        }

        @Test
        public void getMaxValuesPerTagByMetricDefinitionSettings() {
            MetricDefinitionSettings settings = new MetricDefinitionSettings();
            settings.setMaxValuesPerTag(43);
            setupTagGuard(null, settings);

            assertThat(guard.getMaxValuesPerTag("measure", environment.getCurrentConfig())).isEqualTo(43);
            assertThat(guard.getMaxValuesPerTag("measure1", environment.getCurrentConfig())).isEqualTo(defaultMaxValuePerTag);
        }

        @Test
        public void getMaxValuesPerTagWhenAllSettingsAreSet() {
            Map<String, Integer> maxValuesPerTagByMeasure = new HashMap<>();
            maxValuesPerTagByMeasure.put("measure", 43);
            maxValuesPerTagByMeasure.put("measure2", 48);

            MetricDefinitionSettings settings = new MetricDefinitionSettings();
            settings.setMaxValuesPerTag(44);

            setupTagGuard(maxValuesPerTagByMeasure, settings);

            assertThat(guard.getMaxValuesPerTag("measure", environment.getCurrentConfig())).isEqualTo(44);
            assertThat(guard.getMaxValuesPerTag("measure2", environment.getCurrentConfig())).isEqualTo(48);
            assertThat(guard.getMaxValuesPerTag("measure3", environment.getCurrentConfig())).isEqualTo(defaultMaxValuePerTag);
        }
    }

}