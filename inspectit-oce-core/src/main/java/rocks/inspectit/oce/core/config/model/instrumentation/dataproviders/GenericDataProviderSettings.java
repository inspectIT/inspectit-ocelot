package rocks.inspectit.oce.core.config.model.instrumentation.dataproviders;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Settings defining a generic data provider.
 * The name is defined by the key within the map
 * {@link InstrumentationSettings#getDataProviders()}
 */
@Data
@NoArgsConstructor
public class GenericDataProviderSettings {

    public static final String PACKAGE_REGEX = "[a-zA-Z]\\w*(\\.[a-zA-Z]\\w*)*";

    public static final String THIZ_VARIABLE = "thiz";
    public static final String ARGS_VARIABLE = "args";
    public static final String THROWN_VARIABLE = "thrown";
    public static final String RETURN_VALUE_VARIABLE = "returnValue";
    public static final String ARG_VARIABLE_PREFIX = "arg";
    public static final Pattern ARG_VARIABLE_PATTERN = Pattern.compile(ARG_VARIABLE_PREFIX + "(\\d+)");

    //these special variables are passed in via the additionalArguments array
    public static final String CLAZZ_VARIABLE = "clazz";
    public static final String METHOD_NAME_VARIABLE = "methodName";
    public static final String METHOD_PARAMETER_TYPES_VARIABLE = "parameterTypes";


    private static final List<Pattern> SPECIAL_VARIABLES_REGEXES = Arrays.asList(
            Pattern.compile(THIZ_VARIABLE),
            Pattern.compile(ARGS_VARIABLE),
            Pattern.compile(THROWN_VARIABLE),
            Pattern.compile(RETURN_VALUE_VARIABLE),
            ARG_VARIABLE_PATTERN,
            Pattern.compile(CLAZZ_VARIABLE),
            Pattern.compile(METHOD_NAME_VARIABLE),
            Pattern.compile(METHOD_PARAMETER_TYPES_VARIABLE)
    );

    /**
     * Defines the input variables used by this data provider.
     * The key is the name of the variable, the value is the type of the corresponding variable.
     * The following "special" variables are available:
     * - thiz: the this-instance on which the instrumented method is executed.
     * - argN: the N-th argument with which the instrumented method was invoked.
     * - args: an array of all arguments with which the instrumented method was invoked, the type must be Object[]
     * - returnValue: the value returned by the instrumented method,
     * - clazz: the java.lang.{@link Class} defining the method being instrumented
     * - methodName: the name of the method being instrumented, e.g. "hashcode", "doXYZ" or "<init>" for a constructor
     * - parameterTypes: the types of the arguments with which the method is declared in form of a Class[] array
     * <p>
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
    private List<@javax.validation.constraints.Pattern(regexp = PACKAGE_REGEX)
            String> imports = new ArrayList<>();

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


    @AssertFalse(message = "Either 'value' or 'valueBody' must be present")
    private boolean isEitherValueOrValueBodyPresent() {
        return StringUtils.isEmpty(value) && StringUtils.isEmpty(valueBody);
    }

    @AssertFalse(message = "'value' and 'valueBody' cannot be both specified!")
    private boolean isNotValueAndValueBodyPresent() {
        return !StringUtils.isEmpty(value) && !StringUtils.isEmpty(valueBody);
    }

    @AssertTrue(message = "The 'args' input must have the type 'Object[]'")
    private boolean isArgsArrayTypeCorrect() {
        String argsType = input.get(ARGS_VARIABLE);
        return isJavaLangTypeOrNull(argsType, "Object[]");
    }

    @AssertTrue(message = "The 'thrown' input must have the type 'Throwable'")
    private boolean isThrownTypeCorrect() {
        String thrownType = input.get(THROWN_VARIABLE);
        return isJavaLangTypeOrNull(thrownType, "Throwable");
    }

    @AssertTrue(message = "The 'clazz' input must have the type 'Class'")
    private boolean isClazzTypeCorrect() {
        String clazzType = input.get(CLAZZ_VARIABLE);
        return isJavaLangTypeOrNull(clazzType, "Class");
    }

    @AssertTrue(message = "The 'methodName' input must have the type 'String'")
    private boolean isMethodNameTypeCorrect() {
        String methodNameType = input.get(METHOD_NAME_VARIABLE);
        return isJavaLangTypeOrNull(methodNameType, "String");
    }

    @AssertTrue(message = "The 'parameterTypes' input must have the type 'Class[]'")
    private boolean isParameterTypesTypeCorrect() {
        String parameterTypesType = input.get(METHOD_PARAMETER_TYPES_VARIABLE);
        return isJavaLangTypeOrNull(parameterTypesType, "Class[]");
    }

    public static boolean isSpecialVariable(String varName) {
        return SPECIAL_VARIABLES_REGEXES.stream().anyMatch(p -> p.matcher(varName).matches());
    }

    private boolean isJavaLangTypeOrNull(String type, String expectedSimpleName) {
        return type == null || type.equals(expectedSimpleName) || type.equals("java.lang." + expectedSimpleName);
    }

}
