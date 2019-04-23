package rocks.inspectit.ocelot.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Value;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.DataProviderCallSettings;

/**
 * Combines a resolved data provider with a call to it.
 * The call defines how the providers input arguments are assigned.
 * <p>
 * The equals method can be used on this object to detect if any changes occurred.
 * THis includes changes to the data provider itself and also changes to the input assignments perfomred by the call.
 */
@Value
@Builder
public class DataProviderCallConfig {

    /**
     * The name used for this data provider call.
     * This corresponds to the data key written by the provider.
     */
    String name;

    /**
     * The input assignments to use for calling the data provider.
     * It is guaranteed that the provider name specified {@link #callSettings} is the name of the provider defined by {@link #provider}.
     */
    private DataProviderCallSettings callSettings;

    /**
     * The definition of the data provider which shall be called.
     * It is guaranteed that the provider name specified {@link #callSettings} is the name of the provider defined by {@link #provider}.
     */
    private GenericDataProviderConfig provider;
}

