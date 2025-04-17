package rocks.inspectit.ocelot.bootstrap;

import java.util.LinkedList;
import java.util.List;

/**
 * Stores all properties, which are not part of {@code InspectitConfig}, but can be configured.
 * Every property should be definable as system property or environment variable.
 */
public class AgentProperties {

    public static final String START_DELAY_PROPERTY = "inspectit.start.delay";

    public static final String START_DELAY_ENV_PROPERTY = "INSPECTIT_START_DELAY";

    public static final String RECYCLE_JARS_PROPERTY = "inspectit.recycle-jars";

    public static final String RECYCLE_JARS_ENV_PROPERTY = "INSPECTIT_RECYCLE_JARS";

    public static final String INSPECTIT_TEMP_DIR_PROPERTY = "inspectit.temp-dir";

    public static final String INSPECTIT_TEMP_DIR_ENV_PROPERTY = "INSPECTIT_TEMP_DIR";

    /** list of all system property names */
    private static final List<String> PROPERTY_NAMES = new LinkedList<>();

    static {
        PROPERTY_NAMES.add(START_DELAY_PROPERTY);
        PROPERTY_NAMES.add(RECYCLE_JARS_PROPERTY);
        PROPERTY_NAMES.add(INSPECTIT_TEMP_DIR_PROPERTY);
    }

    /**
     * @return all system property names, which are not part of {@code InspectitConfig}
     */
    public static List<String> getAllProperties() {
        return PROPERTY_NAMES;
    }
}
