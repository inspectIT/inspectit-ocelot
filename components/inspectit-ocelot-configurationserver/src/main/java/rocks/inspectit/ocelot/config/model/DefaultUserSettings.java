package rocks.inspectit.ocelot.config.model;

import lombok.Builder;
import lombok.Data;

/**
 * Config structure for defining the default admin user which is
 * created if no User-Database is found.
 */
@Data
@Builder
public class DefaultUserSettings {

    private String name;

    private String password;
    
}
