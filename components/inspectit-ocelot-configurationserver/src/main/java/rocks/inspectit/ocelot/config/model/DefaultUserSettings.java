package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Config structure for defining the default admin user which is
 * created if no User-Database is found.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultUserSettings {

    private String name;

    private String password;
    
}
