package rocks.inspectit.ocelot.core.selfmonitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import rocks.inspectit.ocelot.core.command.AgentCommandService;
import rocks.inspectit.ocelot.core.exporter.JaegerExporterService;
import rocks.inspectit.ocelot.core.exporter.PrometheusExporterService;
import rocks.inspectit.ocelot.core.selfmonitoring.LogPreloader;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicallyActivatableServiceObserver}
 */
public class DynamicallyActivatableServiceObserverTest {
    DynamicallyActivatableServiceObserver serviceObserver = new DynamicallyActivatableServiceObserver();

    @Nested
    class CheckBasicFunctionalityDASObserver {
        DynamicallyActivatableService logPreloader = new LogPreloader();
        DynamicallyActivatableService promExpoService = new PrometheusExporterService();
        DynamicallyActivatableService agentCommandService = new AgentCommandService();
        DynamicallyActivatableService jaegerExpoService = new JaegerExporterService();

        String expectedJSON = "{}";

        Map<DynamicallyActivatableService, Boolean> expectedMap = new HashMap<DynamicallyActivatableService, Boolean>(){{
            put(logPreloader, true);
            put(promExpoService, false);
            put(agentCommandService, true);
            put(jaegerExpoService, false);
        }};

        void setupTest(){
            ObjectMapper objMapper = new ObjectMapper();

            try {
                expectedJSON = objMapper.writeValueAsString(expectedMap);
            } catch (Exception e) {
                System.out.println(e.getMessage()); //Add proper logging
            }

            for(DynamicallyActivatableService service : expectedMap.keySet()){
                serviceObserver.getServices(service);
            }

        }

        @Test
        public void checkMap(){
            setupTest();

            Map<String, Boolean> resultMap = serviceObserver.getServiceStates();

            for(DynamicallyActivatableService service : expectedMap.keySet()){
                assertThat(resultMap.containsKey(service.getName()));
                assertThat(resultMap.get(service)).isEqualTo(expectedMap.get(service)); // Fails because enabled/disabled status not available :(
            }
        }
    }
}
