package rocks.inspectit.ocelot.config.model.env;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contains settings which are usually not directly specified by the user.
 * Instead they are derived from the environment on startup.
 * These proeprties can be used to parametrize other proeprties using the ${} syntax.
 * E.g. using "${inspectit.env.jar-dir}/conf" for file-based configuration will point to a folder next to the
 * inspectit-jar.
 */
@Data
@NoArgsConstructor
public class EnvironmentSettings {

    /**
     * The directory containing the agent jar file.
     * Does not contain a trailing slash.
     */
    private String jarDir;
}
