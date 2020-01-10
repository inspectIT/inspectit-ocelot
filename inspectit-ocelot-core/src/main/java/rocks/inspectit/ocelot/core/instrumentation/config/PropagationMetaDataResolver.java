package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.data.DataSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.Map;

@Component
public class PropagationMetaDataResolver {

    @Autowired
    private CommonTagsManager commonTags;

    /**
     * Configures the {@link PropagationMetaData} based on all sources of settings in the given configuration.
     *
     * @param config the configuration to extract the settings from
     * @return the resulting meta information about data keys
     */
    public PropagationMetaData resolve(InspectitConfig config) {
        PropagationMetaData.Builder builder = PropagationMetaData.builder();

        collectCommonTags(builder);
        collectTagsFromMetricDefinitions(config.getMetrics().getDefinitions(), builder);
        collectUserSettings(config.getInstrumentation().getData(), builder);

        return builder.build();
    }

    @VisibleForTesting
    void collectCommonTags(PropagationMetaData.Builder builder) {
        commonTags.getCommonTagValueMap()
                .keySet()
                .forEach(key -> builder
                        .setTag(key, true)
                        .setDownPropagation(key, PropagationMode.JVM_LOCAL));
    }

    @VisibleForTesting
    void collectTagsFromMetricDefinitions(Map<String, MetricDefinitionSettings> definitions, PropagationMetaData.Builder builder) {
        definitions.values()
                .stream()
                .filter(definition -> definition.getViews() != null)
                .flatMap(definition -> definition.getViews().values().stream())
                .filter(view -> view.getTags() != null)
                .flatMap(view -> view.getTags()
                        .entrySet()
                        .stream()
                        .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                        .map(Map.Entry::getKey)
                )
                .forEach(key -> builder.setTag(key, true));

    }

    @VisibleForTesting
    void collectUserSettings(Map<String, DataSettings> dataSettings, PropagationMetaData.Builder builder) {
        dataSettings.forEach((key, settings) -> {
            if (settings.getIsTag() != null) {
                builder.setTag(key, settings.getIsTag());
            }
            if (settings.getDownPropagation() != null) {
                builder.setDownPropagation(key, settings.getDownPropagation());
            }
            if (settings.getUpPropagation() != null) {
                builder.setUpPropagation(key, settings.getUpPropagation());
            }
        });
    }
}
