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

        Map<String, Boolean> expectedMap = new HashMap<String, Boolean>(){{
            put(logPreloader.getName(), true);
            put(promExpoService.getName(), false);
            put(agentCommandService.getName(), true);
            put(jaegerExpoService.getName(), false);
        }};

        void setupTest(){
            ObjectMapper objMapper = new ObjectMapper();

            try {
                expectedJSON = objMapper.writeValueAsString(expectedMap);
            } catch (Exception e) {
                System.out.println(e.getMessage()); //Add proper logging
            }

            for(String service : expectedMap.keySet()){
                serviceObserver.serviceStates.put(service, expectedMap.get(service));
            }
        }

        @Test
        public void checkMapToJson(){
            setupTest();

            assertThat(serviceObserver.MapToJson()).isEqualTo(expectedJSON);
        }
    }
}
