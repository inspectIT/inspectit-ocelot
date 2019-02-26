package rocks.inspectit.oce.core.instrumentation.hook;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
@Builder
@NonFinal //for mocking in tests
public class MethodReflectionInformation {

    /**
     * The name of the method which is being hooked. This corresponds to {@link Method#getName()}.
     * For a constructor the methodName is "{@literal <init>}".
     */
    private final String name;

    /**
     * The ordered list of types of the hooked methods parameters.
     */
    private final List<WeakReference<Class<?>>> parameterTypes;

    /**
     * The class which declares the hooked method.
     */
    private final WeakReference<Class<?>> declaringClass;

    static MethodReflectionInformation createFor(Class<?> declaringClass, MethodDescription methodDescription) {
        val builder = MethodReflectionInformation.builder();
        builder.name(methodDescription.asSignatureToken().getName());
        builder.declaringClass(new WeakReference<>(declaringClass));

        Stream.of(declaringClass.getDeclaredConstructors())
                .filter(methodDescription::represents)
                .findFirst()
                .ifPresent(constructor ->
                        builder.parameterTypes(Stream.of(constructor.getParameterTypes())
                                .map(c -> new WeakReference<Class<?>>(c))
                                .collect(Collectors.toList())));

        Stream.of(declaringClass.getDeclaredMethods())
                .filter(methodDescription::represents)
                .findFirst()
                .ifPresent(reflMethod ->
                        builder.parameterTypes(Stream.of(reflMethod.getParameterTypes())
                                .map(c -> new WeakReference<Class<?>>(c))
                                .collect(Collectors.toList())));
        return builder.build();
    }


    public Class<?>[] getParameterTypes() {
        Class<?>[] paramTypesArray = new Class[parameterTypes.size()];
        for (int i = 0; i < paramTypesArray.length; i++) {
            paramTypesArray[i] = parameterTypes.get(i).get();
        }
        return paramTypesArray;
    }

    /**
     * @return The class on which this method hook is applied, never null as long as the class has not been garbage collected.
     */
    public Class<?> getDeclaringClass() {
        return declaringClass.get();
    }


    public String getMethodFQN() {
        return getDeclaringClass().getName() + "." + getName();
    }

}
