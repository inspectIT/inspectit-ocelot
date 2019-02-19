package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * The configuration used to build a {@link rocks.inspectit.oce.core.instrumentation.hook.MethodHook}
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
    List<Pair<String, ResolvedDataProviderCall>> entryProviders;

    /**
     * The ordered list of data assignments performed on method exit.
     * The first argument of the pair is the key of the data, the second is the data provider.
     */
    @Singular
    List<Pair<String, ResolvedDataProviderCall>> exitProviders;
}
