package rocks.inspectit.ocelot.core.metrics.jmx;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.InstrumentValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.AggregationType;
import rocks.inspectit.ocelot.config.model.metrics.jmx.JmxMetricsRecorderSettings;
import rocks.inspectit.ocelot.core.metrics.InstrumentManager;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JmxMetricsRecorderTest {

    @InjectMocks
    JmxMetricsRecorder jmxMetricsRecorder;

    @Mock
    InstrumentManager instrumentManager;

    @Mock
    CommonAttributesManager commonAttributesManager;

    @Nested
    class RecordBean {

        @Captor
        ArgumentCaptor<Map<String, MetricDefinitionSettings>> definitionsCaptor;

        @Test
        void valueBasic() {
            double value = 1.2565;
            String expectedMetricName = "jvm_jmx_my_domain_att";
            when(instrumentManager.isInstrumentRegistered(expectedMetricName)).thenReturn(true);

            jmxMetricsRecorder.recordBean("my.domain", new LinkedHashMap<>(), new LinkedList<>(), "att", null, "desc", value);

            verify(instrumentManager).isInstrumentRegistered(expectedMetricName);
            verify(instrumentManager).tryRecordingMetric(expectedMetricName, value, Baggage.empty());
            verifyNoMoreInteractions(instrumentManager, commonAttributesManager);
        }

        @Test
        void valueBasicBooleanTrue() {
            String expectedMetricName = "jvm_jmx_my_domain_attbool";
            when(instrumentManager.isInstrumentRegistered(expectedMetricName)).thenReturn(true);

            jmxMetricsRecorder.recordBean("my.domain", new LinkedHashMap<>(), new LinkedList<>(), "attbool", null, "desc", Boolean.TRUE);

            verify(instrumentManager).tryRecordingMetric(expectedMetricName, 1d, Baggage.empty());
            verifyNoMoreInteractions(instrumentManager, commonAttributesManager);
        }

        @Test
        void valueBasicBooleanFalse() {
            String expectedMetricName = "jvm_jmx_my_domain_attbool";
            when(instrumentManager.isInstrumentRegistered(expectedMetricName)).thenReturn(true);

            jmxMetricsRecorder.recordBean("my.domain", new LinkedHashMap<>(), new LinkedList<>(), "attbool", null, "desc", Boolean.FALSE);

            verify(instrumentManager).tryRecordingMetric(expectedMetricName, 0d, Baggage.empty());
            verifyNoMoreInteractions(instrumentManager, commonAttributesManager);
        }

        @Test
        void valueBasicMeasureDoesNotExists() {
            double value = 1.2565;
            String expectedMetricName = "jvm_jmx_my_domain_att";
            when(instrumentManager.isInstrumentRegistered(expectedMetricName)).thenReturn(false);

            jmxMetricsRecorder.recordBean("my.domain", new LinkedHashMap<>(), new LinkedList<>(), "att", null, "desc", value);

            verify(instrumentManager).processInstrumentUpdates(definitionsCaptor.capture());
            verify(instrumentManager).tryRecordingMetric(expectedMetricName, value, Baggage.empty());
            verifyNoMoreInteractions(instrumentManager, commonAttributesManager);

            assertThat(definitionsCaptor.getValue().values()).anySatisfy(def -> {
                assertThat(def.getDescription()).isEqualTo("desc");
                assertThat(def.getUnit()).isEqualTo("na");
                assertThat(def.getInstrumentType()).isEqualTo(InstrumentType.GAUGE);
                assertThat(def.getValueType()).isEqualTo(InstrumentValueType.LONG);
                assertThat(def.getViews().values()).hasOnlyOneElementSatisfying(view -> {
                    assertThat(view.getAggregation()).isEqualTo(AggregationType.LAST_VALUE);
                    assertThat(view.getAttributes()).isEmpty();
                    assertThat(view.getDescription()).isNotBlank();
                });
            });
        }

        @Test
        void valueComplex() {
            double value = 1.2565;
            LinkedList<String> attributes = new LinkedList<>(Arrays.asList("key1", "key2"));
            LinkedHashMap<String, String> beanProps = new LinkedHashMap<>();
            beanProps.put("prop1", "Prop1Value");
            beanProps.put("prop2", "Prop2Value");
            beanProps.put("prop3", "Prop3Value");
            String expectedMetricName = "jvm_jmx_my_domain_Prop1Value_key1_key2_att";
            when(instrumentManager.isInstrumentRegistered(expectedMetricName)).thenReturn(true);

            jmxMetricsRecorder.recordBean("my.domain", beanProps, attributes, "att", null, "desc", value);

            Baggage expected = Baggage.builder()
                    .put("prop2", "Prop2Value")
                    .put("prop3", "Prop3Value")
                    .build();
            verify(instrumentManager).tryRecordingMetric(expectedMetricName, value, expected);
            verifyNoMoreInteractions(instrumentManager, commonAttributesManager);
        }

        @Test
        void valueNegative() {
            jmxMetricsRecorder.recordBean(null, null, null, null, null, null, -1d);

            verifyNoMoreInteractions(instrumentManager, commonAttributesManager);
        }

        @Test
        void valueNotNumber() {
            jmxMetricsRecorder.recordBean(null, null, null, null, null, null, "something");

            verifyNoMoreInteractions(instrumentManager, commonAttributesManager);
        }

    }

    @Nested
    class CreateScraper {

        @Mock
        JmxMetricsRecorderSettings settings;

        @Mock
        JmxScraper.MBeanReceiver receiver;

        @Captor
        ArgumentCaptor<LinkedHashMap<String, String>> beanPropsCaptor;

        @BeforeEach
        void init() {
            when(settings.isForcePlatformServer()).thenReturn(true);
        }

        @Test
        void noObjectNames() {
            JmxScraper scraper = JmxMetricsRecorder.createScraper(settings, receiver);
            scraper.doScrape();

            verify(receiver, atLeastOnce()).recordBean(notNull(), notNull(), notNull(), notNull(), notNull(), notNull(), notNull());
        }

        @Test
        void whiteListOnly() {
            when(settings.getObjectNames()).thenReturn(Collections.singletonMap("java.lang:type=ClassLoading", true));

            JmxScraper scraper = JmxMetricsRecorder.createScraper(settings, receiver);
            scraper.doScrape();

            verify(receiver, atLeastOnce()).recordBean(eq("java.lang"), beanPropsCaptor.capture(), notNull(), notNull(), notNull(), notNull(), notNull());
            assertThat(beanPropsCaptor.getAllValues()).allSatisfy(map -> assertThat(map)
                    .hasSize(1)
                    .containsEntry("type", "ClassLoading")
            );
        }

        @Test
        void blackListOnly() {
            when(settings.getObjectNames()).thenReturn(Collections.singletonMap("java.lang:type=ClassLoading", false));

            JmxScraper scraper = JmxMetricsRecorder.createScraper(settings, receiver);
            scraper.doScrape();

            verify(receiver, atLeastOnce()).recordBean(eq("java.lang"), beanPropsCaptor.capture(), notNull(), notNull(), notNull(), notNull(), notNull());
            assertThat(beanPropsCaptor.getAllValues()).allSatisfy(map -> assertThat(map).doesNotContainEntry("type", "ClassLoading"));
        }

        @Test
        void mixedLists() {
            Map<String, Boolean> objectNames = new HashMap<>();
            objectNames.put("java.lang:*", true);
            objectNames.put("java.lang:type=Runtime,*", false);
            when(settings.getObjectNames()).thenReturn(objectNames);

            JmxScraper scraper = JmxMetricsRecorder.createScraper(settings, receiver);
            scraper.doScrape();

            verify(receiver, atLeastOnce()).recordBean(eq("java.lang"), beanPropsCaptor.capture(), notNull(), notNull(), notNull(), notNull(), notNull());
            assertThat(beanPropsCaptor.getAllValues()).allSatisfy(map -> assertThat(map)
                    .doesNotContainEntry("type", "Runtime")
            );
        }
    }
}
