package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagKey;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.oce.core.config.model.metrics.definition.ViewDefinitionSettings;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeasuresAndViewsManagerTest {

    @Mock
    ViewManager viewManager;

    @Mock
    CommonTagsManager commonTagsManager;

    @Mock
    Aggregation aggregation;

    private final Supplier<Aggregation> aggregationSupplier = () -> aggregation;

    private final TagKey[] commonTags = {TagKey.create("common-A"), TagKey.create("common-B")};


    @InjectMocks
    MeasuresAndViewsManager manager = new MeasuresAndViewsManager();


    @Nested
    class AddOrUpdateAndCacheMeasureWithViews {

        @Test
        void testMetricWithDefaultsAndDefaultViewCreation() {
            when(commonTagsManager.getCommonTagKeys()).thenReturn(Arrays.asList(commonTags));

            String metricName = "my-metric";
            MetricDefinitionSettings metricDefinition = MetricDefinitionSettings.builder()
                    .unit("my-unit")
                    .build()
                    .getCopyWithDefaultsPopulated(metricName);

            manager.addOrUpdateAndCacheMeasureWithViews(metricDefinition, emptyMap(), emptyMap());

            Measure resultMeasure = manager.getMeasure(metricName).get();
            assertThat(resultMeasure.getName()).isEqualTo(metricName);
            assertThat(resultMeasure.getDescription()).isEqualTo(metricName);
            assertThat(resultMeasure).isInstanceOf(Measure.MeasureDouble.class);

            ArgumentCaptor<View> viewArg = ArgumentCaptor.forClass(View.class);
            verify(viewManager, times(1)).registerView(viewArg.capture());
            View view = viewArg.getValue();

            assertThat(view.getMeasure()).isSameAs(resultMeasure);
            assertThat(view.getName().asString()).isEqualTo(metricName);
            assertThat(view.getDescription()).isNotEmpty();
            assertThat(view.getAggregation()).isInstanceOf(Aggregation.LastValue.class);
            assertThat(view.getColumns()).containsExactlyInAnyOrder(commonTags);

        }


        @Test
        void testMetricWithDefaultsAndCustomViewCreation() {
            when(commonTagsManager.getCommonTagKeys()).thenReturn(Arrays.asList(commonTags));

            String metricName = "my-metric";
            MetricDefinitionSettings metricDefinition = MetricDefinitionSettings.builder()
                    .unit("my-unit")
                    .view("custom-view", ViewDefinitionSettings.builder()
                            .tag("my-tag", true)
                            .description("Cool view")
                            .aggregation(ViewDefinitionSettings.Aggregation.HISTOGRAM)
                            .bucketBoundaries(Arrays.asList(7.0, 42.0))
                            .build())
                    .build()
                    .getCopyWithDefaultsPopulated(metricName);

            manager.addOrUpdateAndCacheMeasureWithViews(metricDefinition, emptyMap(), emptyMap());

            Measure resultMeasure = manager.getMeasure(metricName).get();
            assertThat(resultMeasure.getName()).isEqualTo(metricName);
            assertThat(resultMeasure.getDescription()).isEqualTo(metricName);
            assertThat(resultMeasure).isInstanceOf(Measure.MeasureDouble.class);

            ArgumentCaptor<View> viewArg = ArgumentCaptor.forClass(View.class);
            verify(viewManager, times(1)).registerView(viewArg.capture());
            View view = viewArg.getValue();

            assertThat(view.getMeasure()).isSameAs(resultMeasure);
            assertThat(view.getName().asString()).isEqualTo("custom-view");
            assertThat(view.getDescription()).isEqualTo("Cool view");
            assertThat(view.getAggregation()).isInstanceOf(Aggregation.Distribution.class);
            assertThat(((Aggregation.Distribution) view.getAggregation()).getBucketBoundaries().getBoundaries())
                    .containsExactly(7.0, 42.0);
            assertThat(view.getColumns()).hasSize(3);
            assertThat(view.getColumns()).contains(commonTags);
            assertThat(view.getColumns()).contains(TagKey.create("my-tag"));

        }

        @Test
        void testCommonTagsCanBeDisabled() {
            lenient().when(commonTagsManager.getCommonTagKeys()).thenReturn(Arrays.asList(commonTags));
            String metricName = "my-metric";
            MetricDefinitionSettings metricDefinition = MetricDefinitionSettings.builder()
                    .unit("my-unit")
                    .view("my-unit", ViewDefinitionSettings.builder()
                            .withCommonTags(false)
                            .tag("my-tag", true)
                            .tag("disabled-tag", false)
                            .build())
                    .build()
                    .getCopyWithDefaultsPopulated(metricName);

            manager.addOrUpdateAndCacheMeasureWithViews(metricDefinition, emptyMap(), emptyMap());

            ArgumentCaptor<View> viewArg = ArgumentCaptor.forClass(View.class);
            verify(viewManager, times(1)).registerView(viewArg.capture());
            View view = viewArg.getValue();

            assertThat(view.getColumns()).containsExactly(TagKey.create("my-tag"));
        }

        @Test
        void testCommonTagsCanBeOverridden() {
            when(commonTagsManager.getCommonTagKeys()).thenReturn(Arrays.asList(commonTags));
            String metricName = "my-metric";
            MetricDefinitionSettings metricDefinition = MetricDefinitionSettings.builder()
                    .unit("my-unit")
                    .view("my-unit", ViewDefinitionSettings.builder()
                            .tag("my-tag", true)
                            .tag("common-A", false)
                            .build())
                    .build()
                    .getCopyWithDefaultsPopulated(metricName);

            manager.addOrUpdateAndCacheMeasureWithViews(metricDefinition, emptyMap(), emptyMap());

            ArgumentCaptor<View> viewArg = ArgumentCaptor.forClass(View.class);
            verify(viewManager, times(1)).registerView(viewArg.capture());
            View view = viewArg.getValue();

            assertThat(view.getColumns()).containsExactlyInAnyOrder(TagKey.create("my-tag"), TagKey.create("common-B"));
        }


        @Test
        void testDefaultViewCanBeDisabled() {
            String metricName = "my-metric";
            MetricDefinitionSettings metricDefinition = MetricDefinitionSettings.builder()
                    .unit("my-unit")
                    .view(metricName, ViewDefinitionSettings.builder().enabled(false).build())
                    .build()
                    .getCopyWithDefaultsPopulated(metricName);

            manager.addOrUpdateAndCacheMeasureWithViews(metricDefinition, emptyMap(), emptyMap());

            verify(viewManager, never()).registerView(any());
            assertThat(manager.getMeasure(metricName)).isNotEmpty();
        }


        @Test
        void testExistingViewsAndMeasuresNotReRegistered() {
            when(commonTagsManager.getCommonTagKeys()).thenReturn(Arrays.asList(commonTags));

            String metricName = "my-metric";
            Measure existingMeasure = Measure.MeasureLong.create(metricName, "abc", "def");
            View existingView = View.create(View.Name.create("existing"), "description", existingMeasure, Aggregation.LastValue.create(), Collections.emptyList());

            MetricDefinitionSettings metricDefinition = MetricDefinitionSettings.builder()
                    .unit("my-unit")
                    .view("existing", ViewDefinitionSettings.builder().build())
                    .view("newView", ViewDefinitionSettings.builder().build())
                    .build()
                    .getCopyWithDefaultsPopulated(metricName);

            manager.addOrUpdateAndCacheMeasureWithViews(metricDefinition, Maps.newHashMap(metricName, existingMeasure), Maps.newHashMap("existing", existingView));

            assertThat(manager.getMeasure(metricName)).contains(existingMeasure);

            ArgumentCaptor<View> viewArg = ArgumentCaptor.forClass(View.class);
            verify(viewManager, times(1)).registerView(viewArg.capture());
            View view = viewArg.getValue();

            assertThat(view.getMeasure()).isSameAs(existingMeasure);
            assertThat(view.getName().asString()).isEqualTo("newView");
            assertThat(view.getDescription()).contains(metricName);
            assertThat(view.getAggregation()).isInstanceOf(Aggregation.LastValue.class);
            assertThat(view.getColumns()).containsExactlyInAnyOrder(commonTags);
        }

        @Test
        void testExceptionsDuringViewRegistrationHandled() {
            doThrow(new RuntimeException()).when(viewManager).registerView(argThat(v -> v.getName().asString().equals("viewB")));
            doNothing().when(viewManager).registerView(argThat(v -> !v.getName().asString().equals("viewB")));
            String metricName = "my-metric";
            MetricDefinitionSettings metricDefinition = MetricDefinitionSettings.builder()
                    .unit("my-unit")
                    .view("viewA", ViewDefinitionSettings.builder().build())
                    .view("viewB", ViewDefinitionSettings.builder().build())
                    .view("viewC", ViewDefinitionSettings.builder().build())
                    .build()
                    .getCopyWithDefaultsPopulated(metricName);

            manager.addOrUpdateAndCacheMeasureWithViews(metricDefinition, emptyMap(), emptyMap());

            ArgumentCaptor<View> viewArg = ArgumentCaptor.forClass(View.class);
            verify(viewManager, times(3)).registerView(viewArg.capture());

            List<String> names = viewArg.getAllValues().stream()
                    .map(v -> v.getName().asString())
                    .collect(Collectors.toList());
            assertThat(names).containsExactlyInAnyOrder("viewA", "viewB", "viewC");

        }
    }

}
