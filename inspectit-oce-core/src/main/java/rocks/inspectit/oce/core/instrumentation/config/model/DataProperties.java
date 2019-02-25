package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.*;
import lombok.experimental.NonFinal;
import rocks.inspectit.oce.core.config.model.instrumentation.data.DataSettings;

import java.util.Set;

@Value
@Builder
@NonFinal //To allow mocking for testing
public class DataProperties {

    @Getter(value = AccessLevel.NONE)
    @Singular("notATag")
    private Set<String> noneTagKeys;

    @Getter(value = AccessLevel.NONE)
    @Singular("upPropagatedWithinJVM")
    private Set<String> upPropagatedWithinJVM;

    /**
     * Subset of {@link #upPropagatedWithinJVM}.
     */
    @Getter(value = AccessLevel.NONE)
    @Singular("upPropagatedGlobally")
    private Set<String> upPropagatedGlobally;

    /**
     * Why is this set negated (it stores all keys which are NOT down propagated)?
     * This is because the default value is that down propagation is active.
     * This ensures that for keys which are not known down propagation gets activated correctly.
     */
    @Getter(value = AccessLevel.NONE)
    @Singular("notDownPropagatedWithinJVM")
    private Set<String> notDownPropagatedWithinJVM;

    @Getter(value = AccessLevel.NONE)
    @Singular("downPropagatedGlobally")
    private Set<String> downPropagatedGlobally;


    public boolean isPropagatedUpWithinJVM(String dataKey) {
        return upPropagatedWithinJVM.contains(dataKey);
    }

    public boolean isPropagatedUpGlobally(String dataKey) {
        return upPropagatedGlobally.contains(dataKey);
    }

    public boolean isPropagatedDownWithinJVM(String dataKey) {
        return !notDownPropagatedWithinJVM.contains(dataKey);
    }

    public boolean isPropagatedDownGlobally(String dataKey) {
        return downPropagatedGlobally.contains(dataKey);
    }


    public boolean isTag(String dataKey) {
        return !noneTagKeys.contains(dataKey);
    }

    public static class DataPropertiesBuilder {

        public DataPropertiesBuilder data(String key, DataSettings settings) {
            if (!settings.isTag()) {
                notATag(key);
            }
            switch (settings.getDownPropagation()) {
                case NONE:
                    notDownPropagatedWithinJVM(key);
                    break;
                case JVM_LOCAL:
                    break;
                case GLOBAL:
                    downPropagatedGlobally(key);
                    break;
            }
            switch (settings.getUpPropagation()) {
                case NONE:
                    break;
                case JVM_LOCAL:
                    upPropagatedWithinJVM(key);
                    break;
                case GLOBAL:
                    upPropagatedWithinJVM(key);
                    upPropagatedGlobally(key);
                    break;
            }
            return this;
        }
    }

}
