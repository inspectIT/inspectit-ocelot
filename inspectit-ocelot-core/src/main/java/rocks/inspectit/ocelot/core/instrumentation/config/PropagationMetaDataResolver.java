package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.data.DataSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;

import java.util.Map;

/**
 * Constructs a {@link PropagationMetaData} instance based on the configuration ({@link InspectitConfig}).
 */
@Component
public class PropagationMetaDataResolver {

    @Autowired
    private CommonAttributesManager commonAttributes;

    /**
     * Configures the {@link PropagationMetaData} based on all sources of settings in the given configuration.
     *
     * @param config the configuration to extract the settings from
     * @return the resulting meta information about data keys
     */
    public PropagationMetaData resolve(InspectitConfig config) {
        PropagationMetaData.Builder builder = PropagationMetaData.builder();

        collectCommonAttributes(builder);
        collectUserSettings(config.getInstrumentation().getData(), builder);

        return builder.build();
    }

    @VisibleForTesting
    void collectCommonAttributes(PropagationMetaData.Builder builder) {
        commonAttributes.getCommonAttributeValueMap()
                .keySet()
                .forEach(key -> builder
                        .setDownPropagation(key, PropagationMode.JVM_LOCAL));
    }

    @VisibleForTesting
    void collectUserSettings(Map<String, DataSettings> dataSettings, PropagationMetaData.Builder builder) {
        dataSettings.forEach((key, settings) -> {
            if (settings.getDownPropagation() != null) {
                builder.setDownPropagation(key, settings.getDownPropagation());
            }
            if (settings.getUpPropagation() != null) {
                builder.setUpPropagation(key, settings.getUpPropagation());
            }
            if (settings.getSessionStorage() != null) {
                builder.setSessionStorage(key, settings.getSessionStorage());
            }
        });
    }
}
