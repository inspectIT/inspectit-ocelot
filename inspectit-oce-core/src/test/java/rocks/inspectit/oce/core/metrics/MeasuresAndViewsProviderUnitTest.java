package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagKey;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import java.util.Arrays;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeasuresAndViewsProviderUnitTest {

    @Mock
    ViewManager viewManager;

    @Mock
    CommonTagsManager commonTags;

    @Mock
    Aggregation aggregation;

    private final Supplier<Aggregation> aggregationSupplier = () -> aggregation;

    private final TagKey[] testKeys = new TagKey[]{TagKey.create("keyA"), TagKey.create("keyB")};
    private final TagKey[] commonKeys = new TagKey[]{TagKey.create("commonA"), TagKey.create("commonB")};
    private final TagKey[] allKeys = ArrayUtils.addAll(testKeys, commonKeys);

    @InjectMocks
    MeasuresAndViewsProvider provider = new MeasuresAndViewsProvider();

    @Nested
    class GetOrCreateMeasureDoubleWithView {

        @Test
        void testNewViewCreation() {
            when(viewManager.getView(any())).thenReturn(null);

            Measure.MeasureDouble result = provider.getOrCreateMeasureDoubleWithView("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
            assertThat(result.getDescription()).isEqualTo("description");
            assertThat(result.getUnit()).isEqualTo("meter");

            ArgumentCaptor<View> viewArg = ArgumentCaptor.forClass(View.class);
            verify(viewManager, times(1)).registerView(viewArg.capture());
            View view = viewArg.getValue();

            assertThat(view.getMeasure()).isSameAs(result);
            assertThat(view.getName().asString()).isEqualTo("test");
            assertThat(view.getDescription()).isEqualTo("description [meter]");
            assertThat(view.getAggregation()).isSameAs(aggregation);
            assertThat(view.getColumns()).containsExactlyInAnyOrder(testKeys);

        }

        @Test
        void testViewAlreadyExists() {
            when(viewManager.getView(any())).thenReturn(Mockito.mock(ViewData.class));

            Measure.MeasureDouble result = provider.getOrCreateMeasureDoubleWithView("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
            assertThat(result.getDescription()).isEqualTo("description");
            assertThat(result.getUnit()).isEqualTo("meter");

            verify(viewManager, never()).registerView(any());

        }


        @Test
        void testViewAndMetricCaching() {
            when(viewManager.getView(any())).thenReturn(null);

            Measure.MeasureDouble resultA = provider.getOrCreateMeasureDoubleWithView("test", "description", "meter", aggregationSupplier, testKeys);
            Measure.MeasureDouble resultB = provider.getOrCreateMeasureDoubleWithView("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(resultB).isSameAs(resultA);

            verify(viewManager, times(1)).registerView(any());
            verify(viewManager, times(1)).getView(any());

        }

    }

    @Nested
    class GetOrCreateMeasureDoubleWithViewAndCommonTags {

        @Test
        void testNewViewCreation() {
            when(commonTags.getCommonTagKeys()).thenReturn(Arrays.asList(commonKeys));
            when(viewManager.getView(any())).thenReturn(null);

            Measure.MeasureDouble result = provider.getOrCreateMeasureDoubleWithViewAndCommonTags("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
            assertThat(result.getDescription()).isEqualTo("description");
            assertThat(result.getUnit()).isEqualTo("meter");

            ArgumentCaptor<View> viewArg = ArgumentCaptor.forClass(View.class);
            verify(viewManager, times(1)).registerView(viewArg.capture());
            View view = viewArg.getValue();

            assertThat(view.getMeasure()).isSameAs(result);
            assertThat(view.getName().asString()).isEqualTo("test");
            assertThat(view.getDescription()).isEqualTo("description [meter]");
            assertThat(view.getAggregation()).isSameAs(aggregation);
            assertThat(view.getColumns()).containsExactlyInAnyOrder(allKeys);

        }

        @Test
        void testViewAlreadyExists() {
            when(commonTags.getCommonTagKeys()).thenReturn(Arrays.asList(commonKeys));
            when(viewManager.getView(any())).thenReturn(Mockito.mock(ViewData.class));

            Measure.MeasureDouble result = provider.getOrCreateMeasureDoubleWithViewAndCommonTags("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
            assertThat(result.getDescription()).isEqualTo("description");
            assertThat(result.getUnit()).isEqualTo("meter");

            verify(viewManager, never()).registerView(any());

        }


        @Test
        void testViewAndMetricCaching() {
            when(commonTags.getCommonTagKeys()).thenReturn(Arrays.asList(commonKeys));
            when(viewManager.getView(any())).thenReturn(null);

            Measure.MeasureDouble resultA = provider.getOrCreateMeasureDoubleWithViewAndCommonTags("test", "description", "meter", aggregationSupplier, testKeys);
            Measure.MeasureDouble resultB = provider.getOrCreateMeasureDoubleWithViewAndCommonTags("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(resultB).isSameAs(resultA);

            verify(viewManager, times(1)).registerView(any());
            verify(viewManager, times(1)).getView(any());

        }

    }

    @Nested
    class GetOrCreateMeasureLongWithView {

        @Test
        void testNewViewCreation() {
            when(viewManager.getView(any())).thenReturn(null);

            Measure.MeasureLong result = provider.getOrCreateMeasureLongWithView("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
            assertThat(result.getDescription()).isEqualTo("description");
            assertThat(result.getUnit()).isEqualTo("meter");

            ArgumentCaptor<View> viewArg = ArgumentCaptor.forClass(View.class);
            verify(viewManager, times(1)).registerView(viewArg.capture());
            View view = viewArg.getValue();

            assertThat(view.getMeasure()).isSameAs(result);
            assertThat(view.getName().asString()).isEqualTo("test");
            assertThat(view.getDescription()).isEqualTo("description [meter]");
            assertThat(view.getAggregation()).isSameAs(aggregation);
            assertThat(view.getColumns()).containsExactlyInAnyOrder(testKeys);

        }

        @Test
        void testViewAlreadyExists() {
            when(viewManager.getView(any())).thenReturn(Mockito.mock(ViewData.class));

            Measure.MeasureLong result = provider.getOrCreateMeasureLongWithView("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
            assertThat(result.getDescription()).isEqualTo("description");
            assertThat(result.getUnit()).isEqualTo("meter");

            verify(viewManager, never()).registerView(any());

        }


        @Test
        void testViewAndMetricCaching() {
            when(viewManager.getView(any())).thenReturn(null);

            Measure.MeasureLong resultA = provider.getOrCreateMeasureLongWithView("test", "description", "meter", aggregationSupplier, testKeys);
            Measure.MeasureLong resultB = provider.getOrCreateMeasureLongWithView("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(resultB).isSameAs(resultA);

            verify(viewManager, times(1)).registerView(any());
            verify(viewManager, times(1)).getView(any());

        }

    }

    @Nested
    class GetOrCreateMeasureLongWithViewAndCommonTags {

        @Test
        void testNewViewCreation() {
            when(commonTags.getCommonTagKeys()).thenReturn(Arrays.asList(commonKeys));
            when(viewManager.getView(any())).thenReturn(null);

            Measure.MeasureLong result = provider.getOrCreateMeasureLongWithViewAndCommonTags("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
            assertThat(result.getDescription()).isEqualTo("description");
            assertThat(result.getUnit()).isEqualTo("meter");

            ArgumentCaptor<View> viewArg = ArgumentCaptor.forClass(View.class);
            verify(viewManager, times(1)).registerView(viewArg.capture());
            View view = viewArg.getValue();

            assertThat(view.getMeasure()).isSameAs(result);
            assertThat(view.getName().asString()).isEqualTo("test");
            assertThat(view.getDescription()).isEqualTo("description [meter]");
            assertThat(view.getAggregation()).isSameAs(aggregation);
            assertThat(view.getColumns()).containsExactlyInAnyOrder(allKeys);

        }

        @Test
        void testViewAlreadyExists() {
            when(commonTags.getCommonTagKeys()).thenReturn(Arrays.asList(commonKeys));
            when(viewManager.getView(any())).thenReturn(Mockito.mock(ViewData.class));

            Measure.MeasureLong result = provider.getOrCreateMeasureLongWithViewAndCommonTags("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
            assertThat(result.getDescription()).isEqualTo("description");
            assertThat(result.getUnit()).isEqualTo("meter");

            verify(viewManager, never()).registerView(any());

        }


        @Test
        void testViewAndMetricCaching() {
            when(commonTags.getCommonTagKeys()).thenReturn(Arrays.asList(commonKeys));
            when(viewManager.getView(any())).thenReturn(null);

            Measure.MeasureLong resultA = provider.getOrCreateMeasureLongWithViewAndCommonTags("test", "description", "meter", aggregationSupplier, testKeys);
            Measure.MeasureLong resultB = provider.getOrCreateMeasureLongWithViewAndCommonTags("test", "description", "meter", aggregationSupplier, testKeys);

            assertThat(resultB).isSameAs(resultA);

            verify(viewManager, times(1)).registerView(any());
            verify(viewManager, times(1)).getView(any());

        }

    }

}
