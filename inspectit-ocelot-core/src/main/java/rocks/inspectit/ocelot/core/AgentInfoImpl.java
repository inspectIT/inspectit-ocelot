package rocks.inspectit.ocelot.core;

import rocks.inspectit.ocelot.bootstrap.exposed.InspectitAgentInfo;

import java.util.Arrays;

/**
 * Implementation for the bootstrap interface {@link InspectitAgentInfo}.
 */
public class AgentInfoImpl implements InspectitAgentInfo {

    public static final String BEAN_NAME = "inspectitAgentInfo";

    /** The current agent version */
    private final String version;

    public AgentInfoImpl(String version) {
        this.version = version;
    }

    @Override
    public String currentVersion() {
        return version;
    }

    @Override
    public boolean isAtLeastVersion(String requiredVersion) {
        // We cannot compare the version
        if("UNKNOWN".equals(version)) return false;

        // We assume SNAPSHOT is the newest version
        if("SNAPSHOT".equals(version)) return true;

        int[] current = parseVersion(version);
        int[] required = parseVersion(requiredVersion);

        for (int i = 0; i < current.length; i++) {
            if (current[i] < required[i]) {
                return false;
            } else if (current[i] > required[i]) {
                return true;
            }
            // equal number -> continue
        }
        return true;
    }

    /**
     * Parses the version string into an array of major, minor and patch version number.
     * For instance the version '2.6.12' will be parsed to an array of [2,6,12].
     * We always expect 3 version numbers.
     *
     * @param version the provided version
     *
     * @return the version numbers as array
     */
    private int[] parseVersion(String version) {
        int[] versionNumbers = Arrays.stream(version.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();

        if(versionNumbers.length != 3) throw new IllegalArgumentException("Unsupported version format: " + version);

        return versionNumbers;
    }
}
