package rocks.inspectit.ocelot.core.selfmonitoring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.exporter.OtlpMetricsExporterService;
import rocks.inspectit.ocelot.core.exporter.OtlpTraceExporterService;
import rocks.inspectit.ocelot.core.exporter.PrometheusExporterService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DynamicallyActivatableServiceObserverIntTest extends SpringTestBase {

    @Autowired
    private DynamicallyActivatableServiceObserver serviceObserver;

    @Autowired
    private OtlpTraceExporterService otlpService;

    @Autowired
    private PrometheusExporterService prometheusExporterService;

    @Autowired
    private OtlpMetricsExporterService otlpMetricsExporterService;

    private Map<String, Boolean> expectedServiceStates;

    @BeforeEach
    void Setup() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.influx.enabled", ExporterEnabledState.ENABLED);
            props.setProperty("inspectit.exporters.metrics.prometheus.enabled", ExporterEnabledState.ENABLED);
            props.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.ENABLED);
            props.setProperty("inspectit.exporters.tracing.otlp.endpoint", "http://localhost:4318/v1/metrics");
            props.setProperty("inspectit.exporters.tracing.otlp.protocol", TransportProtocol.HTTP_PROTOBUF);
        });

        expectedServiceStates = new HashMap<String, Boolean>() {{
            put(otlpService.getName(), true);
            put(prometheusExporterService.getName(), true);
            put(otlpMetricsExporterService.getName(), false);
        }};
    }

    @Test
    @DirtiesContext
    void verifyStatesHaveBeenObserved() {
        assertThat(otlpService.isEnabled()).isTrue();
        assertExpectedServices();

        // update properties, check if the update gets observed
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.DISABLED);
        });
        expectedServiceStates.put(otlpService.getName(), false);
        assertExpectedServices();
    }

    @Test
    @DirtiesContext
    void verifyStateUpdatesGetObserved() {
        assertExpectedServices();

        //Update Props 1
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.DISABLED);
        });
        expectedServiceStates.put(otlpService.getName(), false);
        assertExpectedServices();

        //Update Props 2
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.ENABLED);
        });
        expectedServiceStates.put(otlpService.getName(), true);
        assertExpectedServices();

        //Update Props 3 - Wrong input
        try {
            updateProperties(props -> {
                props.setProperty("inspectit.exporters.tracing.otlp.protocol", TransportProtocol.HTTP_THRIFT);
            });
        } catch (Exception e) {
            //ignore
        }

        expectedServiceStates.put(otlpService.getName(), false);
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
