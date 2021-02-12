package rocks.inspectit.ocelot.file.versioning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.events.ConfigurationPromotionEvent;
import rocks.inspectit.ocelot.events.WorkspaceChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExternalChangeDetectorTest {

    @Mock
    FileManager fileManager;

    @Mock
    ApplicationEventPublisher publisher;

    @Mock
    RevisionAccess liveRevision;

    @Mock
    RevisionAccess workspaceRevision;

    ExternalChangeDetector detector;

    @BeforeEach
    void setup() {
        when(fileManager.getLiveRevision()).thenReturn(liveRevision);
        when(fileManager.getWorkspaceRevision()).thenReturn(workspaceRevision);
        when(liveRevision.getRevisionId()).thenReturn("initialLive");
        when(workspaceRevision.getRevisionId()).thenReturn("initialWork");

        detector = new ExternalChangeDetector(fileManager, publisher);
        detector.init();
    }

    @Nested
    class CheckForUpdates {

        @Test
        public void noChange() {
            detector.checkForUpdates();
            verifyZeroInteractions(publisher);
        }

        @Test
        public void internalWorkspaceChange() {
            when(workspaceRevision.getRevisionId()).thenReturn("newWorkspace");
            detector.workspaceChanged(new WorkspaceChangedEvent(this, workspaceRevision));

            detector.checkForUpdates();

            verifyZeroInteractions(publisher);
        }

        @Test
        public void externalWorkspaceChange() {
            when(workspaceRevision.getRevisionId()).thenReturn("newWorkspace");

            detector.checkForUpdates();

            ArgumentCaptor<WorkspaceChangedEvent> eventCaptor = ArgumentCaptor.forClass(WorkspaceChangedEvent.class);
            verify(publisher).publishEvent(eventCaptor.capture());
            WorkspaceChangedEvent event = eventCaptor.getValue();
            assertThat(event.getWorkspaceRevision()).isSameAs(workspaceRevision);
        }

        @Test
        public void internalLiveChange() {
            when(liveRevision.getRevisionId()).thenReturn("newLive");
            detector.configurationPromoted(new ConfigurationPromotionEvent(this, liveRevision));

            detector.checkForUpdates();

            verifyZeroInteractions(publisher);
        }

        @Test
        public void externalLiveChange() {
            when(liveRevision.getRevisionId()).thenReturn("newLive");

            detector.checkForUpdates();

            ArgumentCaptor<ConfigurationPromotionEvent> eventCaptor = ArgumentCaptor.forClass(ConfigurationPromotionEvent.class);
            verify(publisher).publishEvent(eventCaptor.capture());
            ConfigurationPromotionEvent event = eventCaptor.getValue();
            assertThat(event.getLiveRevision()).isSameAs(liveRevision);
        }

    }
}
