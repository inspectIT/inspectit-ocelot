package rocks.inspectit.oce.core.config.filebased;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.service.ActivationConfigCondition;
import rocks.inspectit.oce.core.config.service.ActivationConfigConditionMet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Service for automatically watching all registered {@link DirectoryPropertySource}s.
 * If any changes are detected, the configuration gets reloaded into the {@link InspectitEnvironment}.
 */
@Service
@Slf4j
@ActivationConfigCondition("config.fileBased.watch")
@Conditional(ActivationConfigConditionMet.class)
public class ConfigurationDirectoriesWatcher {

    @Autowired
    InspectitEnvironment env;

    private WatchService ws = null;

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
            startWatcherThread();
        }
    }

    @PreDestroy
    private void stopWatchService() {
        if (ws != null) {
            try {
                ws.close();
            } catch (IOException e) {
                log.error("Unable to close watch service", e);
                return;
            }
        }
    }

    private void startWatcherThread() {
        val watchingThread = new Thread(() -> {
            try {
                while (true) {
                    WatchKey key;
                    try {
                        key = ws.take();
                    } catch (InterruptedException x) {
                        log.info("Stopping watcher thread due to interrupt");
                        return;
                    }
                    env.updatePropertySources(propertySources -> {
                        WatchKey currentKey = key;
                        while (currentKey != null) {
                            currentKey.pollEvents(); //remove all events from watch key
                            DirectoryPropertySource dps = watchKeyToPropertySourceMap.get(key);
                            log.info("reloading " + dps.getRootDir().toString());
                            dps.reload(propertySources);
                            currentKey.reset();

                            currentKey = ws.poll();
                        }
                    });
                }
            } catch (ClosedWatchServiceException e) {
                log.info("Stopping watcher thread due to closed watch service");
            }
        });
        watchingThread.setDaemon(true);
        watchingThread.start();
    }
}
