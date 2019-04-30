package rocks.inspectit.ocelot.core.instrumentation.genericactions;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import javassist.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.exposed.ObjectAttachments;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.config.utils.AutoboxingHelper;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.injection.ClassInjector;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class GenericActionGenerator {

    /**
     * Classes in this package are accessible by generic actions.
     * They are implicitly imported.
     */
    private static final String INSPECTIT_ACCESSIBLE_BOOTSTRAP_PACKAGE = ObjectAttachments.class.getPackage().getName();

    /**
     * Javassist needs to access the .class files of all classes that are referenced in the code to compile.
     * However, for bootstrap classes those are not available.
     * Therefore we provide Javassists with a classloader which is actually never used,
     * but which points to the jar containing our bootstrap classes.
     */
    private static ClassLoader INSPECTIT_BOOTSTRAP_JAR_LOADER;

    static {
        if (Instances.BOOTSTRAP_JAR_URL != null) {
            //normal case, the AgentMain made Instances.BOOTSTRAP_JAR_URL point to the actual jar location
            INSPECTIT_BOOTSTRAP_JAR_LOADER = new URLClassLoader(new URL[]{Instances.BOOTSTRAP_JAR_URL});
        } else {
            //This is the unit test and integration test branch
            //there the bootstrap classes are included in the normal inspectit classloader
            INSPECTIT_BOOTSTRAP_JAR_LOADER = GenericActionGenerator.class.getClassLoader();
        }
    }

    /**
     * Guava seems to not allow null keys.
     * Therefore we just use this object as replacement for the bootstrap loader.
     */
    private static ClassLoader BOOTSTRAP_LOADER_MARKER = new URLClassLoader(new URL[]{});

    private static String NON_VOID_GENERIC_ACTION_STRUCTURAL_ID = "genericAction";
    private static String VOID_GENERIC_ACTION_STRUCTURAL_ID = "voidGenericAction";

    private static String METHOD_ARGS = "$1";
    private static String THIZ = "$2";
    private static String RETURN_VALUE = "$3";
    private static String THROWN = "$4";
    private static String ADDITIONAL_ARGS = "$5";

    @Autowired
    private ClassInjector classInjector;

    private LoadingCache<ClassLoader, Cache<GenericActionConfig, InjectedClass<? extends IGenericAction>>> actionsCache
            = CacheBuilder.newBuilder().weakKeys().build(
            new CacheLoader<ClassLoader, Cache<GenericActionConfig, InjectedClass<? extends IGenericAction>>>() {
                @Override
                public Cache<GenericActionConfig, InjectedClass<? extends IGenericAction>> load(ClassLoader key) {
                    return CacheBuilder.newBuilder().weakValues().build();
                }
            });

    /**
     * Provides an executable {@link IGenericAction} based on the given configuration.
     * The action is either dynamically compiled and injected or a cached action is used.
     *
     * @param actionConfig       the configuration of the generic action to use
     * @param classToUseActionOn the context in which the action will be active. The action will be injected into the classloader of this class.
     * @return the generated action
     */
    @SuppressWarnings("unchecked")
    public InjectedClass<? extends IGenericAction> getOrGenerateGenericAction(GenericActionConfig
                                                                                      actionConfig, Class<?> classToUseActionOn) {
        ClassLoader loader = Optional.ofNullable(classToUseActionOn.getClassLoader()).orElse(BOOTSTRAP_LOADER_MARKER);
        actionsCache.cleanUp();
        Cache<GenericActionConfig, InjectedClass<? extends IGenericAction>> clCache;
        try {
            clCache = actionsCache.get(loader);
            clCache.cleanUp(); //cleanup to make sure unused InjectedClasses are released
            try {
                String id = actionConfig.isVoid() ? VOID_GENERIC_ACTION_STRUCTURAL_ID : NON_VOID_GENERIC_ACTION_STRUCTURAL_ID;
                return clCache.get(actionConfig, () ->
                        (InjectedClass<? extends IGenericAction>)
                                classInjector.inject(id, classToUseActionOn, (className) ->
                                        buildGenericActionByteCode(actionConfig, loader, className)
                                ));
            } catch (ExecutionException | ExecutionError e) {
                log.error("Error creating generic action '{}' in context of class {}! Using a No-Operation action instead!",
                        actionConfig.getName(), classToUseActionOn.getName(), e);
                return clCache.get(actionConfig, () -> new InjectedClass<IGenericAction>(GenericActionTemplate.class));
            }
        } catch (ExecutionException e) {
            //never happens
            throw new RuntimeException(e);
        }
    }

    private byte[] buildGenericActionByteCode(GenericActionConfig actionConfig, ClassLoader loader, String
            className) throws NotFoundException, CannotCompileException, IOException {


        ClassPool cp = new ClassPool();
        cp.insertClassPath(new ClassClassPath(GenericActionTemplate.class));
        //include the dummy bootstrap loader to make interfaces such as InspectitContext or ObjectAttachments accessible
        cp.insertClassPath(new LoaderClassPath(INSPECTIT_BOOTSTRAP_JAR_LOADER));
        if (loader != BOOTSTRAP_LOADER_MARKER) {
            cp.insertClassPath(new LoaderClassPath(loader));
        }

        CtClass action;
        if (actionConfig.isVoid()) {
            action = cp.get(VoidGenericActionTemplate.class.getName());
        } else {
            action = cp.get(GenericActionTemplate.class.getName());
        }
        action.setName(className);

        cp.importPackage(INSPECTIT_ACCESSIBLE_BOOTSTRAP_PACKAGE);
        for (String packageName : actionConfig.getImportedPackages()) {
            cp.importPackage(packageName);
        }

        CtMethod method = action.getDeclaredMethod("executeImpl");
        method.setBody(buildActionMethod(actionConfig));

        return action.toBytecode();
    }

    /**
     * Builds the Java source code used to replace {@link GenericActionTemplate#executeImpl(Object[], Object, Object, Throwable, Object[])}.
     * <p>
     * Example configuration:
     * <pre>
     * {@code
     * my-action:
     *   input:
     *     x: boolean
     *     y: java.lang.Object
     *     arg0: java.lang.String
     *     arg2: int
     *     returnValue: my.domain.Object
     *     thrown: java.lang.Throwable
     *   value: "Hello World!"
     * }
     * </pre>
     * This configuration results in the following output:
     * <pre>
     * {@code
     *  {
     *     boolean x = ((Boolean)$5[0]).booleanValue();         //$5 refers in javassist to the fifth argument, which is Object[] additionalArgs
     *     java.lang.Object y = (java.lang.Object) $5[1];
     *     java.lang.String arg0 = (java.lang.Object) $1[0];    //$1 refers to the parameter Object[] methodArgs
     *     int arg2 = ((java.lang.Integer) $1[2]).intValue();
     *     my.domain.Object returnValue = (my.domain.Object) $3;
     *     java.lang.Throwable thrown = (java.lang.Throwable) $4;
     *
     *     return "Hello World!";
     *  }
     * }
     * </pre>
     *
     * @param actionConfig the config of the action to build
     * @return the generated method body as string
     */
    private String buildActionMethod(GenericActionConfig actionConfig) {
        StringBuilder methodBody = new StringBuilder("{");
        if (actionConfig.getExpectedThisType() != null) {
            buildVariableDefinition(methodBody, actionConfig.getExpectedThisType(), GenericActionSettings.THIS_VARIABLE, THIZ);
        }
        if (actionConfig.getExpectedReturnValueType() != null) {
            buildVariableDefinition(methodBody, actionConfig.getExpectedReturnValueType(), GenericActionSettings.RETURN_VALUE_VARIABLE, RETURN_VALUE);
        }
        if (actionConfig.isUsesThrown()) {
            buildVariableDefinition(methodBody, "java.lang.Throwable", GenericActionSettings.THROWN_VARIABLE, THROWN);
        }
        if (actionConfig.isUsesArgsArray()) {
            buildVariableDefinition(methodBody, "Object[]", GenericActionSettings.ARGS_VARIABLE, METHOD_ARGS);
        }
        actionConfig.getExpectedArgumentTypes().forEach((id, type) -> {
            String value = METHOD_ARGS + "[" + id + "]";
            String varName = GenericActionSettings.ARG_VARIABLE_PREFIX + id;
            buildVariableDefinition(methodBody, type, varName, value);
        });
        val additionalArgs = actionConfig.getAdditionalArgumentTypes();
        val iterator = additionalArgs.entrySet().iterator();
        int id = 0;
        while (iterator.hasNext()) {
            String value = ADDITIONAL_ARGS + "[" + id + "]";
            val argsDef = iterator.next();
            val varName = argsDef.getKey();
            val varType = argsDef.getValue();
            buildVariableDefinition(methodBody, varType, varName, value);
            id++;
        }
        methodBody.append(actionConfig.getValueBody());

        return methodBody.append("}").toString();
    }

    /**
     * Builds a variable definition where another variable is casted from a Object variable.
     * If the target type is a primitive, unboxing is performed.
     * <p>
     * For object types this results in the following string:
     * <p>
     * 'type.of.Variable variableName = (type.of.Variable) (value); \n'
     * <p>
     * If the target type is a primitive, the value is cast to the corresponding wrapper and unboxed.
     * E.g. for an "int":
     * <p>
     * 'int variableName = ((java.lang.Integer) (value)).intValue(); \n'
     *
     * @param buf      the stringbuffer to write the assignment to
     * @param type     the target type of the variable, can be a class or a primitive
     * @param variable the name of the target variable
     * @param value    the value to assign, can be a statement or another variable
     */
    private void buildVariableDefinition(StringBuilder buf, String type, String variable, String value) {
        buf.append(type).append(" ").append(variable).append(" = ");
        if (AutoboxingHelper.isPrimitiveType(type)) {
            String wrapperType = AutoboxingHelper.getWrapperForPrimitive(type);
            String unboxingMethod = AutoboxingHelper.getWrapperUnboxingMethodName(wrapperType);
            buf.append("((").append(wrapperType).append(")(").append(value).append(")).").append(unboxingMethod).append("()");
        } else {
            buf.append('(').append(type).append(")(").append(value).append(")");
        }
        buf.append(";\n");
    }

}
