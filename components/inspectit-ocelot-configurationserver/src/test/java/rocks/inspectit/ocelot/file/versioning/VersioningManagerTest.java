package rocks.inspectit.ocelot.file.versioning;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.RemoteConfigurationsSettings;
import rocks.inspectit.ocelot.config.model.RemoteRepositorySettings;
import rocks.inspectit.ocelot.error.exceptions.SelfPromotionNotAllowedException;
import rocks.inspectit.ocelot.events.WorkspaceChangedEvent;
import rocks.inspectit.ocelot.file.FileTestBase;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.versioning.model.Promotion;
import rocks.inspectit.ocelot.file.versioning.model.SimpleDiffEntry;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceVersion;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VersioningManagerTest extends FileTestBase {

    /**
     * For convenience: if this field is not null, it will be used as working directory during tests.
     * Note: the specified directory is CLEANED before each run, thus, if you have files there, they will be gone ;)
     */
    public static final String TEST_DIRECTORY = null;

    private VersioningManager versioningManager;

    private Authentication authentication;

    private ApplicationEventPublisher eventPublisher;

    private InspectitServerSettings settings;

    @BeforeEach
    public void beforeEach() throws IOException {
        if (TEST_DIRECTORY == null) {
            tempDirectory = Files.createTempDirectory("ocelot");
        } else {
            tempDirectory = Paths.get(TEST_DIRECTORY);
            FileUtils.cleanDirectory(tempDirectory.toFile());
        }

        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        eventPublisher = mock(ApplicationEventPublisher.class);
        settings = mock(InspectitServerSettings.class);
        when(settings.getMailSuffix()).thenReturn("test.com");

        versioningManager = new VersioningManager(tempDirectory, () -> authentication, eventPublisher, settings);

        System.out.println("Test data in: " + tempDirectory.toString());
    }

    @AfterEach
    public void afterEach() throws IOException {
        Git git = (Git) ReflectionTestUtils.getField(versioningManager, "git");
        if (git != null) {
            git.close();
        }

        if (TEST_DIRECTORY == null) {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    @Nested
    class Init {

        @Test
        public void initAndStageFiles() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml", "untracked-file");

            boolean before = Files.exists(tempDirectory.resolve(".git"));

            versioningManager.initialize();

            boolean after = Files.exists(tempDirectory.resolve(".git"));
            int count = versioningManager.getCommitCount();

            boolean clean = versioningManager.isClean();
            assertThat(clean).isTrue();
            assertThat(before).isFalse();
            assertThat(after).isTrue();
            assertThat(count).isEqualTo(1); // Initializing Git repo + committing agent mappings happens in one commit
        }

        @Test
        public void multipleCalls() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml", "untracked-file");

            boolean initFirst = Files.exists(tempDirectory.resolve(".git"));
            int firstCount = versioningManager.getCommitCount();
            assertThat(initFirst).isFalse();
            assertThat(firstCount).isZero();

            versioningManager.initialize();

            boolean cleanFirst = versioningManager.isClean();
            boolean initSecond = Files.exists(tempDirectory.resolve(".git"));
            int secondCount = versioningManager.getCommitCount();
            assertThat(initSecond).isTrue();
            assertThat(cleanFirst).isTrue();
            assertThat(secondCount).isEqualTo(1); // Initializing Git repo + committing agent mappings happens in one commit

            versioningManager.initialize();

            boolean cleanSecond = versioningManager.isClean();
            int thirdCount = versioningManager.getCommitCount();
            assertThat(cleanSecond).isTrue();
            assertThat(thirdCount).isEqualTo(1);
        }

        @Test
        public void externalChanges() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml", "untracked-file");

            boolean initFirst = Files.exists(tempDirectory.resolve(".git"));
            int firstCount = versioningManager.getCommitCount();
            assertThat(initFirst).isFalse();
            assertThat(firstCount).isZero();

            versioningManager.initialize();

            boolean cleanFirst = versioningManager.isClean();
            boolean initSecond = Files.exists(tempDirectory.resolve(".git"));
            int secondCount = versioningManager.getCommitCount();
            assertThat(initSecond).isTrue();
            assertThat(cleanFirst).isTrue();
            assertThat(secondCount).isEqualTo(1); // Initializing Git repo + committing agent mappings happens in one commit

            // edit file
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=content");

            versioningManager.initialize();

            boolean cleanSecond = versioningManager.isClean();
            int thirdCount = versioningManager.getCommitCount();
            assertThat(cleanSecond).isTrue();
            assertThat(thirdCount).isEqualTo(2);
        }
    }

    @Nested
    class CommitAllChanges {

        @Test
        public void commitFile() throws GitAPIException, IOException {
            versioningManager.initialize();
            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            versioningManager.commitAllChanges("test");

            assertThat(versioningManager.getCommitCount()).isEqualTo(2);
            assertThat(versioningManager.isClean()).isTrue();

            verify(eventPublisher).publishEvent(any(WorkspaceChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void amendCommit() throws GitAPIException, IOException {
            versioningManager.initialize();
            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            versioningManager.commitAllChanges("test");

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=content");

            versioningManager.commitAllChanges("another commit");

            assertThat(versioningManager.getCommitCount()).isEqualTo(2);
            assertThat(versioningManager.isClean()).isTrue();

            verify(eventPublisher, times(2)).publishEvent(any(WorkspaceChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void noAmendAfterTimeout() throws GitAPIException, IOException {
            versioningManager.initialize();
            versioningManager.setAmendTimeout(-2000);
            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            versioningManager.commitAllChanges("test");

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=content");

            versioningManager.commitAllChanges("another commit");

            assertThat(versioningManager.getCommitCount()).isEqualTo(3);
            assertThat(versioningManager.isClean()).isTrue();

            verify(eventPublisher, times(2)).publishEvent(any(WorkspaceChangedEvent.class));
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void noChanges() throws GitAPIException, IOException {
            versioningManager.initialize();
            versioningManager.setAmendTimeout(-2000);

            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            versioningManager.commitAllChanges("test");
            verify(eventPublisher).publishEvent(any(WorkspaceChangedEvent.class));
            versioningManager.commitAllChanges("no change");

            assertThat(versioningManager.getCommitCount()).isEqualTo(2);
            assertThat(versioningManager.isClean()).isTrue();

            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        public void invalidState() throws GitAPIException, IOException {
            versioningManager.initialize();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            Git git = (Git) ReflectionTestUtils.getField(versioningManager, "git");
            git.checkout().setName(Branch.LIVE.getBranchName()).call();

            assertThatIllegalStateException().isThrownBy(() -> versioningManager.commitAllChanges("test"))
                    .withMessage("The workspace branch is currently not checked out. Ensure your working directory is in a correct state!");

            assertThat(versioningManager.getCommitCount()).isOne();
            assertThat(versioningManager.isClean()).isFalse();

            verifyNoMoreInteractions(eventPublisher);
        }
    }

    @Nested
    class IsClean {

        @Test
        public void cleanRepository() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml", "untracked-file");
            versioningManager.initialize();

            boolean result = versioningManager.isClean();

            assertThat(result).isTrue();
        }

        @Test
        public void modificationChanges() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            versioningManager.initialize();

            boolean before = versioningManager.isClean();

            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME + "=content");

            boolean after = versioningManager.isClean();

            assertThat(before).isTrue();
            assertThat(after).isFalse();
        }

        @Test
        public void untrackedChanges() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            versioningManager.initialize();

            boolean before = versioningManager.isClean();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            boolean after = versioningManager.isClean();

            assertThat(before).isTrue();
            assertThat(after).isFalse();
        }

        @Test
        public void ignoredFile() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, "ignored-file");
            versioningManager.initialize();

            boolean result = versioningManager.isClean();

            assertThat(result).isTrue();
        }
    }

    @Nested
    class Destroy {

        @Test
        public void callDestroy() {
            Git gitMock = mock(Git.class);
            ReflectionTestUtils.setField(versioningManager, "git", gitMock);

            versioningManager.destroy();

            verify(gitMock).close();
            verifyNoMoreInteractions(gitMock);
        }
    }

    @Nested
    class GetLatestCommit {

        @Test
        public void emptyCommit() throws GitAPIException, IOException {
            versioningManager.initialize();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit(Branch.WORKSPACE);

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get().getFullMessage()).isEqualTo("Initializing Git repository");
        }

        @Test
        public void commitExists() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.initialize();

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit(Branch.WORKSPACE);

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get()
                    .getFullMessage()).isEqualTo("Initializing Git repository using existing working directory");
        }

        @Test
        public void getLatestCommit() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=content");
            versioningManager.commitAllChanges("new commit");

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit(Branch.WORKSPACE);

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get().getFullMessage()).isEqualTo("new commit");
        }

        @Test
        public void getLatestCommitFromLive() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=content");
            versioningManager.commitAllChanges("new commit");

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit(Branch.LIVE);

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get()
                    .getFullMessage()).isEqualTo("Initializing Git repository using existing working directory");
        }
    }

    @Nested
    class GetWorkspaceDiff {

        @Test
        public void getDiff() throws IOException, GitAPIException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_no_change.yml");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_added.yml");
            Files.delete(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml"));
            versioningManager.commitAllChanges("new commit");

            WorkspaceDiff result = versioningManager.getWorkspaceDiffWithoutContent();

            assertThat(result.getEntries()).containsExactlyInAnyOrder(SimpleDiffEntry.builder()
                    .file("/file_added.yml")
                    .type(DiffEntry.ChangeType.ADD)
                    .authors(Collections.singletonList("user"))
                    .build(), SimpleDiffEntry.builder()
                    .file("/file_modified.yml")
                    .type(DiffEntry.ChangeType.MODIFY)
                    .authors(Collections.singletonList("user"))
                    .build(), SimpleDiffEntry.builder()
                    .file("/file_removed.yml")
                    .type(DiffEntry.ChangeType.DELETE)
                    .authors(Collections.singletonList("user"))
                    .build());
            assertThat(result.getLiveCommitId()).isNotEqualTo(result.getWorkspaceCommitId());
        }

        @Test
        public void getDiffWithContent() throws IOException, GitAPIException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_no_change.yml");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=new content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_added.yml");
            Files.delete(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml"));
            versioningManager.commitAllChanges("new commit");

            WorkspaceDiff result = versioningManager.getWorkspaceDiff(true);

            assertThat(result.getEntries()).containsExactlyInAnyOrder(SimpleDiffEntry.builder()
                    .file("/file_added.yml")
                    .type(DiffEntry.ChangeType.ADD)
                    .newContent("")
                    .authors(Collections.singletonList("user"))
                    .build(), SimpleDiffEntry.builder()
                    .file("/file_modified.yml")
                    .type(DiffEntry.ChangeType.MODIFY)
                    .oldContent("")
                    .newContent("new content")
                    .authors(Collections.singletonList("user"))
                    .build(), SimpleDiffEntry.builder()
                    .file("/file_removed.yml")
                    .type(DiffEntry.ChangeType.DELETE)
                    .oldContent("content")
                    .authors(Collections.singletonList("user"))
                    .build());
            assertThat(result.getLiveCommitId()).isNotEqualTo(result.getWorkspaceCommitId());
        }

        @Test
        public void getDiffById() throws IOException, GitAPIException {
            // initial
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            // commit 1
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=new content");
            versioningManager.commitAllChanges("commit");
            ObjectId workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId();
            ObjectId liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId();
            // commit 2
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=another content");
            versioningManager.commitAllChanges("commit");
            ObjectId latestWorkspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId();

            WorkspaceDiff resultFirst = versioningManager.getWorkspaceDiff(true, liveId, workspaceId);
            WorkspaceDiff resultSecond = versioningManager.getWorkspaceDiff(true, liveId, latestWorkspaceId);

            assertThat(resultFirst.getEntries()).containsExactlyInAnyOrder(SimpleDiffEntry.builder()
                    .file("/file_modified.yml")
                    .type(DiffEntry.ChangeType.MODIFY)
                    .oldContent("")
                    .newContent("new content")
                    .authors(Collections.singletonList("user"))
                    .build());
            assertThat(resultFirst.getLiveCommitId()).isEqualTo(liveId.name());
            assertThat(resultFirst.getWorkspaceCommitId()).isEqualTo(workspaceId.name());

            assertThat(resultSecond.getEntries()).containsExactlyInAnyOrder(SimpleDiffEntry.builder()
                    .file("/file_modified.yml")
                    .type(DiffEntry.ChangeType.MODIFY)
                    .oldContent("")
                    .newContent("another content")
                    .authors(Collections.singletonList("user"))
                    .build());
            assertThat(resultSecond.getLiveCommitId()).isEqualTo(liveId.name());
            assertThat(resultSecond.getWorkspaceCommitId()).isEqualTo(latestWorkspaceId.name());
        }
    }

    @Nested
    class PromoteFile {

        @Test
        public void promoteEverything() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_no_change.yml");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_added.yml");
            Files.delete(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml"));
            versioningManager.commitAllChanges("new commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            Promotion promotion = new Promotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList(
                    AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, "/file_added.yml", "/file_modified.yml", "/file_removed.yml")
            );

            versioningManager.promote(promotion, true);

            WorkspaceDiff diff = versioningManager.getWorkspaceDiffWithoutContent();

            assertThat(diff.getEntries()).isEmpty();
        }

        @Test
        public void partialPromotion() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_no_change.yml");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_added.yml");
            Files.delete(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml"));
            versioningManager.commitAllChanges("new commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            Promotion promotion = new Promotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml", "/file_removed.yml"));

            versioningManager.promote(promotion, true);

            WorkspaceDiff diff = versioningManager.getWorkspaceDiffWithoutContent();

            assertThat(diff.getEntries()).containsExactlyInAnyOrder(SimpleDiffEntry.builder()
                    .file("/file_added.yml")
                    .type(DiffEntry.ChangeType.ADD)
                    .authors(Collections.singletonList("user"))
                    .build());
        }

        @Test
        public void multiplePromotions() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_no_change.yml");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_added.yml");
            Files.delete(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml"));
            versioningManager.commitAllChanges("new commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            Promotion promotion = new Promotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            // first promotion
            versioningManager.promote(promotion, true);

            // second
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=new_content");
            versioningManager.commitAllChanges("another commit");

            liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            promotion = new Promotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            // second promotion
            versioningManager.promote(promotion, true);

            // diff
            WorkspaceDiff diff = versioningManager.getWorkspaceDiffWithoutContent();

            assertThat(diff.getEntries()).containsExactlyInAnyOrder(SimpleDiffEntry.builder()
                    .file("/file_added.yml")
                    .type(DiffEntry.ChangeType.ADD)
                    .authors(Collections.singletonList("user"))
                    .build(), SimpleDiffEntry.builder()
                    .file("/file_removed.yml")
                    .type(DiffEntry.ChangeType.DELETE)
                    .authors(Collections.singletonList("user"))
                    .build());
        }

        @Test
        public void differentLiveBranch() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_no_change.yml");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_added.yml");
            Files.delete(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_removed.yml"));
            versioningManager.commitAllChanges("new commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            Promotion promotion = new Promotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            versioningManager.promote(promotion, true);

            Promotion secondPromotion = new Promotion();
            secondPromotion.setLiveCommitId(liveId);
            secondPromotion.setWorkspaceCommitId(workspaceId);
            secondPromotion.setFiles(Arrays.asList("/file_added.yml"));

            assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> versioningManager.promote(secondPromotion, true))
                    .withMessage("Live branch has been modified. The provided promotion definition is out of sync.");
        }

        @Test
        public void promotionWithModification() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_A");
            versioningManager.commitAllChanges("commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            Promotion promotion = new Promotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_B");
            versioningManager.commitAllChanges("commit");

            versioningManager.promote(promotion, true);

            // diff live -> workspace
            WorkspaceDiff diff = versioningManager.getWorkspaceDiffWithoutContent();

            assertThat(diff.getEntries()).containsExactlyInAnyOrder(SimpleDiffEntry.builder()
                    .file("/file_modified.yml")
                    .type(DiffEntry.ChangeType.MODIFY)
                    .authors(Collections.singletonList("user"))
                    .build());
            assertThat(versioningManager.getLiveRevision()
                    .readConfigurationFile("file_modified.yml")).hasValue("content_A");
            assertThat(versioningManager.getWorkspaceRevision()
                    .readConfigurationFile("file_modified.yml")).hasValue("content_B");
        }

        @Test
        public void selfPromotionProtectionEnabled() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_A");
            versioningManager.commitAllChanges("commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            Promotion promotion = new Promotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_B");
            versioningManager.commitAllChanges("commit");

            doReturn("promoter").when(authentication).getName();
            versioningManager.promote(promotion, false);

            assertThat(versioningManager.getLiveRevision()
                    .readConfigurationFile("file_modified.yml")).hasValue("content_A");
            assertThat(versioningManager.getWorkspaceRevision()
                    .readConfigurationFile("file_modified.yml")).hasValue("content_B");
        }

        @Test
        public void selfPromotionPrevented() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_A");
            versioningManager.commitAllChanges("commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            Promotion promotion = new Promotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            RevisionAccess live = versioningManager.getLiveRevision();

            assertThatThrownBy(() -> versioningManager.promote(promotion, false)).isInstanceOf(SelfPromotionNotAllowedException.class);

            assertThat(versioningManager.getLiveRevision().getRevisionId()).isEqualTo(live.getRevisionId());
        }
    }

    @Nested
    class GetRevisionById {

        @Test
        public void getRevision() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=a");
            versioningManager.initialize();

            WorkspaceDiff workspaceDiff = versioningManager.getWorkspaceDiffWithoutContent();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=b");
            versioningManager.commitAllChanges("commit");

            RevisionAccess result = versioningManager.getRevisionById(ObjectId.fromString(workspaceDiff.getWorkspaceCommitId()));

            Optional<String> fileContent = result.readConfigurationFile("file.yml");
            assertThat(fileContent).hasValue("a");
        }
    }

    @Nested
    class GetCurrentAuthor {

        @Test
        void systemUserOnSystemOperationUsed() {
            VersioningManager vm = new VersioningManager(Paths.get(""), () -> null, (event) -> {
            }, InspectitServerSettings.builder().mailSuffix("@test.com").build());
            assertThat(vm.getCurrentAuthor()).isEqualTo(VersioningManager.GIT_SYSTEM_AUTHOR);
        }

        @Test
        void activeUserUsed() {
            VersioningManager vm = new VersioningManager(Paths.get(""), () -> authentication, (event) -> {
            }, InspectitServerSettings.builder().mailSuffix("@test.com").build());
            assertThat(vm.getCurrentAuthor().getName()).isEqualTo(authentication.getName());
        }

        @Test
        void mailCreatedFromConfig() {
            VersioningManager vm = new VersioningManager(Paths.get(""), () -> authentication, (event) -> {
            }, InspectitServerSettings.builder().mailSuffix("@test.com").build());
            assertThat(vm.getCurrentAuthor().getEmailAddress()).isEqualTo(authentication.getName() + "@" + "test.com");
        }

        @Test
        void mailReadFromLdap() {
            InetOrgPerson mockInetOrgPerson = mock(InetOrgPerson.class);
            when(mockInetOrgPerson.getMail()).thenReturn("foo@bar.com");
            when(authentication.getPrincipal()).thenReturn(mockInetOrgPerson);

            VersioningManager vm = new VersioningManager(Paths.get(""), () -> authentication, (event) -> {
            }, InspectitServerSettings.builder().mailSuffix("@test.com").build());

            assertThat(vm.getCurrentAuthor().getEmailAddress()).isEqualTo("foo@bar.com");
        }
    }

    @Nested
    class FillInAuthors {

        private void promote(String... files) throws Exception {
            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            Promotion promotion = new Promotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList(files));
            versioningManager.promote(promotion, true);
        }

        private void buildDummyHistory() throws Exception {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/c0_file_a=content");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/c0_file_b.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/c1_file_a.yml=content");
            doReturn("user_a").when(authentication).getName();
            versioningManager.commitAllChanges("new commit");
            promote("/c0_file_a.yml", "/c0_file_b.yml", "/c1_file_a.yml");

            Files.delete(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/c0_file_b.yml"));
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/c1_file_a.yml=newFileContent");

            doReturn("user_b").when(authentication).getName();
            versioningManager.commitAllChanges("new commit");
            promote("/c0_file_b.yml", "/c1_file_a.yml");
        }

        @Test
        void fileAddedInMostRecentChange() throws Exception {
            buildDummyHistory();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml");
            doReturn("creating_user").when(authentication).getName();
            versioningManager.commitAllChanges("new commit");

            SimpleDiffEntry diff = SimpleDiffEntry.builder()
                    .file("new_file.yml")
                    .type(DiffEntry.ChangeType.ADD)
                    .build();

            List<String> modifyingAuthors = versioningManager.getModifyingAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(modifyingAuthors).containsExactlyInAnyOrder("creating_user");
        }

        @Test
        void agentMappingsAddedInMostRecentChange() throws Exception {
            buildDummyHistory();

            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            doReturn("creating_user").when(authentication).getName();
            versioningManager.commitAllChanges("new commit");

            SimpleDiffEntry diff = SimpleDiffEntry.builder()
                    .file(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME)
                    .type(DiffEntry.ChangeType.ADD)
                    .build();

            List<String> modifyingAuthors = versioningManager.getModifyingAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(modifyingAuthors).containsExactlyInAnyOrder("creating_user");
        }

        @Test
        void fileAddedAndModifiedBeforeLastPromotion() throws Exception {
            buildDummyHistory();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/independent_file.yml");
            doReturn("creating_user").when(authentication).getName();
            versioningManager.commitAllChanges("new commit");
            promote("/independent_file.yml");

            doReturn("editing_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml=content_modified");
            versioningManager.commitAllChanges("new commit");

            doReturn("independent_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/independent_file.yml=modified");
            versioningManager.commitAllChanges("new commit");
            promote("/independent_file.yml");

            SimpleDiffEntry diff = SimpleDiffEntry.builder()
                    .file("new_file.yml")
                    .type(DiffEntry.ChangeType.ADD)
                    .build();

            List<String> modifyingAuthors = versioningManager.getModifyingAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(modifyingAuthors).containsExactlyInAnyOrder("creating_user", "editing_user");
        }

        @Test
        void fileModified() throws Exception {
            buildDummyHistory();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/independent_file.yml");
            doReturn("initial_creating_user").when(authentication).getName();
            versioningManager.commitAllChanges("com1");
            promote("/independent_file.yml", "/new_file.yml");

            doReturn("editing_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml=content_modified");
            versioningManager.commitAllChanges("com2");

            doReturn("deleting_user").when(authentication).getName();
            Files.delete(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml"));
            versioningManager.commitAllChanges("com3");

            doReturn("creating_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml=new_initial_content");
            versioningManager.commitAllChanges("com4");

            doReturn("independent_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/independent_file.yml=modified");
            versioningManager.commitAllChanges("com5");
            promote("/independent_file.yml");

            doReturn("second_editing_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml=content_modified");
            versioningManager.commitAllChanges("com6");

            SimpleDiffEntry diff = SimpleDiffEntry.builder()
                    .file("new_file.yml")
                    .type(DiffEntry.ChangeType.MODIFY)
                    .build();

            List<String> modifyingAuthors = versioningManager.getModifyingAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(modifyingAuthors).containsExactlyInAnyOrder("creating_user", "second_editing_user");
        }

        @Test
        void agentMappingsModified() throws Exception {
            buildDummyHistory();

            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            doReturn("editing_user").when(authentication).getName();
            versioningManager.commitAllChanges("com1");
            promote(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);

            doReturn("editing_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME + "=modified");
            versioningManager.commitAllChanges("com2");

            SimpleDiffEntry diff = SimpleDiffEntry.builder()
                    .file(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME)
                    .type(DiffEntry.ChangeType.MODIFY)
                    .build();

            List<String> modifyingAuthors = versioningManager.getModifyingAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(modifyingAuthors).containsExactlyInAnyOrder("editing_user");
        }

        @Test
        void fileModifiedWithChangesUndone() throws Exception {
            buildDummyHistory();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml=initialContent");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/independent_file.yml");
            doReturn("initial_creating_user").when(authentication).getName();
            versioningManager.commitAllChanges("com1");
            promote("/independent_file.yml", "/new_file.yml");

            doReturn("editing_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml=newContent");
            versioningManager.commitAllChanges("com2");

            doReturn("undoing_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml=initialContent");
            versioningManager.commitAllChanges("com3");

            doReturn("last_editing_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml=superNewContent");
            versioningManager.commitAllChanges("com4");

            SimpleDiffEntry diff = SimpleDiffEntry.builder()
                    .file("new_file.yml")
                    .type(DiffEntry.ChangeType.MODIFY)
                    .build();

            List<String> modifyingAuthors = versioningManager.getModifyingAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(modifyingAuthors).containsExactlyInAnyOrder("last_editing_user");
        }

        @Test
        void fileDeleted() throws Exception {
            buildDummyHistory();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/independent_file.yml");
            doReturn("initial_creating_user").when(authentication).getName();
            versioningManager.commitAllChanges("com1");
            promote("/independent_file.yml", "/new_file.yml");

            doReturn("editing_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml=content_modified");
            versioningManager.commitAllChanges("com2");

            doReturn("independent_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/independent_file.yml=modified");
            versioningManager.commitAllChanges("com3");
            promote("/independent_file.yml");

            doReturn("deleting_user").when(authentication).getName();
            Files.delete(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml"));
            versioningManager.commitAllChanges("com4");
            ObjectId deleting_com4 = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId();

            SimpleDiffEntry diff = SimpleDiffEntry.builder()
                    .file("new_file.yml")
                    .type(DiffEntry.ChangeType.DELETE)
                    .build();

            List<String> modifyingAuthors = versioningManager.getModifyingAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(modifyingAuthors).containsExactlyInAnyOrder("deleting_user");
        }

        @Test
        void fileDeletionAmended() throws Exception {
            buildDummyHistory();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml");
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/independent_file.yml");
            doReturn("deleting_user").when(authentication).getName();
            versioningManager.commitAllChanges("com1");
            promote("/independent_file.yml", "/new_file.yml");

            doReturn("deleting_user").when(authentication).getName();
            Files.delete(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/new_file.yml"));
            versioningManager.commitAllChanges("com2");

            doReturn("independent_user").when(authentication).getName();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/independent_file.yml=modified");
            versioningManager.commitAllChanges("com3");
            promote("/independent_file.yml");

            SimpleDiffEntry diff = SimpleDiffEntry.builder()
                    .file("new_file.yml")
                    .type(DiffEntry.ChangeType.DELETE)
                    .build();

            List<String> modifyingAuthors = versioningManager.getModifyingAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(modifyingAuthors).containsExactlyInAnyOrder("deleting_user");
        }
    }

    @Nested
    class ListVersions {

        private String prevCommitId;

        @BeforeEach
        private void createCommits() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=1", AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_b.yml=1");
            versioningManager.initialize();
            prevCommitId = versioningManager.getWorkspaceRevision().getRevisionId();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=2");
            versioningManager.commitAsExternalChange();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_b.yml=3");
            versioningManager.commitAllChanges("second commit");
        }

        @Test
        public void test() throws GitAPIException, IOException {
            List<WorkspaceVersion> result = versioningManager.listWorkspaceVersions();

            assertThat(result).flatExtracting(WorkspaceVersion::getMessage)
                    .containsExactly("second commit", "Staging and committing of external changes", "Initializing Git repository using existing working directory");
            assertThat(result).flatExtracting(WorkspaceVersion::getAuthor).containsExactly("user", "System", "System");
        }
    }

    @Nested
    class MergeSourceBranch {

        private Path directoryOne;

        private Path directoryTwo;

        private Git repositoryOne;

        private Git repositoryTwo;

        @BeforeEach
        private void createRepository() throws IOException, GitAPIException {
            directoryOne = Files.createTempDirectory("git-test-remote");
            directoryTwo = Files.createTempDirectory("git-test-remote");

            repositoryOne = Git.init().setDirectory(directoryOne.toFile()).call();
            repositoryTwo = Git.init().setDirectory(directoryTwo.toFile()).call();
        }

        @AfterEach
        private void afterEach() throws IOException {
            repositoryOne.close();
            repositoryTwo.close();

            FileUtils.deleteDirectory(directoryOne.toFile());
            FileUtils.deleteDirectory(directoryTwo.toFile());
        }

        @Test
        public void missingRemoteConfiguration() {
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> versioningManager.mergeSourceBranch())
                    .withMessage("The remote configuration settings must not be null.");
        }

        @Test
        public void missingSourceRepository() {
            when(settings.getRemoteConfigurations()).thenReturn(mock(RemoteConfigurationsSettings.class));

            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> versioningManager.mergeSourceBranch())
                    .withMessage("Source repository settings must not be null.");
        }

        @Test
        public void syncTagMissing() throws URISyntaxException, GitAPIException, IOException {
            RemoteRepositorySettings repositorySettings = RemoteRepositorySettings.builder()
                    .branchName("branch")
                    .remoteName("remote")
                    .gitRepositoryUri(new URIish(directoryTwo.toString() + "/.git"))
                    .build();
            RemoteConfigurationsSettings configurationsSettings = RemoteConfigurationsSettings.builder()
                    .enabled(true)
                    .pullRepository(repositorySettings)
                    .build();
            when(settings.getRemoteConfigurations()).thenReturn(configurationsSettings);

            // prepare Git
            repositoryTwo.commit().setAllowEmpty(true).setMessage("init").call();
            repositoryTwo.branchCreate().setName("branch").call();

            // initialize Git
            versioningManager.initialize();
            Git git = (Git) ReflectionTestUtils.getField(versioningManager, "git");

            // fetch "remote" branches
            RemoteConfigurationManager remoteConfigurationManager = (RemoteConfigurationManager) ReflectionTestUtils.getField(versioningManager, "remoteConfigurationManager");
            remoteConfigurationManager.fetchSourceBranch(repositorySettings);

            // ////////////////////////
            // get state before
            Optional<RevCommit> before = versioningManager.getLatestCommit(Branch.WORKSPACE);
            List<Ref> tagsBefore = git.tagList().call();

            // merge
            versioningManager.mergeSourceBranch();

            // ////////////////////////
            // get state after
            Optional<RevCommit> after = versioningManager.getLatestCommit(Branch.WORKSPACE);
            List<Ref> tagsAfter = git.tagList().call();

            ObjectId branchCommitId = git.branchList().setContains("branch").call().get(0).getObjectId();
            RevCommit branchCommit = versioningManager.getCommit(branchCommitId);
            RevCommit tagCommit = versioningManager.getCommit(tagsAfter.get(0).getObjectId());

            // assert result
            assertThat(before.get()).isEqualTo(after.get());
            assertThat(tagsBefore).isEmpty();
            assertThat(tagsAfter).extracting(Ref::getName).containsExactly("refs/tags/ocelot-sync-base");
            assertThat(tagCommit).isEqualTo(branchCommit);
        }

        @Test
        public void mergeBranch() throws URISyntaxException, GitAPIException, IOException {
            RemoteRepositorySettings repositorySettings = RemoteRepositorySettings.builder()
                    .branchName("branch")
                    .remoteName("remote")
                    .gitRepositoryUri(new URIish(directoryTwo.toString() + "/.git"))
                    .build();
            RemoteConfigurationsSettings configurationsSettings = RemoteConfigurationsSettings.builder()
                    .enabled(true)
                    .pullAtStartup(true)
                    .pullRepository(repositorySettings)
                    .build();
            when(settings.getRemoteConfigurations()).thenReturn(configurationsSettings);

            // prepare Git
            repositoryTwo.commit().setAllowEmpty(true).setMessage("init").call();
            repositoryTwo.branchCreate().setName("branch").call();
            repositoryTwo.checkout().setName("branch").call();

            // initialize Git
            versioningManager.initialize();
            Git git = (Git) ReflectionTestUtils.getField(versioningManager, "git");

            // add some new files to the remote
            File fileDirectory = new File(directoryTwo.toFile(), AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER);
            fileDirectory.mkdirs();
            new File(fileDirectory, "dummy.txt").createNewFile();
            repositoryTwo.add().addFilepattern(".").call();
            repositoryTwo.commit().setMessage("dummy").call();

            // fetch "remote" branches
            RemoteConfigurationManager remoteConfigurationManager = (RemoteConfigurationManager) ReflectionTestUtils.getField(versioningManager, "remoteConfigurationManager");
            remoteConfigurationManager.fetchSourceBranch(repositorySettings);

            // ////////////////////////
            // get state before
            ObjectId commitIdBefore = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId();
            ObjectId liveIdBefore = versioningManager.getLatestCommit(Branch.LIVE).get().getId();
            ObjectId tagCommitIdBefore = versioningManager.getCommit(git.tagList().call().get(0).getObjectId()).getId();

            // merge
            versioningManager.mergeSourceBranch();

            // ////////////////////////
            // get state after
            RevCommit commitAfter = versioningManager.getLatestCommit(Branch.WORKSPACE).get();
            RevCommit liveCommit = versioningManager.getLatestCommit(Branch.LIVE).get();
            ObjectId tagCommitIdAfter = versioningManager.getCommit(git.tagList().call().get(0).getObjectId()).getId();
            ObjectId branchCommitId = git.getRepository().exactRef("refs/heads/branch").getObjectId();

            // assert result
            assertThat(commitIdBefore).isNotEqualTo(commitAfter.getId()); // workspace got a new commit
            assertThat(tagCommitIdBefore).isNotEqualTo(tagCommitIdAfter); // the tag has been moved to a different commit
            assertThat(branchCommitId).isEqualTo(tagCommitIdAfter);  // the tag was set to the latest commit on the "branch" remote
            assertThat(commitAfter.getParents()).extracting(RevObject::getId) // the latest workspace commit has two parent commits: the old one and the latest commit on "branch" (=tagged commit)
                    .containsExactlyInAnyOrder(commitIdBefore, tagCommitIdAfter);
            assertThat(liveCommit.getParents()).extracting(RevObject::getId) // the latest live commit has two parent commits: the old live one and the latest workspace one
                    .containsExactlyInAnyOrder(liveIdBefore, commitAfter.getId());
        }

        @Test
        public void initialConfigurationSyncOneRemoteWithoutLocalFiles() throws URISyntaxException, GitAPIException, IOException {
            // ////////////////////////
            // test without local files --> should only reset to remote
            doInitialConfigurationSyncOneRemote(false);

            ObjectId targetCommitIdBeforeInitialization = repositoryOne.getRepository()
                    .exactRef("refs/heads/remote")
                    .getObjectId();
            ObjectId liveCommitId = versioningManager.getLatestCommit(Branch.LIVE).get().getId();

            assertThat(liveCommitId).isEqualTo(targetCommitIdBeforeInitialization); // live should be set to remote repo
            assertThat(Files.list(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER))).extracting(Path::getFileName)
                    .extracting(Path::toString)
                    .containsExactlyInAnyOrder("from_remote.yml"); // should exactly contain content of remote repo
        }

        @Test
        public void initialConfigurationSyncOneRemoteWithLocalFiles() throws URISyntaxException, GitAPIException, IOException {
            // ////////////////////////
            // test with local files --> should overwrite those local files that exist in remote and keep others
            doInitialConfigurationSyncOneRemote(true);

            ObjectId targetCommitIdBeforeInitialization = repositoryOne.getRepository()
                    .exactRef("refs/heads/remote")
                    .getObjectId();
            RevCommit liveCommit = versioningManager.getLatestCommit(Branch.LIVE).get();
            ObjectId parentCommitId = liveCommit.getParent(0).getId();

            assertThat(parentCommitId).isEqualTo(targetCommitIdBeforeInitialization); // live should add one commit to remote repo
            assertThat(Files.list(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER))).extracting(Path::getFileName)
                    .extracting(Path::toString)
                    .containsExactlyInAnyOrder("from_remote.yml", "from_local.yml"); // should contain content of remote repo + from_local.yml
            assertThat(new String(Files.readAllBytes(tempDirectory.resolve("files/from_remote.yml")))).isEmpty(); // file from remote should have remote content (empty)
        }

        private void doInitialConfigurationSyncOneRemote(boolean localFiles) throws URISyntaxException, GitAPIException, IOException {
            RemoteRepositorySettings remoteRepositorySettings = RemoteRepositorySettings.builder()
                    .branchName("remote")
                    .remoteName("remote")
                    .gitRepositoryUri(new URIish(directoryOne.toString() + "/.git"))
                    .useForcePush(false)
                    .build();
            RemoteConfigurationsSettings configurationsSettings = RemoteConfigurationsSettings.builder()
                    .enabled(true)
                    .pullAtStartup(true)
                    .initialConfigurationSync(true)
                    .pullRepository(remoteRepositorySettings)
                    .pushRepository(remoteRepositorySettings)
                    .build();
            when(settings.getRemoteConfigurations()).thenReturn(configurationsSettings);

            // prepare remote Git
            addEmptyFile(repositoryOne, "from_remote.yml");
            repositoryOne.add().addFilepattern("files/from_remote.yml").call();
            repositoryOne.commit().setMessage("init remote").call();
            repositoryOne.branchCreate().setName("remote").call();
            repositoryOne.checkout().setName("remote").call();

            if (localFiles) {
                // add some local files that should (not) be overridden by initial synchronization
                createTestFiles("files/from_local.yml=local", "files/from_remote.yml=local");
            }

            // initialize Git
            versioningManager.initialize();
        }

        @Test
        public void initialConfigurationSyncTwoRemotes() throws URISyntaxException, GitAPIException, IOException {
            RemoteRepositorySettings pullRepositorySettings = RemoteRepositorySettings.builder()
                    .branchName("pull")
                    .remoteName("pull")
                    .gitRepositoryUri(new URIish(directoryOne.toString() + "/.git"))
                    .build();
            RemoteRepositorySettings pushRepositorySettings = RemoteRepositorySettings.builder()
                    .branchName("push")
                    .remoteName("push")
                    .gitRepositoryUri(new URIish(directoryTwo.toString() + "/.git"))
                    .useForcePush(false)
                    .build();
            RemoteConfigurationsSettings configurationsSettings = RemoteConfigurationsSettings.builder()
                    .enabled(true)
                    .pullAtStartup(true)
                    .initialConfigurationSync(true)
                    .pullRepository(pullRepositorySettings)
                    .pushRepository(pushRepositorySettings)
                    .build();
            when(settings.getRemoteConfigurations()).thenReturn(configurationsSettings);

            // prepare pull Git
            addEmptyFile(repositoryOne, "from_pull.yml");
            repositoryOne.add().addFilepattern("files/from_pull.yml").call();
            repositoryOne.commit().setMessage("init pull").call();
            repositoryOne.branchCreate().setName("pull").call();
            repositoryOne.checkout().setName("pull").call();

            // prepare push Git
            addEmptyFile(repositoryTwo, "empty.yml");
            repositoryTwo.add().addFilepattern("files/empty.yml").call();
            repositoryTwo.commit().setMessage("init push").call();
            repositoryTwo.branchCreate().setName("push").call();
            repositoryTwo.checkout().setName("push").call();

            // add local file
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/from_local.yml=existed_before_git_init");

            // initialize Git
            versioningManager.initialize();
            Git git = (Git) ReflectionTestUtils.getField(versioningManager, "git");

            // ////////////////////////
            // check state after initial synchronization
            ObjectId targetCommitId = repositoryTwo.getRepository().exactRef("refs/heads/push").getObjectId();
            ObjectId liveCommitId = versioningManager.getLatestCommit(Branch.LIVE).get().getId();

            assertThat(liveCommitId).isEqualTo(targetCommitId); // live and target should be synchronized now (live pushed to target)
            assertThat(Files.list(tempDirectory.resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER))).extracting(Path::getFileName)
                    .extracting(Path::toString)
                    .containsExactlyInAnyOrder("from_pull.yml", "from_local.yml"); // should contain all files from pull repo and local

            // ////////////////////////
            // add files and push (without force)

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/foobar.yml=content");
            versioningManager.commitAllChanges("add foobar");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            Promotion promotion = new Promotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/foobar.yml"));

            PromotionResult promotionResult = versioningManager.promote(promotion, true);

            assertThat(promotionResult).isEqualTo(PromotionResult.OK); // OK means no synchronization error
        }

        private void addEmptyFile(Git repo, String filename) throws IOException {
            Path filesDir = repo.getRepository()
                    .getWorkTree()
                    .toPath()
                    .resolve(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER);
            Files.createDirectories(filesDir);
            Files.createFile(filesDir.resolve(filename));
        }
    }
}
