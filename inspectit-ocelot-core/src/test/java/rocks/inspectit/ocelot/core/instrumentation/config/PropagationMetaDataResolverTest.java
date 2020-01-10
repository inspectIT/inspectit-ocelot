package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.data.DataSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PropagationMetaDataResolverTest {

    @Mock
    PropagationMetaData.Builder mockBuilder;

    @Mock
    CommonTagsManager commonTags;

    @InjectMocks
    PropagationMetaDataResolver resolver;

    @BeforeEach
    void setupBuilderMock() {
        lenient().doReturn(mockBuilder).when(mockBuilder).setDownPropagation(any(), any());
        lenient().doReturn(mockBuilder).when(mockBuilder).setUpPropagation(any(), any());
        lenient().doReturn(mockBuilder).when(mockBuilder).setTag(any(), anyBoolean());
    }

    @Nested
    class CollectForCommonTags {

        @Test
        void commonTagPropagationCorrect() {
            doReturn(Collections.singletonMap("my_key", "foo")).when(commonTags).getCommonTagValueMap();

            resolver.collectCommonTags(mockBuilder);

            verify(mockBuilder).setDownPropagation(eq("my_key"), eq(PropagationMode.JVM_LOCAL));
            verify(mockBuilder).setTag(eq("my_key"), eq(true));
            verifyNoMoreInteractions(mockBuilder);
        }
    }


    @Nested
    class CollectTagsFromMetricDefinitions {

        @Test
        void commonTagPropagationCorrect() {
            MetricDefinitionSettings def = new MetricDefinitionSettings();
            ViewDefinitionSettings defView = new ViewDefinitionSettings();
            defView.setTags(ImmutableMap.of("my_key", true, "removed_tag", false));
            def.setViews(Collections.singletonMap("view", defView));

            resolver.collectTagsFromMetricDefinitions(Collections.singletonMap("metric", def), mockBuilder);

            verify(mockBuilder).setTag(eq("my_key"), eq(true));
            verifyNoMoreInteractions(mockBuilder);
        }
    }


    @Nested
    class CollectTagsFromUserSettings {

        @Test
        void tagConfigured() {
            DataSettings settings = new DataSettings();
            settings.setIsTag(true);

            resolver.collectUserSettings(Collections.singletonMap("my_key", settings), mockBuilder);

            verify(mockBuilder).setTag(eq("my_key"), eq(true));
            verifyNoMoreInteractions(mockBuilder);
        }

        @Test
        void tagRemoved() {
            DataSettings settings = new DataSettings();
            settings.setIsTag(false);

            resolver.collectUserSettings(Collections.singletonMap("my_key", settings), mockBuilder);

            verify(mockBuilder).setTag(eq("my_key"), eq(false));
            verifyNoMoreInteractions(mockBuilder);
        }

        @Test
        void downPropagationConfigured() {
            DataSettings settings = new DataSettings();
            settings.setDownPropagation(PropagationMode.JVM_LOCAL);

            resolver.collectUserSettings(Collections.singletonMap("my_key", settings), mockBuilder);

            verify(mockBuilder).setDownPropagation(eq("my_key"), eq(PropagationMode.JVM_LOCAL));
            verifyNoMoreInteractions(mockBuilder);
        }

        @Test
        void upPropagationConfigured() {
            DataSettings settings = new DataSettings();
            settings.setUpPropagation(PropagationMode.GLOBAL);

            resolver.collectUserSettings(Collections.singletonMap("my_key", settings), mockBuilder);

            verify(mockBuilder).setUpPropagation(eq("my_key"), eq(PropagationMode.GLOBAL));
            verifyNoMoreInteractions(mockBuilder);
        }

        @Test
        void allConfigured() {
            DataSettings settings = new DataSettings();
            settings.setIsTag(true);
            settings.setDownPropagation(PropagationMode.GLOBAL);
            settings.setUpPropagation(PropagationMode.NONE);

            resolver.collectUserSettings(Collections.singletonMap("my_key", settings), mockBuilder);

            verify(mockBuilder).setTag(eq("my_key"), eq(true));
            verify(mockBuilder).setDownPropagation(eq("my_key"), eq(PropagationMode.GLOBAL));
            verify(mockBuilder).setUpPropagation(eq("my_key"), eq(PropagationMode.NONE));
            verifyNoMoreInteractions(mockBuilder);
        }
    }

    @Nested
    class Resolve {

        @Test
        void ensureUserSettingsRespected() {
            InspectitConfig config = new InspectitConfig();
            InstrumentationSettings instr = new InstrumentationSettings();
            MetricsSettings metricsSettings = new MetricsSettings();
            config.setInstrumentation(instr);
            config.setMetrics(metricsSettings);

            DataSettings data = new DataSettings();
            data.setUpPropagation(PropagationMode.JVM_LOCAL);
            data.setIsTag(true);
            instr.setData(Collections.singletonMap("user_key", data));

            doReturn(Collections.emptyMap()).when(commonTags).getCommonTagValueMap();

            PropagationMetaData result = resolver.resolve(config);

            assertThat(result.isTag("user_key")).isTrue();
            assertThat(result.isPropagatedUpWithinJVM("user_key")).isTrue();
            assertThat(result.isPropagatedUpGlobally("user_key")).isFalse();
            assertThat(result.isPropagatedDownWithinJVM("user_key")).isFalse();
            assertThat(result.isPropagatedDownGlobally("user_key")).isFalse();
        }

        @Test
        void ensureCommonTagsRespected() {
            InspectitConfig config = new InspectitConfig();
            InstrumentationSettings instr = new InstrumentationSettings();
            MetricsSettings metricsSettings = new MetricsSettings();
            config.setInstrumentation(instr);
            config.setMetrics(metricsSettings);

            doReturn(Collections.singletonMap("common_key", "value")).when(commonTags).getCommonTagValueMap();

            PropagationMetaData result = resolver.resolve(config);

            assertThat(result.isTag("common_key")).isTrue();
            assertThat(result.isPropagatedUpWithinJVM("common_key")).isFalse();
            assertThat(result.isPropagatedUpGlobally("common_key")).isFalse();
            assertThat(result.isPropagatedDownWithinJVM("common_key")).isTrue();
            assertThat(result.isPropagatedDownGlobally("common_key")).isFalse();
        }


        @Test
        void ensureTagsFromMetricsRespected() {
            InspectitConfig config = new InspectitConfig();
            InstrumentationSettings instr = new InstrumentationSettings();
            MetricsSettings metricsSettings = new MetricsSettings();
            config.setInstrumentation(instr);
            config.setMetrics(metricsSettings);

            doReturn(Collections.emptyMap()).when(commonTags).getCommonTagValueMap();

            MetricDefinitionSettings def = new MetricDefinitionSettings();
            ViewDefinitionSettings defView = new ViewDefinitionSettings();
            defView.setTags(ImmutableMap.of("metric_key", true));
            def.setViews(Collections.singletonMap("view", defView));
            metricsSettings.setDefinitions(Collections.singletonMap("my_metric", def));

            PropagationMetaData result = resolver.resolve(config);

            assertThat(result.isTag("metric_key")).isTrue();
            assertThat(result.isPropagatedUpWithinJVM("metric_key")).isFalse();
            assertThat(result.isPropagatedUpGlobally("metric_key")).isFalse();
            assertThat(result.isPropagatedDownWithinJVM("metric_key")).isFalse();
            assertThat(result.isPropagatedDownGlobally("metric_key")).isFalse();
        }

        @Test
        void ensureUserOverridesHaveHighestPriority() {
            InspectitConfig config = new InspectitConfig();
            InstrumentationSettings instr = new InstrumentationSettings();
            MetricsSettings metricsSettings = new MetricsSettings();
            config.setInstrumentation(instr);
            config.setMetrics(metricsSettings);

            doReturn(Collections.singletonMap("common_key", "value")).when(commonTags).getCommonTagValueMap();

            MetricDefinitionSettings def = new MetricDefinitionSettings();
            ViewDefinitionSettings defView = new ViewDefinitionSettings();
            defView.setTags(ImmutableMap.of("metric_key", true));
            def.setViews(Collections.singletonMap("view", defView));
            metricsSettings.setDefinitions(Collections.singletonMap("my_metric", def));

            DataSettings metricKey = new DataSettings();
            metricKey.setUpPropagation(PropagationMode.JVM_LOCAL);
            metricKey.setIsTag(false);
            DataSettings commonKey = new DataSettings();
            commonKey.setDownPropagation(PropagationMode.NONE);
            commonKey.setUpPropagation(PropagationMode.GLOBAL);
            instr.setData(ImmutableMap.of("metric_key", metricKey, "common_key", commonKey));

            PropagationMetaData result = resolver.resolve(config);

            assertThat(result.isTag("metric_key")).isFalse();
            assertThat(result.isPropagatedUpWithinJVM("metric_key")).isTrue();
            assertThat(result.isPropagatedUpGlobally("metric_key")).isFalse();
            assertThat(result.isPropagatedDownWithinJVM("metric_key")).isFalse();
            assertThat(result.isPropagatedDownGlobally("metric_key")).isFalse();

            assertThat(result.isTag("common_key")).isTrue();
            assertThat(result.isPropagatedUpWithinJVM("common_key")).isTrue();
            assertThat(result.isPropagatedUpGlobally("common_key")).isTrue();
            assertThat(result.isPropagatedDownWithinJVM("common_key")).isFalse();
            assertThat(result.isPropagatedDownGlobally("common_key")).isFalse();
        }

    }
}
