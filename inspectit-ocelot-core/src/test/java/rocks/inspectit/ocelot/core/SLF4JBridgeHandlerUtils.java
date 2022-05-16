package rocks.inspectit.ocelot.core;

import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Utility class for {@link SLF4JBridgeHandler}
 */
public class SLF4JBridgeHandlerUtils {

    /**
     * Installs the {@link SLF4JBridgeHandler} to send java.util.logging to SLF4J
     */
    public static void installSLF4JBridgeHandler() {
        // enable jul -> slf4j bridge
        // this is necessary as OTEL logs to jul, but we use the LogCapturer with logback
        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
    }

    /**
     * Uninstalls the {@link SLF4JBridgeHandler} if installed.
     */
    public static void uninstallSLF4jBridgeHandler() {
        if (SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.uninstall();
        }
    }
}
