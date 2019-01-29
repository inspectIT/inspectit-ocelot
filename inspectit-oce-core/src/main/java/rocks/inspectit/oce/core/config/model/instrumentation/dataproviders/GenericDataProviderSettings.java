package rocks.inspectit.oce.core.config.model.instrumentation.dataproviders;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.dataprovider.generic.DataProviderGenerator;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings defining a generic data provider.
 * The name is defined by the key within the map
 * {@link InstrumentationSettings#getDataProviders()}
 */
@Data
@NoArgsConstructor
public class GenericDataProviderSettings {

    /**
     * Defines the input variables used by this data provider.
     * The key is the name of the variable, the value is the type of the corresponding variable.
     * The following "special" variables are available:
     * - thiz: the this-instance on which the instrumented method is executed.
     * - argN: the N-th argument with which the instrumented method was invoked.
     * - args: an array of all arguments with which the instrumented method was invoked, the type must be Object[]
     * - returnValue: the value returned by the instrumented method,
     * null if void, the method threw an exception or the provider is not executed at the method exit
     * - thrown: the {@link Throwable}-Object raised by the the executed method, the type must be java.lang.Throwable
     * null if no throwable was raised
     * <p>
     * In addition arbitrary custom input variables may be defined.
     */
    private Map<@NotBlank String, @NotBlank String> input = new HashMap<>();

    /**
     * A list of packages to import when compiling the java code and deriving the types of {@link #input}
     * If a classname is not found, the given packages will be scanned in the given order to locate the class.
     * This allows the User to use classes without the need to specify the FQN.
     */
    private List<@NotBlank String> imports = new ArrayList<>();

    /**
     * A single Java-statement (without return) defining the value of this data provider.
     * The statement must be of type Object, primitive results have to wrapped manually!
     * If this field is present, {@link #valueBody} must be null!
     */
    private String value;

    /**
     * A string defining the Java method body of the data-provider without surrounding braces {}.
     * This method body must have a return statement to return the value provided by the provider!
     * The statement must be of type Object, primitive results have to wrapped manually!
     * If this field is present, {@link #value} must be null!
     */
    private String valueBody;


    @AssertTrue(message = "Either 'value' or 'valueBody' must be present (and not both)!")
    private boolean isEitherValueOrValueBodyPresent() {
        boolean valueEmpty = StringUtils.isEmpty(value);
        boolean valueBodyEmpty = StringUtils.isEmpty(valueBody);
        return (!valueEmpty && valueBodyEmpty) || (valueEmpty && !valueBodyEmpty);
    }

    @AssertTrue(message = "The 'args' input must have the type 'Object[]'")
    private boolean isArgsArrayTypeCorrect() {
        String argsType = input.get(DataProviderGenerator.ARGS_VARIABLE);
        return argsType == null || argsType.equals("Object[]") || argsType.equals("java.lang.Object[]");
    }

    @AssertTrue(message = "The 'thrown' input must have the type 'Throwable'")
    private boolean isThrownTypeCorrect() {
        String thrownType = input.get(DataProviderGenerator.THROWN_VARIABLE);
        return thrownType == null || thrownType.equals("java.lang.Throwable") || thrownType.equals("Throwable");
    }
}
