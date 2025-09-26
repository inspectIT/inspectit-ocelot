package rocks.inspectit.ocelot.core.attributes.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.attributes.ICommonAttributesProvider;

import java.util.Map;

/**
 * Tags providers for user defined extra tags.
 */
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExtrasCommonAttributesProvider implements ICommonAttributesProvider {


    @Override
    public Map<String, String> getAttributes(InspectitConfig configuration) {
        return configuration.getAttributes().getExtra();
    }

}
