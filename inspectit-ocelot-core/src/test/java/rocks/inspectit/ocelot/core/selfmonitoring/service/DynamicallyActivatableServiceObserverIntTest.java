package rocks.inspectit.ocelot.core.selfmonitoring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.exporter.JaegerExporterService;
import rocks.inspectit.ocelot.core.exporter.OtlpMetricsExporterService;
import rocks.inspectit.ocelot.core.exporter.PrometheusExporterService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DynamicallyActivatableServiceObserverIntTest extends SpringTestBase {

    @Autowired
    private DynamicallyActivatableServiceObserver serviceObserver;

    @Autowired
    private JaegerExporterService jaegerService;

    @Autowired
    private PrometheusExporterService prometheusExporterService;

    @Autowired
    private OtlpMetricsExporterService otlpMetricsExporterService;

    private Map<String, Boolean> expectedServiceStates;

    @BeforeEach
    void Setup() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.influx.enabled", true);
            props.setProperty("inspectit.exporters.metrics.prometheus.enabled", ExporterEnabledState.ENABLED);
            props.setProperty("inspectit.exporters.tracing.jaeger.enabled", ExporterEnabledState.ENABLED);
            props.setProperty("inspectit.exporters.tracing.jaeger.endpoint", "http://localhost:14250/api/traces");
            props.setProperty("inspectit.exporters.tracing.jaeger.protocol", TransportProtocol.GRPC);
        });

        expectedServiceStates = new HashMap<String, Boolean>() {{
            put(jaegerService.getName(), true);
            put(prometheusExporterService.getName(), true);
            put(otlpMetricsExporterService.getName(), false);
        }};
    }

    @Test
    @DirtiesContext
    void verifyStatesHaveBeenObserved() {
        assertThat(jaegerService.isEnabled()).isTrue();
        assertExpectedServices();

        // update properties, check if the update gets observed
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.jaeger.enabled", ExporterEnabledState.DISABLED);
        });
        expectedServiceStates.put(jaegerService.getName(), false);
        assertExpectedServices();
    }

    @Test
    @DirtiesContext
    void verifyStateUpdatesGetObserved() {
        assertExpectedServices();

        //Update Props 1
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.jaeger.enabled", ExporterEnabledState.DISABLED);
        });
        expectedServiceStates.put(jaegerService.getName(), false);
        assertExpectedServices();

        //Update Props 2
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.jaeger.enabled", ExporterEnabledState.ENABLED);
        });
        expectedServiceStates.put(jaegerService.getName(), true);
        assertExpectedServices();

        //Update Props 3 - Wrong input
        try {
            updateProperties(props -> {
                props.setProperty("inspectit.exporters.tracing.jaeger.protocol", TransportProtocol.HTTP_PROTOBUF);
            });
        } catch (Exception e) {
            //ignore
        }

        expectedServiceStates.put(jaegerService.getName(), false);
        assertExpectedServices();
    }

    void assertExpectedServices() {
        try {
            Map<String, Boolean> serviceStateMap = serviceObserver.getServiceStateMap();

            for (String serviceName : expectedServiceStates.keySet()) {
                assertThat(serviceStateMap.get(serviceName)).isEqualTo(expectedServiceStates.get(serviceName));
            }
        } catch (Exception e) {
            //ignore
        }
    }
}
