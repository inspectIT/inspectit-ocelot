package rocks.inspectit.oce.eum.server.exporters.beacon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconHttpExporterSettings;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeaconHttpExporterTest {

    @InjectMocks
    private BeaconHttpExporter exporter;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EumServerConfiguration configuration;

    @Mock
    private ExportWorkerFactory workerFactory;

    private BeaconHttpExporterSettings exporterSettings;

    @BeforeEach
    public void beforeEach() {
        exporterSettings = new BeaconHttpExporterSettings();
        exporterSettings.setEnabled(true);
        exporterSettings.setEndpointUrl("http//localhost:1000");
        exporterSettings.setFlushInterval(Duration.ofSeconds(1));
        exporterSettings.setMaxBatchSize(10);
        exporterSettings.setWorkerThreads(3);

        when(configuration.getExporters().getBeacons().getHttp()).thenReturn(exporterSettings);
    }

    @Nested
    public class Initialize {

        @Test
        public void initialize() {
            exporter.initialize();

            ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) ReflectionTestUtils.getField(exporter, "executor");
            assertThat(executor).isNotNull();
            assertThat(executor.getCorePoolSize()).isEqualTo(3);
            BlockingQueue<Beacon> beaconBuffer = (BlockingQueue) ReflectionTestUtils.getField(exporter, "beaconBuffer");
            assertThat(beaconBuffer).isNotNull();
        }
    }

    @Nested
    public class Destroy {

        @Test
        public void destroyExporter() throws InterruptedException {
            exporter.initialize();

            ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) ReflectionTestUtils.getField(exporter, "executor");
            assertThat(executor).isNotNull();

            assertThat(executor.isShutdown()).isFalse();
            exporter.destroy();
            assertThat(executor.isShutdown()).isTrue();
        }
    }

    @Nested
    public class Export {

        @Mock
        private ScheduledExecutorService executor;

        @Mock
        private Beacon beaconA, beaconB;

        @Test
        public void isDisabled() throws InterruptedException {
            exporterSettings.setEnabled(false);
            exporter.initialize();
            ReflectionTestUtils.setField(exporter, "executor", executor);

            exporter.export(beaconA);

            verifyZeroInteractions(executor);
        }

        @Test
        public void succesfulExport() throws InterruptedException {
            exporterSettings.setMaxBatchSize(3);
            exporter.initialize();
            ReflectionTestUtils.setField(exporter, "executor", executor);
            BlockingQueue<Beacon> beaconBuffer = (BlockingQueue) ReflectionTestUtils.getField(exporter, "beaconBuffer");
            when(workerFactory.getWorker(any())).thenReturn(mock(ExportWorkerFactory.ExportWorker.class));

            assertThat(beaconBuffer).isEmpty();

            // export first
            exporter.export(beaconA);

            assertThat(beaconBuffer).containsExactly(beaconA);
            verifyZeroInteractions(executor);

            //export second
            exporter.export(beaconB);

            assertThat(beaconBuffer).containsExactly(beaconA, beaconB);
            verify(executor).submit(any(ExportWorkerFactory.ExportWorker.class));
            verifyNoMoreInteractions(executor);

            BlockingQueue<Beacon> nextBeaconBuffer = (BlockingQueue) ReflectionTestUtils.getField(exporter, "beaconBuffer");
            assertThat(nextBeaconBuffer).isNotSameAs(beaconBuffer);
            assertThat(nextBeaconBuffer).isEmpty();
        }

    }
}