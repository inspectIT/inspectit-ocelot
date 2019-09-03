package rocks.inspectit.ocelot.core.config;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    public void startStringFinder() {
        env.readPropertySources(propertySources -> {
            propertySources.stream()
                    .filter(ps -> ps instanceof EnumerablePropertySource)
                    .map(ps -> (EnumerablePropertySource) ps)
                    .flatMap(ps -> Arrays.stream(ps.getPropertyNames()))
                    .filter(ps -> !checkPropertyName(ps))
                    .forEach(ps -> log.warn("Expression could not be resolved to a property: " + ps));
        });
    }

    /**
     * Checks if a given property is an inspectit-property and is a valid path in the config model
     * This method firstly checks if a given String fulfills the basic requirements: being not null and starting with "inspectit."
     * If a String does not fulfill these basic requirements the method returns false
     * If these checks are successful, a process for recursive path-checking is triggered with checkPropertyExists.
     * This process checks each element of the given path for existence. Upon on the first occurrence of a non-existing path,
     * false is returned. If the path exists, true is returned
     *
     * @param propertyName
     * @return True: the propertyName exists as path <br> False: the propertyName does not exist as path
     */
    @VisibleForTesting
    boolean checkPropertyName(String propertyName) {
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

    boolean isInvalidPath(ArrayList<String> parsedName) {
        Type t = PropertyPathHelper.getPathEndType(parsedName.subList(1, parsedName.size()), InspectitConfig.class);
        if (t == null) {
            return false;
        }
        return PropertyPathHelper.isTerminal(t) || PropertyPathHelper.isListOfTerminalTypes(t);
    }

}