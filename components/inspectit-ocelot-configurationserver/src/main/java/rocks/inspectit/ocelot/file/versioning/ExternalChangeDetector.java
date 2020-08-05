package rocks.inspectit.ocelot.file.versioning;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.events.ConfigurationPromotionEvent;
import rocks.inspectit.ocelot.events.WorkspaceChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;

import javax.annotation.PostConstruct;

/**
 * Monitors the workspace and the live branch for changes.
 * If the ID changes without any event (e.g. an external commit happened on the file system),
 * a corresponding change event will be fired.
 */
@Component
public class ExternalChangeDetector {

    /**
     * The last known workspace commit ID.
     * If this is different from the actual latest ID, an event will be fired.
     */
    private String latestWorkspaceId;

    /**
     * The last known live commit ID.
     * If this is different from the actual latest ID, an event will be fired.
     */
    private String latestLiveId;

    private FileManager fileManager;

    private ApplicationEventPublisher publisher;

    @VisibleForTesting
    @Autowired
    ExternalChangeDetector(FileManager fileManager, ApplicationEventPublisher publisher) {
        this.fileManager = fileManager;
        this.publisher = publisher;
    }

    @PostConstruct
    @VisibleForTesting
    void init() {
        latestWorkspaceId = fileManager.getWorkspaceRevision().getRevisionId();
        latestLiveId = fileManager.getLiveRevision().getRevisionId();
    }

    @VisibleForTesting
    @Scheduled(fixedDelay = 5000)
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
