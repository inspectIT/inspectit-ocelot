package rocks.inspectit.ocelot.file.versioning;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.events.ConfigurationPromotionEvent;
import rocks.inspectit.ocelot.events.WorkspaceChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors the workspace and the live branch for changes.
 * If the ID changes without any event (e.g. an external commit happened on the file system),
 * a corresponding change event will be fired.
 */
@Component
public class ExternalChangeDetector {

    private String latestWorkspaceId;

    private String latestLiveId;

    private FileManager fileManager;

    private ScheduledExecutorService executor;

    private ApplicationEventPublisher publisher;

    private ScheduledFuture<?> updateTask;

    @VisibleForTesting
    @Autowired
    ExternalChangeDetector(FileManager fileManager, ScheduledExecutorService executor, ApplicationEventPublisher publisher) {
        this.fileManager = fileManager;
        this.executor = executor;
        this.publisher = publisher;
    }

    @PostConstruct
    @VisibleForTesting
    void init() {
        latestWorkspaceId = fileManager.getWorkspaceRevision().getRevisionId();
        latestLiveId = fileManager.getLiveRevision().getRevisionId();
        updateTask = executor.scheduleAtFixedRate(this::checkForUpdates, 1, 1, TimeUnit.SECONDS);
    }

    @PreDestroy
    private void destroy() {
        updateTask.cancel(false);
    }

    @VisibleForTesting
    synchronized void checkForUpdates() {
        RevisionAccess currentLiveRevision = fileManager.getLiveRevision();
        if (!currentLiveRevision.getRevisionId().equals(latestLiveId)) {
            publisher.publishEvent(new ConfigurationPromotionEvent(this, currentLiveRevision));
        }

        RevisionAccess currentWorkspaceRevision = fileManager.getWorkspaceRevision();
        if (!currentWorkspaceRevision.getRevisionId().equals(latestWorkspaceId)) {
            publisher.publishEvent(new WorkspaceChangedEvent(this, currentWorkspaceRevision));
        }
    }

    /**
     * Event listener to prevent duplicate firing of promotion events
     *
     * @param event
     */
    @EventListener
    synchronized void configurationPromoted(ConfigurationPromotionEvent event) {
        latestLiveId = event.getLiveRevision().getRevisionId();
    }

    /**
     * Event listener to prevent duplicate firing of workspace change events
     *
     * @param event
     */
    @EventListener
    synchronized void workspaceChanged(WorkspaceChangedEvent event) {
        latestWorkspaceId = event.getWorkspaceRevision().getRevisionId();
    }

}
