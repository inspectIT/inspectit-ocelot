package rocks.inspectit.oce.eum.server.exporters.beacon;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconHttpExporterSettings;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.metrics.SelfMonitoringMetricManager;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Factory class for creating and managing {@link ExportWorker}s. In future, workers may be reused instead of created
 * each time.
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "inspectit-eum-server.beacons.http.enabled", havingValue = "true")
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

    @Autowired
    private SelfMonitoringMetricManager selfMonitoring;

    /**
     * Initializes the required fields.
     */
    @PostConstruct
    public void initialize() {
        BeaconHttpExporterSettings settings = configuration.getExporters().getBeacons().getHttp();

        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

        if (StringUtils.isNotBlank(settings.getUsername())) {
            restTemplateBuilder = restTemplateBuilder.basicAuthentication(settings.getUsername(), settings.getPassword());
        }

        restTemplate = restTemplateBuilder.build();
        exportTargetUrl = URI.create(settings.getEndpointUrl());
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

            boolean successful = true;
            Stopwatch stopwatch = Stopwatch.createUnstarted();
            try {
                stopwatch.start();
                ResponseEntity<Void> response = restTemplate.postForEntity(exportTargetUrl, buffer, Void.class);
                stopwatch.stop();

                if (response.getStatusCode() != HttpStatus.OK) {
                    successful = false;
                    log.warn("Exporting HTTP beacons failed with status {}.", response.getStatusCode());
                }
            } catch (RestClientException e) {
                successful = false;
                log.error("Exporting HTTP beacons failed.", e);
            }

            ImmutableMap<String, String> tagMap = ImmutableMap.of("exporter", "http", "is_error", String.valueOf(!successful));
            selfMonitoring.record("beacons_export_batch", buffer.size(), tagMap);
            selfMonitoring.record("beacons_export", stopwatch.elapsed(TimeUnit.MILLISECONDS), tagMap);
        }
    }
}
