package rocks.inspectit.ocelot.core.selfmonitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.util.HashMap;
import java.util.Map;

/**
 * Observes all currently available services that are supported by the
 * inspectit ocelot agent (e.g. Prometheus, Jaeger, Influx, ...) as well as integrated services such as Log-Preloading
 * and the Agent Command Service and tracks their current state (enabled/disabled).
 * Those states will be send to the Frontend, where depending on the state of the service we can enable/disable certain
 * features. As well as preventing the user to run into errors and give proper guidance to which services still have to
 * be activated to access certain features.
 *
 * Has a method to generate a JSON String out of the services and their states to send it to the UI.
 * The JSON String contains the service names and their current state (true=enabled/false=disabled).
 * In the Frontend the JSON String can be parsed to a JavaScript object to work with.
 */
@Component
public class DynamicallyActivatableServiceObserver {
    @Getter
    private static Map<String, Boolean> serviceStateMap = new HashMap<>();

    public void updateServiceState(DynamicallyActivatableService service) {
        serviceStateMap.put(service.getName(), service.isEnabled());
    }

    public String MapToJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{}";

        try {
            json = objectMapper.writeValueAsString(serviceStateMap);
        } catch (Exception e) {
            System.out.println(e.getMessage()); //Add proper logging
        }

        return json;
    }

    public static String asJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{}";

        try {
            json = objectMapper.writeValueAsString(serviceStateMap);
        } catch (Exception e) {
            System.out.println(e.getMessage()); //Add proper logging
        }

        return json;
    }
}
