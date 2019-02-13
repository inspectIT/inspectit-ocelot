package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import rocks.inspectit.oce.bootstrap.instrumentation.IGenericDataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Contains all properties necessary for defining a {@link IGenericDataProvider}.
 * Instances of this class are derived by the {@link rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver} from
 * corresponding {@link rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.GenericDataProviderSettings} objects.
 */
@Value
@Builder
public class ResolvedGenericDataProviderConfig {

    /**
     * The name uniquely identifying this provider.
     */
    private String name;

    /**
     * A map defining which call arguments of the instrumented method the provider requires and what he expects as their type.
     * These are usually defined using the input parameters named "arg0","arg1" etc
     */
    @Singular
    private Map<Integer, String> expectedArgumentTypes;

    /**
     * The type of the return value of the instrumented method the data provider expects.
     * If the data provider does not use the return value, this field is null.
     */
    private String expectedReturnValueType;

    /**
     * The type of the "this"-object of the instrumented method the data provider expects.
     * If the data provider does not use the this-instance, this field is null.
     */
    private String expectedThisType;

    /**
     * If true, the data provider uses the java.lang.Throwable thrown, giving access to the thrown exception (if any was thrown).
     */
    private boolean usesThrown;

    /**
     * If true, the data provider uses the Object[] args array, giving direct access to theuncasted method parameters
     * of the instrumented method
     */
    private boolean usesArgsArray;

    /**
     * A sorted map of the additional arguments passed to the provider.
     * Additional arguments are usually either constants or "data" taken from the context.
     * <p>
     * The key is the name of the argument, the value is the type.
     * The order the arguments appear in the sorted map corresponds to the order the additionalArgs are passed to
     * {@link IGenericDataProvider#execute(Object[], Object, Object, Throwable, Object[])}
     */
    @Singular
    private SortedMap<String, String> additionalArgumentTypes;

    /**
     * Allows to import packages, so that it is not required to use the Full-Qualified name when referencing types.
     * All packages in this list will be imported.
     */
    @Builder.Default
    private List<String> importedPackages = new ArrayList<>();

    /**
     * A java method body using the specified variables, containing a return statement returning the value provided by this provider
     */
    private String valueBody;
}
