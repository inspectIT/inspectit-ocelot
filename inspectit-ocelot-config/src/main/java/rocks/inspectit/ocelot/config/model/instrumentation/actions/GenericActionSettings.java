package rocks.inspectit.ocelot.config.model.instrumentation.actions;

import lombok.*;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Settings defining a generic action.
 * The name is defined by the key within the map
 * {@link InstrumentationSettings#getActions()}
 */
@Data
@NoArgsConstructor
public class GenericActionSettings {

    public static final String PACKAGE_REGEX = "[a-zA-Z]\\w*(\\.[a-zA-Z]\\w*)*";

    public static final String THIS_VARIABLE = "_this";
    public static final String ARGS_VARIABLE = "_args";
    public static final String THROWN_VARIABLE = "_thrown";
    public static final String RETURN_VALUE_VARIABLE = "_returnValue";
    public static final String ARG_VARIABLE_PREFIX = "_arg";
    public static final Pattern ARG_VARIABLE_REGEX = Pattern.compile(ARG_VARIABLE_PREFIX + "\\d+");

    //these special variables are passed in via the additionalArguments array
    public static final String CLASS_VARIABLE = "_class";
    public static final String METHOD_NAME_VARIABLE = "_methodName";
    public static final String METHOD_PARAMETER_TYPES_VARIABLE = "_parameterTypes";

    public static final String CONTEXT_VARIABLE = "_context";
    public static final String OBJECT_ATTACHMENTS_VARIABLE = "_attachments";


    private static final List<Pattern> SPECIAL_VARIABLES_REGEXES = Arrays.asList(
            Pattern.compile(THIS_VARIABLE),
            Pattern.compile(ARGS_VARIABLE),
            Pattern.compile(THROWN_VARIABLE),
            Pattern.compile(RETURN_VALUE_VARIABLE),
            ARG_VARIABLE_REGEX,
            Pattern.compile(CLASS_VARIABLE),
            Pattern.compile(METHOD_NAME_VARIABLE),
            Pattern.compile(METHOD_PARAMETER_TYPES_VARIABLE),
            Pattern.compile(CONTEXT_VARIABLE),
            Pattern.compile(OBJECT_ATTACHMENTS_VARIABLE)
    );

    /**
     * If true, the action does not return a value.
     * This means when it is called, no data is explicitly written unless done through
     * the _context special variable.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isVoid = false;

    /**
     * Defines the input variables used by this action.
     * The key is the name of the variable, the value is the type of the corresponding variable.
     * The following "special" variables are available:
     * - _this: the this-instance on which the instrumented method is executed.
     * - _argN: the N-th argument with which the instrumented method was invoked.
     * - _args: an array of all arguments with which the instrumented method was invoked, the type must be Object[]
     * - _returnValue: the value returned by the instrumented method, <p>
     * null if void, the method threw an exception or the provider is not executed at the method exit
     * - _class: the java.lang.{@link Class} defining the method being instrumented
     * - _methodName: the name of the method being instrumented, e.g. "hashcode", "doXYZ" or "<init>" for a constructor
     * - _parameterTypes: the types of the arguments with which the method is declared in form of a Class[] array
     * - _attachments: an {@link ObjectAttachments} instance which allows you to "attach" values to a given object
     * - _context: gives read and write access to the current {@link InspectitContext}, allowing you to attach values to the control flow
     * - _thrown: the {@link Throwable}-Object raised by the the executed method, the type must be java.lang.Throwable
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
     * A single Java-statement (without return) defining the value of this action.
     * The statement must be of type Object, primitive results have to wrapped manually!
     * If this field is present, {@link #valueBody} must be null!
     */
    private String value;

    /**
     * A string defining the Java method body of the generic action without surrounding braces {}.
     * This method body must have a return statement to return the value provided by the provider!
     * The statement must be of type Object, primitive results have to wrapped manually!
     * If this field is present, {@link #value} must be null!
     */
    private String valueBody;

    /*
    Why don't we use Lombok generated getters / setters here?
    Lombok would generate methods named isVoid() and setVoid(),
    which imply that the property is named "void" and not "is-void".
    This would confuse the spring configuration binding.
     */
    public boolean getIsVoid() {
        return isVoid;
    }

    public void setIsVoid(boolean isVoid) {
        this.isVoid = isVoid;
    }

    @AssertFalse(message = "Either 'value' or 'valueBody' must be present")
    private boolean isEitherValueOrValueBodyPresent() {
        return StringUtils.isEmpty(value) && StringUtils.isEmpty(valueBody);
    }

    @AssertFalse(message = "'value' and 'valueBody' cannot be both specified!")
    private boolean isNotValueAndValueBodyPresent() {
        return !StringUtils.isEmpty(value) && !StringUtils.isEmpty(valueBody);
    }

    @AssertTrue(message = "The '_args' input must have the type 'Object[]'")
    private boolean isArgsArrayTypeCorrect() {
        String argsType = input.get(ARGS_VARIABLE);
        return verifyType(argsType, "Object[]");
    }

    @AssertTrue(message = "The '_thrown' input must have the type 'Throwable'")
    private boolean isThrownTypeCorrect() {
        String thrownType = input.get(THROWN_VARIABLE);
        return verifyType(thrownType, "Throwable");
    }

    @AssertTrue(message = "The '_class' input must have the type 'Class'")
    private boolean isClazzTypeCorrect() {
        String clazzType = input.get(CLASS_VARIABLE);
        return verifyType(clazzType, "Class");
    }

    @AssertTrue(message = "The '_methodName' input must have the type 'String'")
    private boolean isMethodNameTypeCorrect() {
        String methodNameType = input.get(METHOD_NAME_VARIABLE);
        return verifyType(methodNameType, "String");
    }

    @AssertTrue(message = "The '_parameterTypes' input must have the type 'Class[]'")
    private boolean isParameterTypesTypeCorrect() {
        String parameterTypesType = input.get(METHOD_PARAMETER_TYPES_VARIABLE);
        return verifyType(parameterTypesType, "Class[]");
    }

    @AssertTrue(message = "The '_context' input must have the type 'InspectitContext'")
    private boolean isContextTypeCorrect() {
        String type = input.get(CONTEXT_VARIABLE);
        return type == null || "InspectitContext".equals(type);
    }

    @AssertTrue(message = "The '_attachments' input must have the type 'ObjectAttachments'")
    private boolean isAttachmentsTypeCorrect() {
        String type = input.get(OBJECT_ATTACHMENTS_VARIABLE);
        return type == null || "ObjectAttachments".equals(type);
    }

    public static boolean isSpecialVariable(String varName) {
        return SPECIAL_VARIABLES_REGEXES.stream().anyMatch(p -> p.matcher(varName).matches());
    }

    private boolean verifyType(String type, String expectedSimpleName) {
        return type == null || type.equals(expectedSimpleName) || type.equals("java.lang." + expectedSimpleName);
    }

}
