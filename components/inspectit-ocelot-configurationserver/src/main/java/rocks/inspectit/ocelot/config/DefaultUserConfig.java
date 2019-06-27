package rocks.inspectit.ocelot.config;

import lombok.Data;

/**
 * Config structure for defining the default admin user which is
 * created if no User-Database is found.
 */
@Data
public class DefaultUserConfig {
    private String name;
    private String password;
}
