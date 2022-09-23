package rocks.inspectit.ocelot.core.selfmonitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.util.HashMap;
import java.util.Map;

@Component
public class DynamicallyActivatableServiceObserver {

    public static Map<String, Boolean> serviceStates = new HashMap<>();

    public void getServices(DynamicallyActivatableService service) {
        System.out.println("[SERVICE]: " + service + " STATE: " + service.isEnabled());
        System.out.println("[SERVICE CLASS NAME]: " + service.getName());

        serviceStates.put(service.getName(), service.isEnabled());
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
