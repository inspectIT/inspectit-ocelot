package rocks.inspectit.ocelot.core.attributes;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.context.Scope;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.utils.AttributeUtils;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Component that provides attributes that should be considered as common and used when ever a metric is recorded.
 */
@Component
@Slf4j
public class CommonAttributesManager {

    /**
     * Defines with which @{@link Order} the event listener for updating the common attributes in reaction to an updated configuration is executed.
     * The Common attributes manager defines highest precedence to ensure that all other registered listeners have access to the updated attributes.
     */
    public static final int CONFIG_EVENT_LISTENER_ORDER_PRIORITY = Ordered.HIGHEST_PRECEDENCE;

    @Autowired
    private InspectitEnvironment env;

    /**
     * All {@link ICommonAttributesProvider}s registered in the manager.
     */
    @Autowired
    private List<ICommonAttributesProvider> providers;

    /**
     * All common attributes a simple String map.
     */
    @Getter
    private Map<String, String> commonAttributeValueMap = Collections.emptyMap();

    /**
     * OpenTelemetry baggage representing common baggage.
     */
    @Getter
    private Baggage commonBaggage = Baggage.empty();

    /**
     * List of common attribute keys that can be used when creating the views.
     */
    private List<String> commonAttributeKeys = Collections.emptyList();

    /**
     * Returns common attribute keys that all view should register.
     *
     * @return the common attribute keys that all view should register.
     */
    public List<String> getCommonAttributeKeys() {
        return Collections.unmodifiableList(commonAttributeKeys);
    }

    /**
     * Returns newly created scope with common attributes.
     * ALWAYS close the scope returned by the method.
     * Metrics collectors should use this scope with the try/resource block:
     * <code>
     * try (Scope scope = withCommonAttributesScope()) {
     *      my-counter.add(1L);
     * }
     * </code>
     *
     * @return the newly created scope with default baggage
     */
    public Scope withCommonAttributesScope() {
        return commonBaggage.makeCurrent();
    }

    /**
     * Returns newly created scope with common attributes including the additional given attributes.
     * ALWAYS close the scope returned by the method.
     * Metrics collectors should use this scope with the try/resource block:
     * <code>
     * try (Scope scope = withCommonAttributesScope()) {
     *      my-counter.add(1L);
     * }
     * </code>
     *
     * @param customAttributeMap Map with additional attributes
     *
     * @return the newly created scope with default baggage including the additional given attributes
     */
    public Scope withCommonAttributesScope(Map<String, String> customAttributeMap) {
        if (CollectionUtils.isEmpty(customAttributeMap)) {
            return withCommonAttributesScope();
        }

        BaggageBuilder builder = Baggage.builder();
        HashMap<String, String> attributes = new HashMap<>(commonAttributeValueMap);
        attributes.putAll(customAttributeMap);
        attributes.forEach(builder::put);

        return builder.build().makeCurrent();
    }

    /**
     * @return the attributes which are exposed for the given view
     */
    public Set<String> getAttributeKeysForView(ViewDefinitionSettings settings) {
        Set<String> viewAttributes = new HashSet<>();
        if (settings.isWithCommonAttributes()) {
            getCommonAttributeKeys().stream()
                    .filter(attr -> settings.getAttributes().get(attr) != Boolean.FALSE)
                    .forEach(viewAttributes::add);
        }

        settings.getAttributes().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .forEach(viewAttributes::add);

        return viewAttributes;
    }

    /**
     * Processes all {@link #providers} and creates common baggage based on the providers' priority.
     */
    @EventListener(InspectitConfigChangedEvent.class)
    @Order(CONFIG_EVENT_LISTENER_ORDER_PRIORITY)
    @PostConstruct
    private void update() {
        InspectitConfig configuration = env.getCurrentConfig();

        // first create map of attributes based on the providers priority
        Map<String, String> newCommonAttributeValueMap = new HashMap<>();
        providers.forEach(provider ->
                provider.getAttributes(configuration).forEach(newCommonAttributeValueMap::putIfAbsent)
        );

        // then create key/value attribute pairs for resolved map
        List<String> newCommonAttributeKeys = new ArrayList<>();
        BaggageBuilder builder = Baggage.builder();

        newCommonAttributeValueMap.forEach((key, value) -> {
            newCommonAttributeKeys.add(key);
            builder.put(key, AttributeUtils.resolveValue(key, value));
        });

        commonAttributeKeys = newCommonAttributeKeys;
        commonAttributeValueMap = newCommonAttributeValueMap;
        commonBaggage = builder.build();
    }
}
