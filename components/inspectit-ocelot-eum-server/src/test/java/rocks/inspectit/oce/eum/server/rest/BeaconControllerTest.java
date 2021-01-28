package rocks.inspectit.oce.eum.server.rest;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.beacon.processor.CompositeBeaconProcessor;
import rocks.inspectit.oce.eum.server.exporters.beacon.BeaconHttpExporter;
import rocks.inspectit.oce.eum.server.metrics.BeaconMetricManager;
import rocks.inspectit.oce.eum.server.metrics.SelfMonitoringMetricManager;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeaconControllerTest {

    @InjectMocks
    private BeaconController controller;

    @Mock
    private BeaconMetricManager beaconMetricManager;

    @Mock
    private CompositeBeaconProcessor beaconProcessor;

    @Mock
    private SelfMonitoringMetricManager selfMonitoringService;

    @Mock
    private BeaconHttpExporter beaconHttpExporter;

    @Nested
    public class BeaconPost {

        @Test
        public void successful() {
            when(beaconProcessor.process(any())).then(i -> i.getArguments()[0]);
            when(beaconMetricManager.processBeacon(any())).thenReturn(true);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("key", "value");

            ResponseEntity result = controller.beaconPost(map);

            ArgumentCaptor<Beacon> beaconCaptor = ArgumentCaptor.forClass(Beacon.class);
            verify(beaconMetricManager).processBeacon(beaconCaptor.capture());
            verify(beaconProcessor).process(any());
            verify(selfMonitoringService).record("beacons_received", 1, Collections.singletonMap("is_error", "false"));
            verifyNoMoreInteractions(beaconMetricManager, beaconProcessor, selfMonitoringService);

            assertThat(result).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.OK);
            assertThat(beaconCaptor.getValue().toMap())
                    .hasSize(1)
                    .containsEntry("key", "value");
        }

        @Test
        public void notSuccessful() {
            when(beaconProcessor.process(any())).then(i -> i.getArguments()[0]);
            when(beaconMetricManager.processBeacon(any())).thenReturn(false);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

            ResponseEntity result = controller.beaconPost(map);

            verify(beaconMetricManager).processBeacon(any());
            verify(beaconProcessor).process(any());
            verify(selfMonitoringService).record("beacons_received", 1, Collections.singletonMap("is_error", "true"));
            verifyNoMoreInteractions(beaconMetricManager, beaconProcessor, selfMonitoringService);

            assertThat(result).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.OK);
        }

    }

    @Nested
    public class BeaconGet {

        @Test
        public void successful() {
            when(beaconProcessor.process(any())).then(i -> i.getArguments()[0]);
            when(beaconMetricManager.processBeacon(any())).thenReturn(true);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("key", "value");

            ResponseEntity result = controller.beaconGet(map);

            ArgumentCaptor<Beacon> beaconCaptor = ArgumentCaptor.forClass(Beacon.class);
            verify(beaconMetricManager).processBeacon(beaconCaptor.capture());
            verify(beaconProcessor).process(any());
            verify(selfMonitoringService).record("beacons_received", 1, Collections.singletonMap("is_error", "false"));
            verifyNoMoreInteractions(beaconMetricManager, beaconProcessor, selfMonitoringService);

            assertThat(result).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.OK);
            assertThat(beaconCaptor.getValue().toMap())
                    .hasSize(1)
                    .containsEntry("key", "value");
        }

        @Test
        public void notSuccessful() {
            when(beaconProcessor.process(any())).then(i -> i.getArguments()[0]);
            when(beaconMetricManager.processBeacon(any())).thenReturn(false);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

            ResponseEntity result = controller.beaconGet(map);

            verify(beaconMetricManager).processBeacon(any());
            verify(beaconProcessor).process(any());
            verify(selfMonitoringService).record("beacons_received", 1, Collections.singletonMap("is_error", "true"));
            verifyNoMoreInteractions(beaconMetricManager, beaconProcessor, selfMonitoringService);

            assertThat(result).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.OK);
        }

    }
}