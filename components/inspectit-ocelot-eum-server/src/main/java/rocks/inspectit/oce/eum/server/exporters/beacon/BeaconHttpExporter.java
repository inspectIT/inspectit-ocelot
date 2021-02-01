package rocks.inspectit.oce.eum.server.exporters.beacon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconHttpExporterSettings;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;

/**
 * Exporter for exporting Boomerang beacons to a HTTP endpoint.
 */
@Component
@Slf4j
@ConditionalOnProperty(value = "inspectit-eum-server.beacons.http.enabled", havingValue = "true")
public class BeaconHttpExporter {

    /**
     * Executor used to execute the {@link ExportWorkerFactory.ExportWorker}s.
     */
    private ScheduledExecutorService executor;

    /**
     * The current buffer which will be filled by beacons to export.
     */
    private BlockingQueue<Beacon> beaconBuffer;

    /**
     * Monitor for buffer swapping.
     */
    private final Object bufferMonitor = new Object();

    /**
     * It is tried to export the buffer when this many elements are in it.
     */
    private int flushThreshold;

    @Autowired
    private EumServerConfiguration configuration;

    @Autowired
    private ExportWorkerFactory workerFactory;

    /**
     * Initializes the exporter and starting the scheduled flush interval.
     */
    @PostConstruct
    public void initialize() {
        BeaconHttpExporterSettings settings = configuration.getExporters().getBeacons().getHttp();
        log.info("Starting beacon export via HTTP to endpoint: {}", settings.getEndpointUrl());

        flushThreshold = (int) (settings.getMaxBatchSize() * 0.8D);

        executor = Executors.newScheduledThreadPool(settings.getWorkerThreads());

        long flushInterval = settings.getFlushInterval().toMillis();
        executor.scheduleWithFixedDelay(() -> {
            BlockingQueue<Beacon> beacons = swapBuffer();
            if (!beacons.isEmpty()) {
                executor.submit(workerFactory.getWorker(beacons));
            }
        }, flushInterval, flushInterval, TimeUnit.MILLISECONDS);

        beaconBuffer = new ArrayBlockingQueue<>(settings.getMaxBatchSize());
    }

    /**
     * Shutting down the exporter and its executor.
     */
    @PreDestroy
    public void destroy() throws InterruptedException {
        log.info("Shutting down HTTP beacon exporter..");

        executor.shutdown();

        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            log.warn("Couldn't shut down HTTP beacon exporter correctly..");
        }
    }

    /**
     * This method replaces the currently used buffer with a new and empty one. The previously used buffer
     * is then returned.
     *
     * @return the previously used buffer
     */
    private synchronized BlockingQueue<Beacon> swapBuffer() {
        int maxBatchSize = configuration.getExporters().getBeacons().getHttp().getMaxBatchSize();

        BlockingQueue<Beacon> currentBuffer = beaconBuffer;
        beaconBuffer = new ArrayBlockingQueue<>(maxBatchSize);
        return currentBuffer;
    }

    /**
     * Exports the given beacon. The beacon will not be exported immediately, but put in a buffer. The beacon will be
     * exported in a batch with multiple beacons once the buffer is full or after a specific amount of time.
     *
     * @param beacon the beacon to export
     */
    public void export(Beacon beacon) {
        boolean success = beaconBuffer.offer(beacon);

        if (!success) {
            log.warn("Skipping beacon export via HTTP because there is no space in the export buffer.");
        }

        if (beaconBuffer.size() >= flushThreshold) {
            synchronized (bufferMonitor) {
                if (beaconBuffer.size() >= flushThreshold) {
                    executor.submit(workerFactory.getWorker(swapBuffer()));
                }
            }
        }
    }
}
