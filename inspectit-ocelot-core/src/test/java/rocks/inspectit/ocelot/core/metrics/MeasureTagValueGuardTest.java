package rocks.inspectit.ocelot.core.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasureTagValueGuardTest {

    @Mock
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
}