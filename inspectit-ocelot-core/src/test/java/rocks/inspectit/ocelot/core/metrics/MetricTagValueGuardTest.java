package rocks.inspectit.ocelot.core.metrics;

import com.google.common.collect.Maps;
import io.opentelemetry.api.baggage.Baggage;
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
import rocks.inspectit.ocelot.core.instrumentation.context.session.PropagationSessionStorage;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction.ExecutionContext;

import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.tagGuard.PersistedAttributesReaderWriter;
import rocks.inspectit.ocelot.core.selfmonitoring.AgentHealthManager;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricTagValueGuardTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitEnvironment environment;

    @Mock
    CommonAttributesManager commonAttributesManager;

    @Mock
    PersistedAttributesReaderWriter readerWriter;

    @Mock
    AgentHealthManager agentHealthManager;

    @Mock
    ScheduledExecutorService executor;

    @InjectMocks
    MetricTagValueGuard tagGuard;

    ExecutionContext context;

    final static int defaultMaxValuePerAttribute = 42;

    final static String OVERFLOW = "overflow";

    /**
     * Helper method to configure tag value limits as well as metrics settings before testing.
     *
     * @param maxValuesPerAttributeByMetric Map with metrics and their attribute value limits
     * @param settings MetricDefinitionSettings, which should be applied for "metric"
     */
    private void setupTagGuard(Map<String, Integer> maxValuesPerAttributeByMetric, MetricDefinitionSettings settings) {
        TagGuardSettings tagGuardSettings = new TagGuardSettings();
        tagGuardSettings.setEnabled(true);
        tagGuardSettings.setScheduleDelay(Duration.ofSeconds(1));
        tagGuardSettings.setOverflowReplacement(OVERFLOW);
        tagGuardSettings.setMaxValuesPerAttribute(defaultMaxValuePerAttribute);
        if (maxValuesPerAttributeByMetric != null)
            tagGuardSettings.setMaxValuesPerAttributeByMetric(maxValuesPerAttributeByMetric);

        when(environment.getCurrentConfig().getMetrics().getTagGuard()).thenReturn(tagGuardSettings);

        if (settings != null)
            when(environment.getCurrentConfig()
                    .getMetrics()
                    .getDefinitions()
                    .get("metric")).thenReturn(settings);
    }

    @Nested
    class ReaderWrite {

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

        private Map<String, Map<String, Set<String>>> createMetricAttributeValues() {
            Set<String> attributeValues = new HashSet<>();
            attributeValues.add("value1");
            attributeValues.add("value2");
            attributeValues.add("value3");

            Map<String, Set<String>> attributes = Maps.newHashMap();
            attributes.put("attrKey_1", attributeValues);

            Map<String, Map<String, Set<String>>> metrics = Maps.newHashMap();
            metrics.put("metric_1", attributes);

            return metrics;
        }

        @Test
        void testReadWriteTagsFromDisk() {
            String tempFileName = generateTempFilePath();

            PersistedAttributesReaderWriter readerWriter = PersistedAttributesReaderWriter.of(tempFileName);
            Map<String, Map<String, Set<String>>> attributes = createMetricAttributeValues();
            readerWriter.write(attributes);
            Map<String, Map<String, Set<String>>> loaded = readerWriter.read();

            assertThat(loaded).flatExtracting("metric_1")
                    .flatExtracting("attrKey_1")
                    .containsExactlyInAnyOrder("value1", "value2", "value3");

        }
    }

    @Nested
    class getMaxValuesPerTag {

        @Test
        void getMaxValuesPerTagByDefault() {
            setupTagGuard(null, null);

            assertThat(tagGuard.getMaxValuesPerAttribute("metric", environment.getCurrentConfig())).isEqualTo(defaultMaxValuePerAttribute);
        }

        @Test
        void getMaxValuesPerTagByMeasure() {
            Map<String, Integer> maxValuesPerTagByMeasure = new HashMap<>();
            maxValuesPerTagByMeasure.put("metric", 43);
            setupTagGuard(maxValuesPerTagByMeasure, null);

            assertThat(tagGuard.getMaxValuesPerAttribute("metric", environment.getCurrentConfig())).isEqualTo(43);
            assertThat(tagGuard.getMaxValuesPerAttribute("metric1", environment.getCurrentConfig())).isEqualTo(defaultMaxValuePerAttribute);
        }

        @Test
        void getMaxValuesPerTagByMetricDefinitionSettings() {
            MetricDefinitionSettings settings = new MetricDefinitionSettings();
            settings.setMaxValuesPerAttribute(43);
            setupTagGuard(null, settings);

            assertThat(tagGuard.getMaxValuesPerAttribute("metric", environment.getCurrentConfig())).isEqualTo(43);
            assertThat(tagGuard.getMaxValuesPerAttribute("metric1", environment.getCurrentConfig())).isEqualTo(defaultMaxValuePerAttribute);
        }

        @Test
        void getMaxValuesPerTagWhenAllSettingsAreSet() {
            Map<String, Integer> maxValuesPerTagByMeasure = new HashMap<>();
            maxValuesPerTagByMeasure.put("metric", 43);
            maxValuesPerTagByMeasure.put("metric2", 48);

            MetricDefinitionSettings settings = new MetricDefinitionSettings();
            settings.setMaxValuesPerAttribute(44);

            setupTagGuard(maxValuesPerTagByMeasure, settings);

            assertThat(tagGuard.getMaxValuesPerAttribute("metric", environment.getCurrentConfig())).isEqualTo(44);
            assertThat(tagGuard.getMaxValuesPerAttribute("metric2", environment.getCurrentConfig())).isEqualTo(48);
            assertThat(tagGuard.getMaxValuesPerAttribute("metric3", environment.getCurrentConfig())).isEqualTo(defaultMaxValuePerAttribute);
        }
    }

    @Nested
    class getBaggage {

        static final String ATTRIBUTE_KEY = "test-key";
        static final String ATTRIBUTE_VALUE_1 = "test-value-1";
        static final String ATTRIBUTE_VALUE_2 = "test-value-2";

        MetricAccessor metricAccessor1;
        MetricAccessor metricAccessor2;

        private ExecutionContext createExecutionContext() {
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), PropagationMetaData.builder().build(),
                    new PropagationSessionStorage(), false);
            return new ExecutionContext(null, this, "return", null, null,
                    ctx, null);
        }

        @BeforeEach
        void setUp() {
            VariableAccessor metricValueAccess = Mockito.mock(VariableAccessor.class);
            metricAccessor1 = new MetricAccessor("metric", metricValueAccess, Collections.emptyMap(),
                    Collections.singletonMap(ATTRIBUTE_KEY, (context) -> ATTRIBUTE_VALUE_1));
            metricAccessor2 = new MetricAccessor("metric", metricValueAccess, Collections.emptyMap(),
                    Collections.singletonMap(ATTRIBUTE_KEY, (context) -> ATTRIBUTE_VALUE_2));

            context = createExecutionContext();

            when(readerWriter.read()).thenReturn(new HashMap<>());
            when(commonAttributesManager.getCommonAttributeKeys()).thenReturn(Collections.emptyList());
        }

        @Test
        void verifyOverflow() {
            Map<String, Integer> maxValuesPerTagByMeasure = new HashMap<>();
            maxValuesPerTagByMeasure.put("metric", 1);
            setupTagGuard(maxValuesPerTagByMeasure, null);

            Baggage expectedBaggage = Baggage.builder()
                    .put(ATTRIBUTE_KEY, ATTRIBUTE_VALUE_1)
                    .build();

            Baggage expectedOverflow = Baggage.builder()
                    .put(ATTRIBUTE_KEY, OVERFLOW)
                    .build();

            // first attribute value should be accepted
            Baggage baggage = tagGuard.getBaggage(context, metricAccessor1);

            tagGuard.blockAttributeValuesTask.run();

            // second attribute value will exceed the limit
            Baggage overflow = tagGuard.getBaggage(context, metricAccessor2);

            assertThat(baggage.equals(expectedBaggage)).isTrue();
            assertThat(overflow.equals(expectedOverflow)).isTrue();
        }

        @Test
        void verifyOverflowResolvedAfterLimitIncrease() {
            Map<String, Integer> maxValuesPerAttributeByMetric = new HashMap<>();
            maxValuesPerAttributeByMetric.put("metric", 1);
            setupTagGuard(maxValuesPerAttributeByMetric, null);

            Baggage expectedBaggage1 = Baggage.builder()
                    .put(ATTRIBUTE_KEY, ATTRIBUTE_VALUE_1)
                    .build();

            Baggage expectedBaggage2 = Baggage.builder()
                    .put(ATTRIBUTE_KEY, ATTRIBUTE_VALUE_2)
                    .build();

            Baggage expectedOverflow = Baggage.builder()
                    .put(ATTRIBUTE_KEY, OVERFLOW)
                    .build();

            // first tag value should be accepted
            Baggage baggage1 = tagGuard.getBaggage(context, metricAccessor1);
            tagGuard.blockAttributeValuesTask.run();

            // second tag value will exceed the limit
            Baggage overflow = tagGuard.getBaggage(context, metricAccessor2);

            // increase tag limit to resolve overflow
            maxValuesPerAttributeByMetric.put("metric", 5);
            setupTagGuard(maxValuesPerAttributeByMetric, null);
            tagGuard.blockAttributeValuesTask.run();

            // second tag value should be accepted
            Baggage baggage2 = tagGuard.getBaggage(context, metricAccessor2);

            assertThat(baggage1.equals(expectedBaggage1)).isTrue();
            assertThat(overflow.equals(expectedOverflow)).isTrue();
            assertThat(baggage2.equals(expectedBaggage2)).isTrue();
        }

        @Test
        void verifyOverflowNotResolvedAfterLimitIncrease() {
            Map<String, Integer> maxValuesPerAttributeByMetric = new HashMap<>();
            maxValuesPerAttributeByMetric.put("metric", 1);
            setupTagGuard(maxValuesPerAttributeByMetric, null);

            Baggage expectedBaggage1 = Baggage.builder()
                    .put(ATTRIBUTE_KEY, ATTRIBUTE_VALUE_1)
                    .build();

            Baggage expectedOverflow = Baggage.builder()
                    .put(ATTRIBUTE_KEY, OVERFLOW)
                    .build();

            // first tag value should be accepted
            Baggage baggage1 = tagGuard.getBaggage(context, metricAccessor1);
            tagGuard.blockAttributeValuesTask.run();

            // second tag value will exceed the limit
            Baggage overflow1 = tagGuard.getBaggage(context, metricAccessor2);

            // increase tag limit to resolve overflow
            maxValuesPerAttributeByMetric.put("metric", 2);
            setupTagGuard(maxValuesPerAttributeByMetric, null);
            tagGuard.blockAttributeValuesTask.run();

            // second tag value should be accepted
            Baggage overflow2 = tagGuard.getBaggage(context, metricAccessor2);

            assertThat(baggage1.equals(expectedBaggage1)).isTrue();
            assertThat(overflow1.equals(expectedOverflow)).isTrue();
            assertThat(overflow2.equals(expectedOverflow)).isTrue();
        }
    }
}
