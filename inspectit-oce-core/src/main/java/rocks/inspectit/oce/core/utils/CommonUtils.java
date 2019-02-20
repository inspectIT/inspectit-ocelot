package rocks.inspectit.oce.core.utils;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A generic utility class.
 */
public class CommonUtils {

    /**
     * Return true if the JVM is shutting down.
     * Note: This method is expensive! Only call it in destructions methods and don't call it within loops!
     *
     * @return true if the JVM is shutting down, false otherwise
     */
    public static boolean isJVMShuttingDown() {
        Thread dummyHook = new Thread(() -> {
        });
        try {
            Runtime.getRuntime().addShutdownHook(dummyHook);
            Runtime.getRuntime().removeShutdownHook(dummyHook);
        } catch (IllegalStateException e) {
            return true;
        }
        return false;
    }

    public static boolean contentsEqual(Set<?> setA, Set<?> setB) {
        if (setA.size() != setB.size()) {
            return false;
        }
        return !setA.stream().anyMatch(elem -> !setB.contains(elem));
    }

    /**
     * Tries to find the given type in the given classloader or one of its parents.
     * Hereby, the given imported packages are taken into account.
     * In addition, java.lang is assumed to be an implicit import.
     *
     * @param typename the name of the type, can also be fully qualified
     * @param context  the classloader to search in
     * @param packages the imported packages, e.g. "java.util", "javax.servlet"
     * @return the Class if it was found, null otherwise
     */
    public static Class<?> locateTypeWithinImports(String typename, ClassLoader context, Collection<String> packages) {
        return Stream.concat(
                Stream.concat(
                        Stream.of(""),
                        packages.stream().map(s -> s + ".")
                ),
                Stream.of("java.lang.")
        ).flatMap(prefix -> {
            try {
                return Stream.of(Class.forName(prefix + typename, false, context));
            } catch (Exception e) {
                return Stream.empty();
            }
        }).findFirst().orElse(null);
    }

    /**
     * Returns the signature for the given method for the given method or constructor, does not contain the return type.
     * The signature does not contain any spaces.
     *
     * @param m the method to query the signature for
     * @return the signature string in the form methodname(paramtype,paramtype)
     */
    public static String getSignature(MethodDescription m) {
        MethodDescription.SignatureToken sig = m.asSignatureToken();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(sig.getName()).append('(');
        boolean first = true;
        for (TypeDescription parameterType : sig.getParameterTypes()) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(',');
            }
            stringBuilder.append(parameterType.getName());
        }
        return stringBuilder.append(')').toString();
    }

}
