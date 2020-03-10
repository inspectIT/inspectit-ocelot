package rocks.inspectit.ocelot.core.metrics.jmx;

import io.opencensus.stats.Measure;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.tags.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.jmx.JmxMetricsRecorderSettings;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JmxMetricsRecorderTest {

    JmxMetricsRecorder jmxMetricsRecorder;

    @Mock
    Tagger tagger;

    @Mock
    MeasuresAndViewsManager measuresManager;

    @Mock
    StatsRecorder statsRecorder;

    @Mock
    CommonTagsManager commonTagsManager;

    @BeforeEach
    public void initMocks() {
        this.jmxMetricsRecorder = new JmxMetricsRecorder(tagger, measuresManager, statsRecorder, commonTagsManager);
    }

    @Nested
    class RecordBean {

        @Mock
        Measure.MeasureDouble measureDoubleMock;

        @Mock
        MeasureMap measureMap;

        @Captor
        ArgumentCaptor<MetricDefinitionSettings> definitionCaptor;

        @Test
        public void valueBasic() {
            TagContextBuilder tagContextBuilder = Tags.getTagger().emptyBuilder();
            double value = 1.2565;
            String expectedMeasureName = "jvm/jmx/my/domain/att";
            when(measuresManager.getMeasureDouble(expectedMeasureName)).thenReturn(Optional.of(measureDoubleMock));
            when(statsRecorder.newMeasureMap()).thenReturn(measureMap);
            when(tagger.currentBuilder()).thenReturn(tagContextBuilder);

            jmxMetricsRecorder.recordBean("my.domain", new LinkedHashMap<>(), new LinkedList<>(), "att", null, "desc", value);

            TagContext tagContext = tagContextBuilder.build();
            assertThat(InternalUtils.getTags(tagContext)).isEmpty();

            verify(measuresManager).getMeasureDouble(expectedMeasureName);
            verify(statsRecorder).newMeasureMap();
            verify(tagger).currentBuilder();
            verify(measureMap).put(measureDoubleMock, value);
            verify(measureMap).record(tagContext);
            verifyNoMoreInteractions(measuresManager, statsRecorder, commonTagsManager, tagger);
        }

        @Test
        public void valueBasicBooleanTrue() {
            TagContextBuilder tagContextBuilder = Tags.getTagger().emptyBuilder();
            String expectedMeasureName = "jvm/jmx/my/domain/attbool";
            when(measuresManager.getMeasureDouble(expectedMeasureName)).thenReturn(Optional.of(measureDoubleMock));
            when(statsRecorder.newMeasureMap()).thenReturn(measureMap);
            when(tagger.currentBuilder()).thenReturn(tagContextBuilder);

            jmxMetricsRecorder.recordBean("my.domain", new LinkedHashMap<>(), new LinkedList<>(), "attbool", null, "desc", Boolean.TRUE);

            TagContext tagContext = tagContextBuilder.build();
            assertThat(InternalUtils.getTags(tagContext)).isEmpty();

            verify(measuresManager).getMeasureDouble(expectedMeasureName);
            verify(statsRecorder).newMeasureMap();
            verify(tagger).currentBuilder();
            verify(measureMap).put(measureDoubleMock, 1d);
            verify(measureMap).record(tagContext);
            verifyNoMoreInteractions(measuresManager, statsRecorder, commonTagsManager, tagger);
        }

        @Test
        public void valueBasicBooleanFalse() {
            TagContextBuilder tagContextBuilder = Tags.getTagger().emptyBuilder();
            String expectedMeasureName = "jvm/jmx/my/domain/attbool";
            when(measuresManager.getMeasureDouble(expectedMeasureName)).thenReturn(Optional.of(measureDoubleMock));
            when(statsRecorder.newMeasureMap()).thenReturn(measureMap);
            when(tagger.currentBuilder()).thenReturn(tagContextBuilder);

            jmxMetricsRecorder.recordBean("my.domain", new LinkedHashMap<>(), new LinkedList<>(), "attbool", null, "desc", Boolean.FALSE);

            TagContext tagContext = tagContextBuilder.build();
            assertThat(InternalUtils.getTags(tagContext)).isEmpty();

            verify(measuresManager).getMeasureDouble(expectedMeasureName);
            verify(statsRecorder).newMeasureMap();
            verify(tagger).currentBuilder();
            verify(measureMap).put(measureDoubleMock, 0d);
            verify(measureMap).record(tagContext);
            verifyNoMoreInteractions(measuresManager, statsRecorder, commonTagsManager, tagger);
        }

        @Test
        public void valueBasicMeasureDoesNotExists() {
            TagContextBuilder tagContextBuilder = Tags.getTagger().emptyBuilder();
            double value = 1.2565;
            String expectedMeasureName = "jvm/jmx/my/domain/att";
            when(measuresManager.getMeasureDouble(expectedMeasureName)).thenReturn(Optional.empty()).thenReturn(Optional.of(measureDoubleMock));
            when(statsRecorder.newMeasureMap()).thenReturn(measureMap);
            when(tagger.currentBuilder()).thenReturn(tagContextBuilder);

            jmxMetricsRecorder.recordBean("my.domain", new LinkedHashMap<>(), new LinkedList<>(), "att", null, "desc", value);

            TagContext tagContext = tagContextBuilder.build();
            assertThat(InternalUtils.getTags(tagContext)).isEmpty();

            verify(measuresManager, times(2)).getMeasureDouble(expectedMeasureName);
            verify(measuresManager).addOrUpdateAndCacheMeasureWithViews(eq(expectedMeasureName), definitionCaptor.capture());
            verify(statsRecorder).newMeasureMap();
            verify(tagger).currentBuilder();
            verify(measureMap).put(measureDoubleMock, value);
            verify(measureMap).record(tagContext);
            verifyNoMoreInteractions(measuresManager, statsRecorder, commonTagsManager, tagger);

            assertThat(definitionCaptor.getValue()).satisfies(metricDefinitionSettings -> {
                assertThat(metricDefinitionSettings.getDescription()).isEqualTo("desc");
                assertThat(metricDefinitionSettings.getUnit()).isEqualTo("na");
                assertThat(metricDefinitionSettings.getType()).isEqualTo(MetricDefinitionSettings.MeasureType.DOUBLE);
                assertThat(metricDefinitionSettings.getViews().values()).hasOnlyOneElementSatisfying(view -> {
                    assertThat(view.getAggregation()).isEqualTo(ViewDefinitionSettings.Aggregation.LAST_VALUE);
                    assertThat(view.getTags()).isEmpty();
                    assertThat(view.getDescription()).isNotBlank();
                });
            });
        }

        @Test
        public void valueComplex() {
            TagContextBuilder tagContextBuilder = Tags.getTagger().emptyBuilder();
            double value = 1.2565;
            LinkedList<String> attributes = new LinkedList<>(Arrays.asList("key1", "key2"));
            LinkedHashMap<String, String> beanProps = new LinkedHashMap<>();
            beanProps.put("prop1", "Prop1Value");
            beanProps.put("prop2", "Prop2Value");
            beanProps.put("prop3", "Prop3Value");
            String expectedMeasureName = "jvm/jmx/my/domain/Prop1Value/key1/key2/att";
            when(measuresManager.getMeasureDouble(expectedMeasureName)).thenReturn(Optional.of(measureDoubleMock));
            when(statsRecorder.newMeasureMap()).thenReturn(measureMap);
            when(tagger.currentBuilder()).thenReturn(tagContextBuilder);

            jmxMetricsRecorder.recordBean("my.domain", beanProps, attributes, "att", null, "desc", value);

            TagContext tagContext = tagContextBuilder.build();
            assertThat(InternalUtils.getTags(tagContext)).hasSize(2)
                    .anySatisfy(tag -> {
                        assertThat(tag.getKey().getName()).isEqualTo("prop2");
                        assertThat(tag.getValue().asString()).isEqualTo("Prop2Value");
                    })
                    .anySatisfy(tag -> {
                        assertThat(tag.getKey().getName()).isEqualTo("prop3");
                        assertThat(tag.getValue().asString()).isEqualTo("Prop3Value");
                    });

            verify(measuresManager).getMeasureDouble(expectedMeasureName);
            verify(statsRecorder).newMeasureMap();
            verify(tagger).currentBuilder();
            verify(measureMap).put(measureDoubleMock, value);
            verify(measureMap).record(tagContext);
            verifyNoMoreInteractions(measuresManager, statsRecorder, commonTagsManager, tagger);
        }

        @Test
        public void valueNegative() {
            jmxMetricsRecorder.recordBean(null, null, null, null, null, null, -1d);

            verifyNoMoreInteractions(measuresManager, statsRecorder, commonTagsManager);
        }

        @Test
        public void valueNotNumber() {
            jmxMetricsRecorder.recordBean(null, null, null, null, null, null, "something");

            verifyNoMoreInteractions(measuresManager, statsRecorder, commonTagsManager);
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
        public void init() {
            when(settings.isForcePlatformServer()).thenReturn(true);
        }

        @Test
        public void noObjectNames() throws Exception {
            JmxScraper scraper = JmxMetricsRecorder.createScraper(settings, receiver);
            scraper.doScrape();

            verify(receiver, atLeastOnce()).recordBean(notNull(), notNull(), notNull(), notNull(), notNull(), notNull(), notNull());
        }

        @Test
        public void whiteListOnly() throws Exception {
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
        public void blackListOnly() throws Exception {
            when(settings.getObjectNames()).thenReturn(Collections.singletonMap("java.lang:type=ClassLoading", false));

            JmxScraper scraper = JmxMetricsRecorder.createScraper(settings, receiver);
            scraper.doScrape();

            verify(receiver, atLeastOnce()).recordBean(eq("java.lang"), beanPropsCaptor.capture(), notNull(), notNull(), notNull(), notNull(), notNull());
            assertThat(beanPropsCaptor.getAllValues()).allSatisfy(map -> assertThat(map).doesNotContainEntry("type", "ClassLoading"));
        }

        @Test
        public void mixedLists() throws Exception {
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