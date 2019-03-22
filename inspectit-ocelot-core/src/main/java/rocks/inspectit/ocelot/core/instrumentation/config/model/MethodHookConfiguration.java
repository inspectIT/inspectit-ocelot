package rocks.inspectit.ocelot.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;

import java.util.List;
import java.util.Map;

/**
 * The configuration used to build a {@link MethodHook}
 * Note that the {@link #equals(Object)} function on this class is used to decide whether a recreation of the hook is required.
 */
@Builder
@Value
public class MethodHookConfiguration {

    /**
     * The ordered list of data assignments performed on method entry.
     * The first argument of the pair is the key of the data, the second is the data provider.
     */
    @Singular
    List<Pair<String, DataProviderCallConfig>> entryProviders;

    /**
     * The ordered list of data assignments performed on method exit.
     * The first argument of the pair is the key of the data, the second is the data provider.
     */
    @Singular
    List<Pair<String, DataProviderCallConfig>> exitProviders;

    /**
     * Maps the metrics to capture to the data keys to use as value.
     */
    @Singular
    Map<String, String> dataMetrics;

    /**
     * Maps the metrics to capture to the constants which should be used as value.
     * This is for example useful for counter metrics where you don't actually are interested in the value.
     */
    @Singular
    Map<String, Number> constantMetrics;
}
