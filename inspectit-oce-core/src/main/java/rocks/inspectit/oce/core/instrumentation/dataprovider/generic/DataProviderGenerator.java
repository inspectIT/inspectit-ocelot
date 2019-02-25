package rocks.inspectit.oce.core.instrumentation.dataprovider.generic;

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
import rocks.inspectit.oce.bootstrap.instrumentation.IGenericDataProvider;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.GenericDataProviderSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.GenericDataProviderConfig;
import rocks.inspectit.oce.core.instrumentation.injection.ClassInjector;
import rocks.inspectit.oce.core.instrumentation.injection.InjectedClass;
import rocks.inspectit.oce.core.utils.AutoboxingHelper;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class DataProviderGenerator {

    /**
     * Guava seems to not allow null keys.
     * Therefore we just use this object as replacement for the bootstrap loader.
     */
    private static final ClassLoader BOOTSTRAP_LOADER_MARKER = new URLClassLoader(new URL[]{});

    private static final String GENERIC_PROVIDER_STRUCTURAL_ID = "genericDataProvider";

    private static final String METHOD_ARGS = "$1";
    private static final String THIZ = "$2";
    private static final String RETURN_VALUE = "$3";
    private static final String THROWN = "$4";
    private static final String ADDITIONAL_ARGS = "$5";

    @Autowired
    private ClassInjector classInjector;

    private LoadingCache<ClassLoader, Cache<GenericDataProviderConfig, InjectedClass<? extends IGenericDataProvider>>> providersCache
            = CacheBuilder.newBuilder().weakKeys().build(
            new CacheLoader<ClassLoader, Cache<GenericDataProviderConfig, InjectedClass<? extends IGenericDataProvider>>>() {
                @Override
                public Cache<GenericDataProviderConfig, InjectedClass<? extends IGenericDataProvider>> load(ClassLoader key) {
                    return CacheBuilder.newBuilder().weakValues().build();
                }
            });

    /**
     * Provides an executable {@link IGenericDataProvider} based on the given configuration.
     * The provider is either dynamically compiled and injected or a cached provider is used.
     *
     * @param providerConfig       the configuration of the generic data provider to use
     * @param classToUseProviderOn the context in which the provider will be actively. The provider will be injected into the classloader of this class.
     * @return the generated provider
     * @throws ExecutionException
     */
    @SuppressWarnings("unchecked")
    public InjectedClass<? extends IGenericDataProvider> getOrGenerateDataProvider(GenericDataProviderConfig providerConfig, Class<?> classToUseProviderOn) {
        ClassLoader loader = Optional.ofNullable(classToUseProviderOn.getClassLoader()).orElse(BOOTSTRAP_LOADER_MARKER);
        providersCache.cleanUp();
        Cache<GenericDataProviderConfig, InjectedClass<? extends IGenericDataProvider>> clCache;
        try {
            clCache = providersCache.get(loader);
            clCache.cleanUp(); //cleanup to make sure unused InjectedClasses are released
            try {
                return clCache.get(providerConfig, () ->
                        (InjectedClass<? extends IGenericDataProvider>)
                                classInjector.inject(GENERIC_PROVIDER_STRUCTURAL_ID, classToUseProviderOn, (className) ->
                                        buildGenericDataProviderByteCode(providerConfig, loader, className)
                                ));
            } catch (ExecutionException | ExecutionError e) {
                log.error("Error creating data provider '{}' in context of class {}! Using a No-Operation data provider instead!",
                        providerConfig.getName(), classToUseProviderOn.getName(), e);
                return clCache.get(providerConfig, () -> new InjectedClass<IGenericDataProvider>(GenericDataProviderTemplate.class));
            }
        } catch (ExecutionException e) {
            //never happens
            throw new RuntimeException(e);
        }
    }

    private byte[] buildGenericDataProviderByteCode(GenericDataProviderConfig providerConfig, ClassLoader loader, String className) throws NotFoundException, CannotCompileException, IOException {
        ClassPool cp = new ClassPool();
        cp.insertClassPath(new ClassClassPath(GenericDataProviderTemplate.class));
        if (loader != BOOTSTRAP_LOADER_MARKER) {
            cp.insertClassPath(new LoaderClassPath(loader));
        }

        CtClass provider = cp.get(GenericDataProviderTemplate.class.getName());
        provider.setName(className);

        for (String packageName : providerConfig.getImportedPackages()) {
            cp.importPackage(packageName);
        }

        CtMethod method = provider.getDeclaredMethod("executeImpl");
        method.setBody(buildProviderMethod(providerConfig));

        return provider.toBytecode();
    }

    /**
     * Builds the Java source code used to replace {@link GenericDataProviderTemplate#executeImpl(Object[], Object, Object, Throwable, Object[])}.
     * <p>
     * Example configuration:
     * <pre>
     * {@code
     * my-provider:
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
     * @param providerConfig
     * @return
     */
    private String buildProviderMethod(GenericDataProviderConfig providerConfig) {
        StringBuilder methodBody = new StringBuilder("{");
        if (providerConfig.getExpectedThisType() != null) {
            buildVariableDefinition(methodBody, providerConfig.getExpectedThisType(), GenericDataProviderSettings.THIZ_VARIABLE, THIZ);
        }
        if (providerConfig.getExpectedReturnValueType() != null) {
            buildVariableDefinition(methodBody, providerConfig.getExpectedReturnValueType(), GenericDataProviderSettings.RETURN_VALUE_VARIABLE, RETURN_VALUE);
        }
        if (providerConfig.isUsesThrown()) {
            buildVariableDefinition(methodBody, "java.lang.Throwable", GenericDataProviderSettings.THROWN_VARIABLE, THROWN);
        }
        if (providerConfig.isUsesArgsArray()) {
            buildVariableDefinition(methodBody, "Object[]", GenericDataProviderSettings.ARGS_VARIABLE, METHOD_ARGS);
        }
        providerConfig.getExpectedArgumentTypes().forEach((id, type) -> {
            String value = METHOD_ARGS + "[" + id + "]";
            String varName = GenericDataProviderSettings.ARG_VARIABLE_PREFIX + id;
            buildVariableDefinition(methodBody, type, varName, value);
        });
        val additionalArgs = providerConfig.getAdditionalArgumentTypes();
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
        methodBody.append(providerConfig.getValueBody());

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
