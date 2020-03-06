package rocks.inspectit.ocelot.core.instrumentation.config.model.propagation;

import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;

import java.util.HashSet;
import java.util.Set;

/**
 * Standard implementation for {@link PropagationMetaData}.
 * It is designed for best query performance.
 * <p>
 * Has an optimized {@link #copy()} which slightly reduces the performance of the resulting {@link PropagationMetaData},
 * it however provides a copy time proportional only to the number of changed settings.
 */
class RootPropagationMetaData implements PropagationMetaData {

    /**
     * Contains all data keys which have been defined to be used as tags.
     */
    private Set<String> tagKeys = new HashSet<>();

    /**
     * Contains all data keys which have a down propagation of either JVM_LOCAL or GLOBAL.
     */
    private Set<String> localDownPropagatedKeys = new HashSet<>();

    /**
     * Contains all data keys which have a down propagation of GLOBAL.
     * Therefore this set is a subset of {@link #localDownPropagatedKeys}.
     */
    private Set<String> globalDownPropagatedKeys = new HashSet<>();

    /**
     * Contains all data keys which have an up propagation of either JVM_LOCAL or GLOBAL.
     */
    private Set<String> localUpPropagatedKeys = new HashSet<>();

    /**
     * Contains all data keys which have an up propagation of GLOBAL.
     * Therefore this set is a subset of {@link #localUpPropagatedKeys}.
     */
    private Set<String> globalUpPropagatedKeys = new HashSet<>();

    private RootPropagationMetaData() {
    }

    public static Builder builder() {
        return new RootPropagationMetaDataBuilder();
    }

    @Override
    public boolean isPropagatedDownWithinJVM(String dataKey) {
        return localDownPropagatedKeys.contains(dataKey);
    }

    @Override
    public boolean isPropagatedDownGlobally(String dataKey) {
        return globalDownPropagatedKeys.contains(dataKey);
    }

    @Override
    public boolean isPropagatedUpWithinJVM(String dataKey) {
        return localUpPropagatedKeys.contains(dataKey);
    }

    @Override
    public boolean isPropagatedUpGlobally(String dataKey) {
        return globalUpPropagatedKeys.contains(dataKey);
    }

    @Override
    public boolean isTag(String dataKey) {
        return tagKeys.contains(dataKey);
    }

    @Override
    public Builder copy() {
        return CompositePropagationMetaData.builder(this);
    }

    private static class RootPropagationMetaDataBuilder implements RootPropagationMetaData.Builder {

        private RootPropagationMetaData result = new RootPropagationMetaData();

        @Override
        public Builder setTag(String dataKey, boolean isTag) {
            if (isTag) {
                result.tagKeys.add(dataKey);
            } else {
                result.tagKeys.remove(dataKey);
            }
            return this;
        }

        @Override
        public Builder setDownPropagation(String dataKey, PropagationMode propagation) {
            switch (propagation) {
                case NONE:
                    result.localDownPropagatedKeys.remove(dataKey);
                    result.globalDownPropagatedKeys.remove(dataKey);
                    break;
                case JVM_LOCAL:
                    result.localDownPropagatedKeys.add(dataKey);
                    result.globalDownPropagatedKeys.remove(dataKey);
                    break;
                case GLOBAL:
                    result.localDownPropagatedKeys.add(dataKey);
                    result.globalDownPropagatedKeys.add(dataKey);
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled case: " + propagation);
            }
            return this;
        }

        @Override
        public Builder setUpPropagation(String dataKey, PropagationMode propagation) {
            switch (propagation) {
                case NONE:
                    result.localUpPropagatedKeys.remove(dataKey);
                    result.globalUpPropagatedKeys.remove(dataKey);
                    break;
                case JVM_LOCAL:
                    result.localUpPropagatedKeys.add(dataKey);
                    result.globalUpPropagatedKeys.remove(dataKey);
                    break;
                case GLOBAL:
                    result.localUpPropagatedKeys.add(dataKey);
                    result.globalUpPropagatedKeys.add(dataKey);
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled case: " + propagation);
            }
            return this;
        }

        @Override
        public PropagationMetaData build() {
            return result;
        }
    }

}
