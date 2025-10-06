package rocks.inspectit.ocelot.core.attributes;

import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.util.Map;

public interface ICommonAttributesProvider {

    /**
     * @return the attributes provided by this metrics providers
     */
    Map<String, String> getAttributes(InspectitConfig configuration);
}
