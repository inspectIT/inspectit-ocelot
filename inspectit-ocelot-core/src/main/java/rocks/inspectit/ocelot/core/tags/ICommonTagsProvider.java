package rocks.inspectit.ocelot.core.tags;

import rocks.inspectit.ocelot.core.config.model.InspectitConfig;

import java.util.Map;

public interface ICommonTagsProvider {

    /**
     * Get tags provided by this metrics providers.
     *
     * @return Get tags provided by this metrics providers.
     */
    Map<String, String> getTags(InspectitConfig configuration);

}
