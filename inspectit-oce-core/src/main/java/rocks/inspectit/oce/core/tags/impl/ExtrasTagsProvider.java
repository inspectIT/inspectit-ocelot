package rocks.inspectit.oce.core.tags.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.tags.AbstractTagsProvider;

import java.util.Map;

/**
 * Tags providers for user defined extra tags.
 */
@Component
@Slf4j
public class ExtrasTagsProvider extends AbstractTagsProvider {

    /**
     * Default constructor.
     */
    public ExtrasTagsProvider() {
        super("tags.extra");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        Map<String, String> extraMap = conf.getTags().getExtra();
        return extraMap != null && extraMap.size() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return Priority.HIGH.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> resolveTagsInternal(InspectitConfig configuration) {
        return configuration.getTags().getExtra();
    }

}
