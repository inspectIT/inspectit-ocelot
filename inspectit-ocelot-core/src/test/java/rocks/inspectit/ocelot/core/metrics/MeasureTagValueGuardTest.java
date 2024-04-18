package rocks.inspectit.ocelot.core.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.metrics.TagGuardSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction.ExecutionContext;

import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.tagGuards.PersistedTagsReaderWriter;
import rocks.inspectit.ocelot.core.selfmonitoring.AgentHealthManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasureTagValueGuardTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitEnvironment environment;
    @Mock
    private CommonTagsManager commonTagsManager;
    @Mock
    PersistedTagsReaderWriter readerWriter;
    @Mock
    private AgentHealthManager agentHealthManager;
    @Mock
    private ScheduledExecutorService executor;
    @InjectMocks
    private MeasureTagValueGuard guard = new MeasureTagValueGuard();

    private ExecutionContext context;
    private final static int defaultMaxValuePerTag = 42;
    private final static String OVERFLOW = "overflow";

    /**
     * Helper method to configure tag value limits as well as metrics settings before testing
     * @param maxValuesPerTagByMeasure Map with measures and their tag value limits
     * @param settings MetricDefinitionSettings, which should be applied for "measure"
     */
    private void setupTagGuard(Map<String, Integer> maxValuesPerTagByMeasure, MetricDefinitionSettings settings) {
        TagGuardSettings tagGuardSettings = new TagGuardSettings();
        tagGuardSettings.setEnabled(true);
        tagGuardSettings.setScheduleDelay(Duration.ofSeconds(1));
        tagGuardSettings.setOverflowReplacement(OVERFLOW);
        tagGuardSettings.setMaxValuesPerTag(defaultMaxValuePerTag);
        if (maxValuesPerTagByMeasure != null)
            tagGuardSettings.setMaxValuesPerTagByMeasure(maxValuesPerTagByMeasure);

        when(environment.getCurrentConfig().getMetrics().getTagGuard()).thenReturn(tagGuardSettings);

        if (settings != null)
            when(environment.getCurrentConfig()
                    .getMetrics()
                    .getDefinitions()
                    .get("measure")).thenReturn(settings);
    }

    @Nested
    public class ReaderWrite {

        private String generateTempFilePath() {
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

        private Map<String, Map<String, Set<String>>> createTagValues() {
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

        @Test
        public void testReadWriteTagsFromDisk() {
            String tempFileName = generateTempFilePath();

            PersistedTagsReaderWriter readerWriter = PersistedTagsReaderWriter.of(tempFileName);
            Map<String, Map<String, Set<String>>> tagValues = createTagValues();
            readerWriter.write(tagValues);
            Map<String, Map<String, Set<String>>> loaded = readerWriter.read();

            assertThat(loaded).flatExtracting("measure_1")
                    .flatExtracting("tagKey_1")
                    .containsExactlyInAnyOrder("value1", "value2", "value3");

        }
    }

    @Nested
    public class getMaxValuesPerTag {

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

    @Nested
    public class getTagContext {

        static final String TAG_KEY = "test-tag-key";
        static final String TAG_VALUE_1 = "test-tag-value-1";
        static final String TAG_VALUE_2 = "test-tag-value-2";
        private MetricAccessor metricAccessor1;
        private MetricAccessor metricAccessor2;

        private ExecutionContext createExecutionContext() {
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), PropagationMetaData.builder().build(), false);
            return new ExecutionContext(null, this, "return", null, null,
                    ctx, null);
        }

        @BeforeEach
        void setUp() {
            VariableAccessor metricValueAccess = Mockito.mock(VariableAccessor.class);
            metricAccessor1 = new MetricAccessor("measure", metricValueAccess, Collections.emptyMap(),
                    Collections.singletonMap(TAG_KEY, (context) -> TAG_VALUE_1));
            metricAccessor2 = new MetricAccessor("measure", metricValueAccess, Collections.emptyMap(),
                    Collections.singletonMap(TAG_KEY, (context) -> TAG_VALUE_2));

            context = createExecutionContext();

            when(readerWriter.read()).thenReturn(new HashMap<>());
            when(commonTagsManager.getCommonTagKeys()).thenReturn(Collections.emptyList());
        }

        @Test
        void verifyOverflow() {
            Map<String, Integer> maxValuesPerTagByMeasure = new HashMap<>();
            maxValuesPerTagByMeasure.put("measure", 1);
            setupTagGuard(maxValuesPerTagByMeasure, null);

            TagContext expectedTagContext = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create(TAG_KEY), TagValue.create(TAG_VALUE_1))
                    .build();

            TagContext expectedOverflow = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create(TAG_KEY), TagValue.create(OVERFLOW))
                    .build();

            // first tag value should be accepted
            TagContext tagContext = guard.getTagContext(context, metricAccessor1);
            guard.blockTagValuesTask.run();
            // second tag value will exceed the limit
            TagContext overflow = guard.getTagContext(context, metricAccessor2);

            assertThat(tagContext.equals(expectedTagContext)).isTrue();
            assertThat(overflow.equals(expectedOverflow)).isTrue();
        }

        @Test
        void verifyOverflowResolvedAfterLimitIncrease() {
            Map<String, Integer> maxValuesPerTagByMeasure = new HashMap<>();
            maxValuesPerTagByMeasure.put("measure", 1);
            setupTagGuard(maxValuesPerTagByMeasure, null);

            TagContext expectedTagContext1 = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create(TAG_KEY), TagValue.create(TAG_VALUE_1))
                    .build();

            TagContext expectedTagContext2 = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create(TAG_KEY), TagValue.create(TAG_VALUE_2))
                    .build();

            TagContext expectedOverflow = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create(TAG_KEY), TagValue.create(OVERFLOW))
                    .build();

            // first tag value should be accepted
            TagContext tagContext1 = guard.getTagContext(context, metricAccessor1);
            guard.blockTagValuesTask.run();
            // second tag value will exceed the limit
            TagContext overflow = guard.getTagContext(context, metricAccessor2);
            // increase tag limit to resolve overflow
            maxValuesPerTagByMeasure.put("measure", 5);
            setupTagGuard(maxValuesPerTagByMeasure, null);
            guard.blockTagValuesTask.run();
            // second tag value should be accepted
            TagContext tagContext2 = guard.getTagContext(context, metricAccessor2);

            assertThat(tagContext1.equals(expectedTagContext1)).isTrue();
            assertThat(overflow.equals(expectedOverflow)).isTrue();
            assertThat(tagContext2.equals(expectedTagContext2)).isTrue();
        }

        @Test
        void verifyOverflowNotResolvedAfterLimitIncrease() {
            Map<String, Integer> maxValuesPerTagByMeasure = new HashMap<>();
            maxValuesPerTagByMeasure.put("measure", 1);
            setupTagGuard(maxValuesPerTagByMeasure, null);

            TagContext expectedTagContext1 = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create(TAG_KEY), TagValue.create(TAG_VALUE_1))
                    .build();

            TagContext expectedOverflow = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create(TAG_KEY), TagValue.create(OVERFLOW))
                    .build();

            // first tag value should be accepted
            TagContext tagContext1 = guard.getTagContext(context, metricAccessor1);
            guard.blockTagValuesTask.run();
            // second tag value will exceed the limit
            TagContext overflow1 = guard.getTagContext(context, metricAccessor2);
            // increase tag limit to resolve overflow
            maxValuesPerTagByMeasure.put("measure", 2);
            setupTagGuard(maxValuesPerTagByMeasure, null);
            guard.blockTagValuesTask.run();
            // second tag value should be accepted
            TagContext overflow2 = guard.getTagContext(context, metricAccessor2);

            assertThat(tagContext1.equals(expectedTagContext1)).isTrue();
            assertThat(overflow1.equals(expectedOverflow)).isTrue();
            assertThat(overflow2.equals(expectedOverflow)).isTrue();
        }
    }
}
