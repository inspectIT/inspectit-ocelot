package rocks.inspectit.ocelot.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Contains all properties necessary for defining a {@link IGenericAction}.
 * Instances of this class are derived by the {@link InstrumentationConfigurationResolver} from
 * corresponding {@link GenericActionSettings} objects.
 */
@Value
@Builder
public class GenericActionConfig {

    /**
     * See {@link GenericActionSettings#isVoid}
     */
    private boolean isVoid;

    /**
     * The name uniquely identifying this generic action.
     */
    private String name;

    /**
     * A map defining which call arguments of the instrumented method the action requires and what he expects as their type.
     * These are usually defined using the input parameters named "arg0","arg1" etc
     */
    @Singular
    private Map<Integer, String> expectedArgumentTypes;

    /**
     * The type of the return value of the instrumented method the action expects.
     * If the action does not use the return value, this field is null.
     */
    private String expectedReturnValueType;

    /**
     * The type of the "this"-object of the instrumented method the action expects.
     * If the action does not use the this-instance, this field is null.
     */
    private String expectedThisType;

    /**
     * If true, the action uses the java.lang.Throwable thrown, giving access to the thrown exception (if any was thrown).
     */
    private boolean usesThrown;

    /**
     * If true, the action uses the Object[] args array, giving direct access to the uncasted method parameters
     * of the instrumented method
     */
    private boolean usesArgsArray;

    /**
     * A sorted map of the additional arguments passed to the action.
     * Additional arguments are usually either constants or "data" taken from the context.
     * <p>
     * The key is the name of the argument, the value is the type.
     * The order the arguments appear in the sorted map corresponds to the order the additionalArgs are passed to
     * {@link IGenericAction#execute(Object[], Object, Object, Throwable, Object[])}
     */
    @Singular
    private SortedMap<String, String> additionalArgumentTypes;

    /**
     * Allows to import packages, so that it is not required to use the Full-Qualified name when referencing types.
     * All packages in this list will be imported.
     */
    @Singular
    private List<String> importedPackages;

    /**
     * A java method body using the specified variables, containing a return statement returning the value provided by this action
     */
    private String valueBody;
}
