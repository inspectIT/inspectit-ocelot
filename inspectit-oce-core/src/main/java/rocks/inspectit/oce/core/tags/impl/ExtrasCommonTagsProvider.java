package rocks.inspectit.oce.core.tags.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.tags.ICommonTagsProvider;

import java.util.Map;

/**
 * Tags providers for user defined extra tags.
 */
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExtrasCommonTagsProvider implements ICommonTagsProvider {


    @Override
    public Map<String, String> getTags(InspectitConfig configuration) {
        return configuration.getTags().getExtra();
    }

}
