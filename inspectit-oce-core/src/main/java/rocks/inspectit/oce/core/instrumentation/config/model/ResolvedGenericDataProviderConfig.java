package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.oce.bootstrap.instrumentation.IGenericDataProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Contains all properties necessary for defining a {@link IGenericDataProvider}.
 * Instances of this class are derived by the {@link rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver} from
 * corresponding {@link rocks.inspectit.oce.core.config.model.instrumentation.GenericDataProviderConfig} objects.
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
     * These are usually defined using the input paramters anmed "arg0","arg1" etc
     */
    @Builder.Default
    private HashMap<Integer, String> expectedArgumentTypes = new HashMap<>();

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
     * The type of the thrown exception the data provider expects.
     * If the data provider does not make use of the thrown exception, this field is null.
     * <p>
     * In contrast to the other fields, this type implies control flow.
     * If a throwable is thrown, which is not a subtype of this type, the provider automatically return null.
     */
    private String expectedThrowableType;

    /**
     * If true, the data provider uses the Objet[] args array, giving direct access to theuncasted method parameters
     * of the instrumented method
     */
    private boolean usesArgsArray;

    /**
     * A list of the additional arguments passed to the provider.
     * Additional arguments are usually either constants or "data" taken from the context.
     * <p>
     * The first element of the pair is the name of the argument, the second one being the type.
     * The order the arguments appear corresponds to the order the additionalArgs are passed to
     * {@link IGenericDataProvider#execute(Object[], Object, Object, Throwable, Object[])}
     */
    @Builder.Default
    private List<Pair<String, String>> additionalArgumentTypes = new ArrayList<>();

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
