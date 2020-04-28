package rocks.inspectit.ocelot.file.versioning;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.VersioningSettings;

import static org.mockito.Mockito.verify;

class VersioningManagerTest {

    @InjectMocks
    private VersioningManager versioningManager;

    @Mock
    private Git git;

    @BeforeEach
    public void beforeEach() {
        VersioningSettings versioningSettings = new VersioningSettings();
        versioningSettings.setGitUsername("ocelot");
        versioningSettings.setGitMail("ocelot@inspectit.rocks");
        InspectitServerSettings settings = new InspectitServerSettings();
        settings.setWorkingDirectory("/working-directory");
        settings.setVersioning(versioningSettings);

        versioningManager = new VersioningManager(settings);

        MockitoAnnotations.initMocks(this);
    }

    @Nested
    class Destroy {

        @Test
        public void callDestroy() {
            versioningManager.destroy();

            verify(git).close();
        }
    }
}