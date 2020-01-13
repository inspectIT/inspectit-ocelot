package rocks.inspectit.ocelot.core.instrumentation.config.model.propagation;

import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;

/**
 * Interface for a data structure holding information about the context propagation behaviour of individual data keys.
 * <p>
 * Implementations should be designed as Immutable. If changes are required, it should be copied and altered via {@link #clone()}.
 */
public interface PropagationMetaData {

    /**
     * @param dataKey the data key to check
     * @return true, if the dataKey is configured with JVM_LOCAL or GLOBAL down-propagation.
     */
    boolean isPropagatedDownWithinJVM(String dataKey);

    /**
     * @param dataKey the data key to check
     * @return true, if the dataKey is configured with GLOBAL down-propagation.
     */
    boolean isPropagatedDownGlobally(String dataKey);

    /**
     * @param dataKey the data key to check
     * @return true, if the dataKey is configured with JVM_LOCAL or GLOBAL up-propagation.
     */
    boolean isPropagatedUpWithinJVM(String dataKey);

    /**
     * @param dataKey the data key to check
     * @return true, if the dataKey is configured with GLOBAL up-propagation.
     */
    boolean isPropagatedUpGlobally(String dataKey);

    /**
     * @param dataKey the data key to check
     * @return true, if the data key is configured to be used as tag
     */
    boolean isTag(String dataKey);

    /**
     * Copies the currently active settings to a new Builder.
     * This builder can then be altered as required and used to generate a new PropagationMetaData instance.
     * *
     *
     * @return a builder initially having exactly the same settings as this object.
     */
    Builder copy();

    /**
     * Creates a new, empty builder for the default PropagationMetaData implementation.
     * By default, all data keys will not propagate and won't be used as tags.
     *
     * @return the new, empty Builder.
     */
    static Builder builder() {
        return RootPropagationMetaData.builder();
    }

    /**
     * Builder for construction a {@link PropagationMetaData} instance.
     */
    interface Builder {

        Builder setTag(String dataKey, boolean isTag);

        Builder setDownPropagation(String dataKey, PropagationMode propagation);

        Builder setUpPropagation(String dataKey, PropagationMode propagation);

        PropagationMetaData build();
    }
}
