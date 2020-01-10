package rocks.inspectit.ocelot.core.instrumentation.config.model.propagation;

import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;

public interface PropagationMetaData {

    boolean isPropagatedDownWithinJVM(String dataKey);

    boolean isPropagatedDownGlobally(String dataKey);

    boolean isPropagatedUpWithinJVM(String dataKey);

    boolean isPropagatedUpGlobally(String dataKey);

    boolean isTag(String dataKey);

    Builder copy();

    static Builder builder() {
        return RootPropagationMetaData.builder();
    }

    interface Builder {

        Builder setTag(String dataKey, boolean isTag);

        Builder setDownPropagation(String dataKey, PropagationMode propagation);

        Builder setUpPropagation(String dataKey, PropagationMode propagation);

        PropagationMetaData build();
    }
}
