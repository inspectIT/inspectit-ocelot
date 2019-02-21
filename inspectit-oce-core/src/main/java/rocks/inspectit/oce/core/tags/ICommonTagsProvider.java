package rocks.inspectit.oce.core.tags;

import rocks.inspectit.oce.core.config.model.InspectitConfig;

import java.util.Map;

public interface ICommonTagsProvider {

    /**
     * Get tags provided by this metrics providers.
     *
     * @return Get tags provided by this metrics providers.
     */
    Map<String, String> getTags(InspectitConfig configuration);

}
