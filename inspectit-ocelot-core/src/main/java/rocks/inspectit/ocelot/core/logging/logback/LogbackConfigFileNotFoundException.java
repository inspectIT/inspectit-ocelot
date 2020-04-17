package rocks.inspectit.ocelot.core.logging.logback;

public class LogbackConfigFileNotFoundException extends RuntimeException {

    public LogbackConfigFileNotFoundException(String logfileLocation) {
        super("The logback configuration file '" + logfileLocation + "' could not be found.");
    }

    public LogbackConfigFileNotFoundException(String logfileLocation, Throwable t) {
        super("The logback configuration file '" + logfileLocation + "' could not be found.", t);
    }
}
