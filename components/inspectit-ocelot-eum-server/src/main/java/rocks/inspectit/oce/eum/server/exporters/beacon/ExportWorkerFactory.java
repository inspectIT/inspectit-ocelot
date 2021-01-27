package rocks.inspectit.oce.eum.server.exporters.beacon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.concurrent.BlockingQueue;

/**
 * Factory class for creating and managing {@link ExportWorker}s. In future, workers may be reused instead of created
 * each time.
 */
@Slf4j
@Component
public class ExportWorkerFactory {

    /**
     * The rest templated shared and used by all {@link ExportWorker}s.
     */
    private RestTemplate restTemplate;

    /**
     * The HTTP target to send the beacons to.
     */
    private URI exportTargetUrl;

    @Autowired
    private EumServerConfiguration configuration;

    /**
     * Initializes the required fields.
     */
    @PostConstruct
    public void initialize() {
        restTemplate = new RestTemplateBuilder().build();

        exportTargetUrl = URI.create(configuration.getExporters().getBeacons().getHttp().getEndpointUrl());
    }

    /**
     * Returns a {@link ExportWorker} for exporting the given beacon buffer.
     *
     * @param beaconBuffer the buffer to export
     *
     * @return a {@link ExportWorker} that can be used
     */
    public ExportWorker getWorker(BlockingQueue<Beacon> beaconBuffer) {
        return new ExportWorker(beaconBuffer);
    }

    /**
     * The exporters, which do the actual exporting and sending.
     */
    class ExportWorker implements Runnable {

        /**
         * The buffer to export.
         */
        private final BlockingQueue<Beacon> buffer;

        /**
         * Constructor.
         *
         * @param buffer the buffer to export
         */
        private ExportWorker(BlockingQueue<Beacon> buffer) {
            this.buffer = buffer;
        }

        @Override
        public void run() {
            log.debug("Exporting {} beacons via HTTP.", buffer.size());
            ResponseEntity<Void> response = restTemplate.postForEntity(exportTargetUrl, buffer, Void.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                log.warn("Exporting HTTP beacons failed with status {}.", response.getStatusCode());
            }
        }
    }
}
