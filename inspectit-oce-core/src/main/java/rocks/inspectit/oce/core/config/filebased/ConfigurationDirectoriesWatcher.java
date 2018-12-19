package rocks.inspectit.oce.core.config.filebased;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.service.ActivationConfigCondition;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Service for automatically watching all registered {@link DirectoryPropertySource}s.
 * If any changes are detected, the configuration gets reloaded into the {@link InspectitEnvironment}.
 */
@Service
@Slf4j
@ActivationConfigCondition("config.fileBased.watch")
public class ConfigurationDirectoriesWatcher {

    /**
     * Defines how often is the watch service polled.
     */
    private static final int POLL_FREQUENCY_MS = 500;

    @Autowired
    InspectitEnvironment env;

    @Autowired
    ScheduledExecutorService executor;

    private WatchService ws = null;
    private ScheduledFuture<?> watchingTask = null;

    private Map<WatchKey, DirectoryPropertySource> watchKeyToPropertySourceMap = new HashMap<>();

    @PostConstruct
    private void init() {
        log.info("Starting config directory watch service..");
        try {
            ws = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            log.error("Unable to create watch service", e);
            return;
        }
        env.readPropertySources(propertySources ->
                propertySources.stream()
                        .filter(ps -> ps instanceof DirectoryPropertySource)
                        .map(ps -> (DirectoryPropertySource) ps)
                        .forEach(ps -> {
                            Path dir = ps.getRootDir();
                            try {
                                WatchKey key = dir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                                watchKeyToPropertySourceMap.put(key, ps);
                            } catch (IOException e) {
                                log.error("Unable to register dir " + dir.toString() + " for watching", e);
                            }
                        })
        );
        if (watchKeyToPropertySourceMap.isEmpty()) {
            log.info("Terminating config directory watch service as no valid directory was registered");
            stopWatchService();
        } else {
            startWatcherTask();
        }
    }

    @PreDestroy
    private void stopWatchService() {
        if (ws != null) {
            log.info("Stopping config directory watch service.");
            try {
                if (watchingTask != null) {
                    watchingTask.cancel(true);
                }
                ws.close();
                ws = null;
            } catch (IOException e) {
                log.error("Unable to close watch service", e);
            }
        }
    }

    private void startWatcherTask() {
        watchingTask = executor.scheduleWithFixedDelay(() -> {
            try {
                WatchKey key = ws.poll();
                if (key != null) {
                    env.updatePropertySources(propertySources -> {
                        WatchKey currentKey = key;
                        while (currentKey != null) {
                            DirectoryPropertySource dps = watchKeyToPropertySourceMap.get(currentKey);
                            log.info("reloading " + dps.getRootDir().toString());
                            dps.reload(propertySources);
                            currentKey.pollEvents();
                            currentKey.reset();

                            currentKey = ws.poll();
                        }
                    });
                }
            } catch (ClosedWatchServiceException e) {
                //this exception is expected
            } catch (Exception e) {
                log.error("Error checking configs directories for updates", e);
            }
        }, 0, POLL_FREQUENCY_MS, TimeUnit.MILLISECONDS);
    }
}
