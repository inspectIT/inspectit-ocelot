package rocks.inspectit.ocelot.core.utils;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.apache.commons.lang3.StringUtils;
import rocks.inspectit.ocelot.bootstrap.AgentProperties;

public class CoreUtils {

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

    /**
     * @return the configured temp-dir or the java default temp-dir
     */
    public static String getTempDir() {
        String configuredTempDir = null != System.getProperty(AgentProperties.INSPECTIT_TEMP_DIR_PROPERTY) ?
                System.getProperty(AgentProperties.INSPECTIT_TEMP_DIR_PROPERTY) : System.getenv(AgentProperties.INSPECTIT_TEMP_DIR_ENV_PROPERTY);
        if(configuredTempDir != null)
            return configuredTempDir;

        String tempdir = System.getProperty("java.io.tmpdir");
        if (StringUtils.isBlank(tempdir))
            return "";
        else if (tempdir.endsWith("/") || tempdir.endsWith("\\"))
            return tempdir.substring(0, tempdir.length() - 1);
         else
             return tempdir;
    }
}
