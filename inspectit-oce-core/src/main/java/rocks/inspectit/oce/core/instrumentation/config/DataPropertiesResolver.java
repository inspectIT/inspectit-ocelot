package rocks.inspectit.oce.core.instrumentation.config;

import lombok.val;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.data.DataSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedDataProperties;

import java.util.Optional;
import java.util.Set;

@Component
public class DataPropertiesResolver {

    /**
     * When a data key starts (case insensitvely) with this prefix, it is assumed that it denotes local data.
     * This means that the down-propagation by default is disabled and that it is not published as a Tag.
     * <p>
     * Note that this default behaviour only is applied for tags which appear in {@link InstrumentationSettings#getAllDataKeys()}.
     * E.g. if data appears because it is inherited from a TagContext built in the instrumented application, the normal defaul values are used:
     * Then the data is propagated downwards and published as a tag.
     */
    private static final String LOCAL_DATA_PREFIX = "local_";

    public ResolvedDataProperties resolve(InstrumentationSettings settings) {
        val builder = ResolvedDataProperties.builder();
        Set<String> keys = settings.getAllDataKeys();
        for (String dataKey : keys) {
            DataSettings ds = Optional.ofNullable(settings.getData().get(dataKey)).orElse(new DataSettings());
            boolean isLocal = dataKey.toLowerCase().startsWith(LOCAL_DATA_PREFIX);
            ds.setDownPropagation(Optional.ofNullable(ds.getDownPropagation()).orElse(
                    isLocal ? PropagationMode.NONE : PropagationMode.JVM_LOCAL
            ));

            ds.setUpPropagation(Optional.ofNullable(ds.getUpPropagation()).orElse(PropagationMode.NONE));

            ds.setIsTag(Optional.ofNullable(ds.getIsTag()).orElse(
                    !isLocal
            ));

            builder.data(dataKey, ds);
        }
        return builder.build();
    }
}
