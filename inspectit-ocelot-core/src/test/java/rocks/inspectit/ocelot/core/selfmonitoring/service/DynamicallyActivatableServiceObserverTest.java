package rocks.inspectit.ocelot.core.selfmonitoring.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import rocks.inspectit.ocelot.core.command.AgentCommandService;
import rocks.inspectit.ocelot.core.exporter.PrometheusExporterService;
import rocks.inspectit.ocelot.core.selfmonitoring.logs.LogPreloader;
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

        List<DynamicallyActivatableService> expectedList = new ArrayList<DynamicallyActivatableService>() {{
            add(new LogPreloader());
            add(new PrometheusExporterService());
            add(new AgentCommandService());
        }};

        void setupTest(){
            for(DynamicallyActivatableService service : expectedList){
                serviceObserver.updateServiceState(service);
            }
        }

        @Test
        public void checkMap(){
            setupTest();

            Map<String, Boolean> resultMap = serviceObserver.getServiceStateMap();

            //Check LogPreloader
            assertThat(resultMap.containsKey(logPreloader.getName())).isTrue();
            assertThat(resultMap.get(logPreloader.getName())).isEqualTo(logPreloader.isEnabled());

            //Check PrometheusExporterService
            assertThat(resultMap.containsKey(promExpoService.getName())).isTrue();
            assertThat(resultMap.get(promExpoService.getName())).isEqualTo(promExpoService.isEnabled());

            //Check AgentCommandService
            assertThat(resultMap.containsKey(agentCommandService.getName())).isTrue();
            assertThat(resultMap.get(agentCommandService.getName())).isEqualTo(agentCommandService.isEnabled());
        }

        // Hello again ;)
        @Test
        public void testAsJson() {
            setupTest();
            String expectedJson = "{\"LoggingMetricExporterService\":false,\"MemoryMetricsRecorder\":true,\"HttpConfigurationPoller\":true,\"OtlpTraceExporterService\":false,\"DiskMetricsRecorder\":true,\"ActionMetricsRecorder\":false,\"PrometheusExporterService\":false,\"InstrumentationFeedbackService\":false,\"LoggingTraceExporterService\":false,\"GCMetricsRecorder\":true,\"OtlpMetricsExporterService\":false,\"JmxMetricsRecorder\":false,\"ProcessorMetricsRecorder\":true,\"ThreadMetricsRecorder\":true,\"AgentCommandService\":false,\"ConcurrentMetricsRecorder\":false,\"ConfigurationDirectoriesPoller\":false,\"LogPreloader\":false,\"ClassLoaderMetricsRecorder\":true,\"ZipkinExporterService\":false}";

            assertThat(serviceObserver.asJson()).isEqualTo(expectedJson);
        }
    }
}
