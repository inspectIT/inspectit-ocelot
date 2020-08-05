package rocks.inspectit.ocelot.core.config.propertysources.file;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.bootstrap.AgentManager;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.config.FileBasedConfigSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for automatically polling all registered {@link DirectoryPropertySource}s.
 * If any changes are detected, the configuration gets reloaded into the {@link InspectitEnvironment}.
 */
@Service
@Slf4j
public class ConfigurationDirectoriesPoller extends DynamicallyActivatableService {

    @Autowired
    InspectitEnvironment env;

    @Autowired
    ScheduledExecutorService executor;

    private ScheduledFuture<?> pollingTask = null;

    private List<DirectoryPropertySourceChangeChecker> filePollers;

    public ConfigurationDirectoriesPoller() {
        super("config.fileBased");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        FileBasedConfigSettings fileBased = conf.getConfig().getFileBased();
        return fileBased.isWatch();
    }

    @Override
    protected boolean doEnable(InspectitConfig conf) {
        log.info("Starting config directory polling service..");
        env.readPropertySources(propertySources ->
                filePollers = propertySources.stream()
                        .filter(ps -> ps instanceof DirectoryPropertySource)
                        .map(ps -> (DirectoryPropertySource) ps)
                        .map(ps -> createCheckerForDirectory(ps))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList())
        );
        if (filePollers.isEmpty()) {
            log.info("No directories to watch registered, terminating poll service.");
            return false;
        } else {
            long freqMS = conf.getConfig().getFileBased().getFrequency().toMillis();
            pollingTask = executor.scheduleWithFixedDelay(this::pollDirectories, freqMS, freqMS, TimeUnit.MILLISECONDS);
            return true;
        }
    }

    private void pollDirectories() {
        if (!AgentManager.isInitialized()) {
            log.debug("Skipping update of directory property sources because the agent is not initialized, yet.");
            return;
        }

        for (val poller : filePollers) {
            poller.checkForChangesAndReloadIfRequired();
        }
    }


    @Override
    protected boolean doDisable() {
        if (pollingTask != null) {
            log.info("Stopping config directory polling service.");
            pollingTask.cancel(true);
            for (val poller : filePollers) {
                try {
                    poller.destroy();
                } catch (Exception e) {
                    log.error("Error destroying poller!", e);
                }
            }
            filePollers.clear();
        }
        return true;
    }

    private Optional<DirectoryPropertySourceChangeChecker> createCheckerForDirectory(DirectoryPropertySource dps) {
        val result = new DirectoryPropertySourceChangeChecker();
        try {
            result.init(dps);
            return Optional.of(result);
        } catch (Exception e) {
            log.error("Error creating watcher for directory!", e);
            return Optional.empty();
        }
    }

    private class DirectoryPropertySourceChangeChecker {

        private FileAlterationObserver observer;
        private DirectoryPropertySource dirToWatch;
        private boolean anyFileChanged = false;

        public void init(DirectoryPropertySource dirToWatch) throws Exception {
            this.dirToWatch = dirToWatch;
            File path = dirToWatch.getRootDir().toFile();
            observer = new FileAlterationObserver(path);
            observer.addListener(new FileAlterationListenerAdaptor() {
                @Override
                public void onFileCreate(File file) {
                    anyFileChanged = true;
                }

                @Override
                public void onFileChange(File file) {
                    anyFileChanged = true;
                }

                @Override
                public void onFileDelete(File file) {
                    anyFileChanged = true;
                }
            });
            observer.initialize();
        }

        public void checkForChangesAndReloadIfRequired() {
            observer.checkAndNotify();
            if (anyFileChanged) {
                log.info("reloading {}", dirToWatch.getRootDir().toString());
                env.updatePropertySources(mps -> dirToWatch.reload(mps));
                anyFileChanged = false;
            }
        }

        public void destroy() throws Exception {
            observer.destroy();
        }
    }
}
