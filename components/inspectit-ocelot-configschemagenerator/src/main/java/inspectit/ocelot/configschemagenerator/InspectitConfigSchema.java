package inspectit.ocelot.configschemagenerator;

import lombok.Data;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

/**
 * Wrapper class to include the root property {@code inspectit} into the schema.
 */
@Data
public class InspectitConfigSchema {

    /**
     * root element of the configuration schema
     */
    private InspectitConfig inspectit;
}
