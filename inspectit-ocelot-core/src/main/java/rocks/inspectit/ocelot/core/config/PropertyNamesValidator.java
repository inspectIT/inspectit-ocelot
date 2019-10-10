package rocks.inspectit.ocelot.core.config;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

@Slf4j
@Component
public class PropertyNamesValidator {

    @Autowired
    private InspectitEnvironment env;

    /**
     * A HashSet of classes which are used as wildcards in the search for properties. If a found class matches one of these
     * classes, the end of the property path is reached. Mainly used in the search of maps
     */
    private static final HashSet<Class<?>> TERMINAL_TYPES = new HashSet(Arrays.asList(Object.class, String.class, Integer.class, Long.class,
            Float.class, Double.class, Character.class, Void.class,
            Boolean.class, Byte.class, Short.class, Duration.class));

    @PostConstruct
    @EventListener(InspectitConfigChangedEvent.class)
    public void logInvalidPropertyNames() {
        env.readPropertySources(propertySources -> {
            propertySources.stream()
                    .filter(ps -> ps instanceof EnumerablePropertySource)
                    .map(ps -> (EnumerablePropertySource) ps)
                    .flatMap(ps -> Arrays.stream(ps.getPropertyNames()))
                    .filter(ps -> isInvalidPropertyName(ps))
                    .forEach(ps -> log.warn("The specified property '{}' does not exist! ", ps));
        });
    }

    /**
     * Checks if a given property should be handled as an invalid property. Invalid properties are properties which
     * apparently are meant to be found in the inspectit environment but are not. This applies for all properties
     * starting with "inspectit.". Further it is checked whether or not a given property exists in the model and ends
     * in a terminal-type. Terminal types are all enums, primitive types and their corresponding wrapper classes as well
     * as Duration.class, Path.class, URL.class and FileSystemResource.class
     *
     * @param propertyName the path which should be checked
     * @return True: the propertyName exists as path <br> False: the propertyName does not exist as path
     */
    @VisibleForTesting
    boolean isInvalidPropertyName(String propertyName) {
        ArrayList<String> parsedName = (ArrayList<String>) PropertyPathHelper.parse(propertyName);
        try {
            return propertyName != null
                    && propertyName.startsWith("inspectit.")
                    && isInvalidPath(parsedName);
        } catch (Exception e) {
            log.error("Error while checking property existence", e);
            return false;
        }
    }

    /**
     * Checks if a given path is invalid. Invalid paths are such paths that are not resembled by the variables found in
     * the InspectitConfig class
     *
     * @param parsedName the path which should be checked
     * @return True if the path is invalid, false if the path is valid
     */
    private boolean isInvalidPath(ArrayList<String> parsedName) {
        Type t = PropertyPathHelper.getPathEndType(parsedName.subList(1, parsedName.size()), InspectitConfig.class);
        if (t == null) {
            return true;
        }
        return !PropertyPathHelper.isTerminal(t) && !PropertyPathHelper.isListOfTerminalTypes(t);
    }

}