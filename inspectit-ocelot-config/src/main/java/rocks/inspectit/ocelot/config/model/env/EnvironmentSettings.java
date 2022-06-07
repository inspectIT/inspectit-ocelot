package rocks.inspectit.ocelot.config.model.env;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contains settings which are usually not directly specified by the user.
 * Instead they are derived from the environment on startup.
 * These properties can be used to parametrize other properties using the ${} syntax.
 * E.g. using "${inspectit.env.jar-dir}/conf" for file-based configuration will point to a folder next to the
 * inspectit agent-jar.
 */
@Data
@NoArgsConstructor
public class EnvironmentSettings {

    /**
     * The directory containing the agent jar file.
     * Does not contain a trailing slash.
     */
    private String agentDir;

    /**
     * The hostname where the agent is running.
     */
    private String hostname;

    /**
     * The process id of the JVM process.
     */
    private String pid;

    /**
     * The Java version used by the JVM process.
     */
    private String javaVersion;

    /**
     * The version of the inspectIT Ocelot agent.
     */
    private String agentVersion;

    /**
     * The version of OpenTelemetry the agent was build with.
     */
    private String openTelemetryVersion;
}
