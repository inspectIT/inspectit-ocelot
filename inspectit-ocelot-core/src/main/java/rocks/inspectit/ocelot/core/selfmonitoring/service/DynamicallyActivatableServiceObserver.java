package rocks.inspectit.ocelot.core.selfmonitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.util.HashMap;
import java.util.Map;

/**
 * Observes all the currently available services and their current state (enabled/disabled)
 *
 * Has a method to generate a JSON String out of the services and their states to send it to the UI
 */
@Component
public class DynamicallyActivatableServiceObserver {
    @Getter
    private static Map<String, Boolean> serviceStates = new HashMap<>();

    @Getter
    private static String settingStatesJSON = "{}";

    public void getServices(DynamicallyActivatableService service) {
        serviceStates.put(service.getName(), service.isEnabled());

        settingStatesJSON = MapToJson();
    }

    public String MapToJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{}";

        try {
            json = objectMapper.writeValueAsString(serviceStates);
        } catch (Exception e) {
            System.out.println(e.getMessage()); //Add proper logging
        }

        return json;
    }
}
