package rocks.inspectit.ocelot.file.versioning;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.ocelot.error.exceptions.SelfPromotionNotAllowedException;
import rocks.inspectit.ocelot.events.WorkspaceChangedEvent;
import rocks.inspectit.ocelot.file.FileTestBase;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.versioning.model.ConfigurationPromotion;
import rocks.inspectit.ocelot.file.versioning.model.SimpleDiffEntry;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VersioningManagerTest extends FileTestBase {

    /**
     * For convenience: if this field is not null, it will used as working directory during tests.
     * Note: the specified directory is CLEANED before each run, thus, if you have files there, they will be gone ;)
     */
    public static final String TEST_DIRECTORY = null;

    private VersioningManager versioningManager;

    private Authentication authentication;

    private ApplicationEventPublisher eventPublisher;

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

        versioningManager = new VersioningManager(tempDirectory, () -> authentication, eventPublisher, "test.com");

        System.out.println("Test data in: " + tempDirectory.toString());
    }

    @AfterEach
    public void afterEach() throws IOException {
        if (TEST_DIRECTORY == null) {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    @Nested
    class Init {

        @Test
        public void initAndStageFiles() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml", "untracked-file");

            boolean before = Files.exists(tempDirectory.resolve(".git"));

            versioningManager.initialize();

            boolean after = Files.exists(tempDirectory.resolve(".git"));
            int count = versioningManager.getCommitCount();

            boolean clean = versioningManager.isClean();
            assertThat(clean).isTrue();
            assertThat(before).isFalse();
            assertThat(after).isTrue();
            assertThat(count).isOne();
        }

        @Test
        public void multipleCalls() throws GitAPIException {
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
            assertThat(secondCount).isOne();

            versioningManager.initialize();

            boolean cleanSecond = versioningManager.isClean();
            int thirdCount = versioningManager.getCommitCount();
            assertThat(cleanSecond).isTrue();
            assertThat(thirdCount).isOne();
        }

        @Test
        public void externalChanges() throws GitAPIException {
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
            assertThat(secondCount).isOne();

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
        public void commitFile() throws GitAPIException {
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
        public void amendCommit() throws GitAPIException {
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
        public void noAmendAfterTimeout() throws GitAPIException {
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
        public void noChanges() throws GitAPIException {
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
        public void invalidState() throws GitAPIException {
            versioningManager.initialize();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            Git git = (Git) ReflectionTestUtils.getField(versioningManager, "git");
            git.checkout().setName(Branch.LIVE.getBranchName()).call();

            assertThatIllegalStateException().isThrownBy(() -> versioningManager.commitAllChanges("test"))
                    .withMessage("The workspace branch is currently not checked out. Ensure your working directory is in a correct state!");

            assertThat(versioningManager.getCommitCount()).isOne();
            assertThat(versioningManager.isClean()).isFalse();

            verifyZeroInteractions(eventPublisher);
        }
    }

    @Nested
    class IsClean {

        @Test
        public void cleanRepository() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME, AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml", "untracked-file");
            versioningManager.initialize();

            boolean result = versioningManager.isClean();

            assertThat(result).isTrue();
        }

        @Test
        public void modificationChanges() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            versioningManager.initialize();

            boolean before = versioningManager.isClean();

            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME + "=content");

            boolean after = versioningManager.isClean();

            assertThat(before).isTrue();
            assertThat(after).isFalse();
        }

        @Test
        public void untrackedChanges() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
            versioningManager.initialize();

            boolean before = versioningManager.isClean();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            boolean after = versioningManager.isClean();

            assertThat(before).isTrue();
            assertThat(after).isFalse();
        }

        @Test
        public void ignoredFile() throws GitAPIException {
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
        public void emptyCommit() throws GitAPIException {
            versioningManager.initialize();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit(Branch.WORKSPACE);

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get().getFullMessage()).isEqualTo("Initializing Git repository");
        }

        @Test
        public void commitExists() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.initialize();

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit(Branch.WORKSPACE);

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get()
                    .getFullMessage()).isEqualTo("Initializing Git repository using existing working directory");
        }

        @Test
        public void getLatestCommit() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=content");
            versioningManager.commitAllChanges("new commit");

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit(Branch.WORKSPACE);

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get().getFullMessage()).isEqualTo("new commit");
        }

        @Test
        public void getLatestCommitFromLive() throws GitAPIException {
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
    class PromoteConfiguration {

        @Test
        public void promoteEverything() throws GitAPIException, IOException {
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

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_added.yml", "/file_modified.yml", "/file_removed.yml"));

            versioningManager.promoteConfiguration(promotion, true);

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

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml", "/file_removed.yml"));

            versioningManager.promoteConfiguration(promotion, true);

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

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            // first promotion
            versioningManager.promoteConfiguration(promotion, true);

            // second
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=new_content");
            versioningManager.commitAllChanges("another commit");

            liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            // second promotion
            versioningManager.promoteConfiguration(promotion, true);

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

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            versioningManager.promoteConfiguration(promotion, true);

            ConfigurationPromotion secondPromotion = new ConfigurationPromotion();
            secondPromotion.setLiveCommitId(liveId);
            secondPromotion.setWorkspaceCommitId(workspaceId);
            secondPromotion.setFiles(Arrays.asList("/file_added.yml"));

            assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> versioningManager.promoteConfiguration(secondPromotion, true))
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

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_B");
            versioningManager.commitAllChanges("commit");

            versioningManager.promoteConfiguration(promotion, true);

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
        public void selfPromotionProtectionEnabled() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_A");
            versioningManager.commitAllChanges("commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_B");
            versioningManager.commitAllChanges("commit");

            doReturn("promoter").when(authentication).getName();
            versioningManager.promoteConfiguration(promotion, false);

            assertThat(versioningManager.getLiveRevision()
                    .readConfigurationFile("file_modified.yml")).hasValue("content_A");
            assertThat(versioningManager.getWorkspaceRevision()
                    .readConfigurationFile("file_modified.yml")).hasValue("content_B");
        }

        @Test
        public void selfPromotionPrevented() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_A");
            versioningManager.commitAllChanges("commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList("/file_modified.yml"));

            RevisionAccess live = versioningManager.getLiveRevision();

            assertThatThrownBy(() -> versioningManager.promoteConfiguration(promotion, false)).isInstanceOf(SelfPromotionNotAllowedException.class);

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
            }, "@test.com");
            assertThat(vm.getCurrentAuthor()).isEqualTo(VersioningManager.GIT_SYSTEM_AUTHOR);
        }

        @Test
        void activeUserUsed() {
            VersioningManager vm = new VersioningManager(Paths.get(""), () -> authentication, (event) -> {
            }, "@test.com");
            assertThat(vm.getCurrentAuthor().getName()).isEqualTo(authentication.getName());
        }

        @Test
        void mailCreatedFromConfig() {
            VersioningManager vm = new VersioningManager(Paths.get(""), () -> authentication, (event) -> {
            }, "@test.com");
            assertThat(vm.getCurrentAuthor().getEmailAddress()).isEqualTo(authentication.getName() + "@" + "test.com");
        }

        @Test
        void mailReadFromLdap() {
            InetOrgPerson mockInetOrgPerson = mock(InetOrgPerson.class);
            when(mockInetOrgPerson.getMail()).thenReturn("foo@bar.com");
            when(authentication.getPrincipal()).thenReturn(mockInetOrgPerson);

            VersioningManager vm = new VersioningManager(Paths.get(""), () -> authentication, (event) -> {
            }, "@test.com");

            assertThat(vm.getCurrentAuthor().getEmailAddress()).isEqualTo("foo@bar.com");
        }
    }

    @Nested
    class FillInAuthors {

        private void promote(String... files) throws Exception {
            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList(files));
            versioningManager.promoteConfiguration(promotion, true);
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

            versioningManager.fillInAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(diff.getAuthors()).containsExactlyInAnyOrder("creating_user");
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

            versioningManager.fillInAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(diff.getAuthors()).containsExactlyInAnyOrder("creating_user", "editing_user");
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

            versioningManager.fillInAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(diff.getAuthors()).containsExactlyInAnyOrder("creating_user", "second_editing_user");
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

            versioningManager.fillInAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(diff.getAuthors()).containsExactlyInAnyOrder("last_editing_user");
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

            versioningManager.fillInAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(diff.getAuthors()).containsExactlyInAnyOrder("deleting_user");
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

            versioningManager.fillInAuthors(diff, versioningManager.getLatestCommit(Branch.LIVE)
                    .get()
                    .getId(), versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId());

            assertThat(diff.getAuthors()).containsExactlyInAnyOrder("deleting_user");
        }
    }

    @Nested
    class ListVersions {

        private String prevCommitId;

        @BeforeEach
        private void createCommits() throws GitAPIException {
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
            assertThat(result).flatExtracting(WorkspaceVersion::getAuthor)
                    .containsExactly("user", "System", "System");

        }

    }
}