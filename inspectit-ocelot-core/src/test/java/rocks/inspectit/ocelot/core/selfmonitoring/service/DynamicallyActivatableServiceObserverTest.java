package rocks.inspectit.ocelot.core.selfmonitoring.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import rocks.inspectit.ocelot.core.command.AgentCommandService;
import rocks.inspectit.ocelot.core.exporter.JaegerExporterService;
import rocks.inspectit.ocelot.core.exporter.PrometheusExporterService;
import rocks.inspectit.ocelot.core.selfmonitoring.LogPreloader;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.util.ArrayList;
import java.util.List;
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

        List<DynamicallyActivatableService> expectedList = new ArrayList<DynamicallyActivatableService>() {{
            add(new LogPreloader());
            add(new PrometheusExporterService());
            add(new AgentCommandService());
            add(new JaegerExporterService());
        }};

        void setupTest(){
            for(DynamicallyActivatableService service : expectedList){
                serviceObserver.getServices(service);
            }
        }

        @Test
        public void checkMap(){
            setupTest();

            Map<String, Boolean> resultMap = serviceObserver.getServiceStates();

            //Check LogPreloader
            assertThat(resultMap.containsKey(logPreloader.getName()));
            assertThat(resultMap.get(logPreloader.getName())).isEqualTo(logPreloader.isEnabled());

            //Check PrometheusExporterService
            assertThat(resultMap.containsKey(promExpoService.getName()));
            assertThat(resultMap.get(promExpoService.getName())).isEqualTo(promExpoService.isEnabled());

            //Check AgentCommandService
            assertThat(resultMap.containsKey(agentCommandService.getName()));
            assertThat(resultMap.get(agentCommandService.getName())).isEqualTo(agentCommandService.isEnabled());

            //Check JaegerExporterService
            assertThat(resultMap.containsKey(jaegerExpoService.getName()));
            assertThat(resultMap.get(jaegerExpoService.getName())).isEqualTo(jaegerExpoService.isEnabled());
        }
    }
}
