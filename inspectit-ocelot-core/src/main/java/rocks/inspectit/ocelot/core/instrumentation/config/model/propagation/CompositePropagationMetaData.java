package rocks.inspectit.ocelot.core.instrumentation.config.model.propagation;

import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;

import java.util.HashMap;
import java.util.Map;

/**
 * Composition-based implementation for {@link PropagationMetaData}.
 * <p>
 * This data structure is designed to hold overrides for specified data keys.
 * If a value was not explicitly defined for a data key, the lookup is delegated to a "parent" {@link PropagationMetaData}
 * instance.
 */
class CompositePropagationMetaData implements PropagationMetaData {

    /**
     * The parent instance to use for lookups if no value was specified for a given key.
     */
    private PropagationMetaData parent;

    /**
     * Maps data keys to overrides for the down-propagation settings.
     */
    private Map<String, PropagationMode> downPropagationOverrides;

    /**
     * Maps data keys to overrides for the up-propagation settings.
     */
    private Map<String, PropagationMode> upPropagationOverrides;

    /**
     * Maps data keys to a boolean specifying whether the given data key is a tag or not.
     */
    private Map<String, Boolean> tagOverrides;

    private CompositePropagationMetaData() {
    }

    /**
     * Creates a new builder using the given instance as parent.
     * <p>
     * The implementation is specialized to prevent deep-nesting of {@link CompositePropagationMetaData}s.
     *
     * @param parent the parent instance to use
     * @return the newly created builder
     */
    public static PropagationMetaData.Builder builder(PropagationMetaData parent) {
        return new CompositePropagationMetaDataBuilder(parent);
    }

    @Override
    public boolean isPropagatedDownWithinJVM(String dataKey) {
        PropagationMode mode = downPropagationOverrides.get(dataKey);
        if (mode == null) {
            return parent.isPropagatedDownWithinJVM(dataKey);
        } else {
            return mode == PropagationMode.JVM_LOCAL || mode == PropagationMode.GLOBAL;
        }
    }

    @Override
    public boolean isPropagatedDownGlobally(String dataKey) {
        PropagationMode mode = downPropagationOverrides.get(dataKey);
        if (mode == null) {
            return parent.isPropagatedDownGlobally(dataKey);
        } else {
            return mode == PropagationMode.GLOBAL;
        }
    }

    @Override
    public boolean isPropagatedUpWithinJVM(String dataKey) {
        PropagationMode mode = upPropagationOverrides.get(dataKey);
        if (mode == null) {
            return parent.isPropagatedUpWithinJVM(dataKey);
        } else {
            return mode == PropagationMode.JVM_LOCAL || mode == PropagationMode.GLOBAL;
        }
    }

    @Override
    public boolean isPropagatedUpGlobally(String dataKey) {
        PropagationMode mode = upPropagationOverrides.get(dataKey);
        if (mode == null) {
            return parent.isPropagatedUpGlobally(dataKey);
        } else {
            return mode == PropagationMode.GLOBAL;
        }
    }

    @Override
    public boolean isTag(String dataKey) {
        Boolean isTag = tagOverrides.get(dataKey);
        return isTag != null ? isTag : parent.isTag(dataKey);
    }

    @Override
    public PropagationMetaData.Builder copy() {
        return new CompositePropagationMetaDataBuilder(this);
    }

    private static class CompositePropagationMetaDataBuilder implements PropagationMetaData.Builder {

        private CompositePropagationMetaData result;

        CompositePropagationMetaDataBuilder(PropagationMetaData parent) {
            result = new CompositePropagationMetaData();
            if (parent instanceof CompositePropagationMetaData) {
                CompositePropagationMetaData cParent = (CompositePropagationMetaData) parent;
                result.parent = cParent.parent;
                result.downPropagationOverrides = new HashMap<>(cParent.downPropagationOverrides);
                result.upPropagationOverrides = new HashMap<>(cParent.upPropagationOverrides);
                result.tagOverrides = new HashMap<>(cParent.tagOverrides);
            } else {
                result.parent = parent;
                result.downPropagationOverrides = new HashMap<>();
                result.upPropagationOverrides = new HashMap<>();
                result.tagOverrides = new HashMap<>();
            }
        }

        @Override
        public Builder setTag(String dataKey, boolean isTag) {
            result.tagOverrides.put(dataKey, isTag);
            return this;
        }

        @Override
        public Builder setDownPropagation(String dataKey, PropagationMode propagation) {
            result.downPropagationOverrides.put(dataKey, propagation);
            return this;
        }

        @Override
        public Builder setUpPropagation(String dataKey, PropagationMode propagation) {
            result.upPropagationOverrides.put(dataKey, propagation);
            return this;
        }

        @Override
        public PropagationMetaData build() {
            return result;
        }
    }
}
