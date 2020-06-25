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
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.ocelot.file.FileTestBase;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.versioning.model.ConfigurationPromotion;
import rocks.inspectit.ocelot.file.versioning.model.SimpleDiffEntry;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

class VersioningManagerTest extends FileTestBase {

    /**
     * For convenience: if this field is not null, it will used as working directory during tests.
     * Note: the specified directory is CLEANED before each run, thus, if you have files there, they will be gone ;)
     */
    public static final String TEST_DIRECTORY = null;

    private VersioningManager versioningManager;

    private Authentication authentication;

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
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        versioningManager = new VersioningManager(tempDirectory, () -> authentication, eventPublisher);

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
    class Commit {

        @Test
        public void commitFile() throws GitAPIException {
            versioningManager.initialize();
            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            versioningManager.commit("test");

            assertThat(versioningManager.getCommitCount()).isEqualTo(2);
            assertThat(versioningManager.isClean()).isTrue();
        }

        @Test
        public void amendCommit() throws GitAPIException {
            versioningManager.initialize();
            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            versioningManager.commit("test");

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=content");

            versioningManager.commit("another commit");

            assertThat(versioningManager.getCommitCount()).isEqualTo(2);
            assertThat(versioningManager.isClean()).isTrue();
        }

        @Test
        public void noAmendAfterTimeout() throws GitAPIException {
            versioningManager.initialize();
            versioningManager.setAmendTimeout(-2000);
            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            versioningManager.commit("test");

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=content");

            versioningManager.commit("another commit");

            assertThat(versioningManager.getCommitCount()).isEqualTo(3);
            assertThat(versioningManager.isClean()).isTrue();
        }

        @Test
        public void noChanges() throws GitAPIException {
            versioningManager.initialize();
            versioningManager.setAmendTimeout(-2000);

            assertThat(versioningManager.getCommitCount()).isOne();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");

            versioningManager.commit("test");
            versioningManager.commit("no change");

            assertThat(versioningManager.getCommitCount()).isEqualTo(2);
            assertThat(versioningManager.isClean()).isTrue();
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

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit();

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get().getFullMessage()).isEqualTo("Initializing Git repository");
        }

        @Test
        public void commitExists() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.initialize();

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit();

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get().getFullMessage()).isEqualTo("Initializing Git repository using existing working directory");
        }

        @Test
        public void getLatestCommit() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=content");
            versioningManager.commit("new commit");

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit();

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get().getFullMessage()).isEqualTo("new commit");
        }

        @Test
        public void getLatestCommitFromLive() throws GitAPIException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=content");
            versioningManager.commit("new commit");

            Optional<RevCommit> latestCommit = versioningManager.getLatestCommit(Branch.LIVE);

            assertThat(latestCommit).isNotEmpty();
            assertThat(latestCommit.get().getFullMessage()).isEqualTo("Initializing Git repository using existing working directory");
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
            versioningManager.commit("new commit");

            WorkspaceDiff result = versioningManager.getWorkspaceDiff();

            assertThat(result.getDiffEntries()).containsExactlyInAnyOrder(
                    SimpleDiffEntry.builder().file("/file_added.yml").type(DiffEntry.ChangeType.ADD).build(),
                    SimpleDiffEntry.builder().file("/file_modified.yml").type(DiffEntry.ChangeType.MODIFY).build(),
                    SimpleDiffEntry.builder().file("/file_removed.yml").type(DiffEntry.ChangeType.DELETE).build()
            );
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
            versioningManager.commit("new commit");

            WorkspaceDiff result = versioningManager.getWorkspaceDiff(true);

            assertThat(result.getDiffEntries()).containsExactlyInAnyOrder(
                    SimpleDiffEntry.builder().file("/file_added.yml").type(DiffEntry.ChangeType.ADD).newContent("").build(),
                    SimpleDiffEntry.builder().file("/file_modified.yml").type(DiffEntry.ChangeType.MODIFY).oldContent("").newContent("new content").build(),
                    SimpleDiffEntry.builder().file("/file_removed.yml").type(DiffEntry.ChangeType.DELETE).oldContent("content").build()
            );
            assertThat(result.getLiveCommitId()).isNotEqualTo(result.getWorkspaceCommitId());
        }

        @Test
        public void getDiffById() throws IOException, GitAPIException {
            // initial
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            // commit 1
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=new content");
            versioningManager.commit("commit");
            ObjectId workspaceId = versioningManager.getLatestCommit().get().getId();
            ObjectId liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId();
            // commit 2
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=another content");
            versioningManager.commit("commit");
            ObjectId latestWorkspaceId = versioningManager.getLatestCommit().get().getId();

            WorkspaceDiff resultFirst = versioningManager.getWorkspaceDiff(true, liveId, workspaceId);
            WorkspaceDiff resultSecond = versioningManager.getWorkspaceDiff(true, liveId, latestWorkspaceId);

            assertThat(resultFirst.getDiffEntries()).containsExactlyInAnyOrder(
                    SimpleDiffEntry.builder().file("/file_modified.yml").type(DiffEntry.ChangeType.MODIFY).oldContent("").newContent("new content").build()
            );
            assertThat(resultFirst.getLiveCommitId()).isEqualTo(liveId.name());
            assertThat(resultFirst.getWorkspaceCommitId()).isEqualTo(workspaceId.name());

            assertThat(resultSecond.getDiffEntries()).containsExactlyInAnyOrder(
                    SimpleDiffEntry.builder().file("/file_modified.yml").type(DiffEntry.ChangeType.MODIFY).oldContent("").newContent("another content").build()
            );
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
            versioningManager.commit("new commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList(
                    "/file_added.yml",
                    "/file_modified.yml",
                    "/file_removed.yml"
            ));

            versioningManager.promoteConfiguration(promotion);

            WorkspaceDiff diff = versioningManager.getWorkspaceDiff();

            assertThat(diff.getDiffEntries()).isEmpty();
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
            versioningManager.commit("new commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList(
                    "/file_modified.yml",
                    "/file_removed.yml"
            ));

            versioningManager.promoteConfiguration(promotion);

            WorkspaceDiff diff = versioningManager.getWorkspaceDiff();

            assertThat(diff.getDiffEntries()).containsExactlyInAnyOrder(
                    SimpleDiffEntry.builder().file("/file_added.yml").type(DiffEntry.ChangeType.ADD).build()
            );
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
            versioningManager.commit("new commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList(
                    "/file_modified.yml"
            ));

            // first promotion
            versioningManager.promoteConfiguration(promotion);

            // second
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=new_content");
            versioningManager.commit("another commit");

            liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList(
                    "/file_modified.yml"
            ));

            // second promotion
            versioningManager.promoteConfiguration(promotion);

            // diff
            WorkspaceDiff diff = versioningManager.getWorkspaceDiff();

            assertThat(diff.getDiffEntries()).containsExactlyInAnyOrder(
                    SimpleDiffEntry.builder().file("/file_added.yml").type(DiffEntry.ChangeType.ADD).build(),
                    SimpleDiffEntry.builder().file("/file_removed.yml").type(DiffEntry.ChangeType.DELETE).build()
            );
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
            versioningManager.commit("new commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList(
                    "/file_modified.yml"
            ));

            versioningManager.promoteConfiguration(promotion);

            ConfigurationPromotion secondPromotion = new ConfigurationPromotion();
            secondPromotion.setLiveCommitId(liveId);
            secondPromotion.setWorkspaceCommitId(workspaceId);
            secondPromotion.setFiles(Arrays.asList(
                    "/file_added.yml"
            ));

            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> versioningManager.promoteConfiguration(secondPromotion))
                    .withMessage("Live branch has been modified. The provided promotion definition is out of sync.");
        }

        @Test
        public void promotionWithModifictaion() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml");
            versioningManager.initialize();
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_A");
            versioningManager.commit("commit");

            String liveId = versioningManager.getLatestCommit(Branch.LIVE).get().getId().name();
            String workspaceId = versioningManager.getLatestCommit(Branch.WORKSPACE).get().getId().name();

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setLiveCommitId(liveId);
            promotion.setWorkspaceCommitId(workspaceId);
            promotion.setFiles(Arrays.asList(
                    "/file_modified.yml"
            ));

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file_modified.yml=content_B");
            versioningManager.commit("commit");

            versioningManager.promoteConfiguration(promotion);

            // diff live -> workspace
            WorkspaceDiff diff = versioningManager.getWorkspaceDiff();

            assertThat(diff.getDiffEntries()).containsExactlyInAnyOrder(
                    SimpleDiffEntry.builder().file("/file_modified.yml").type(DiffEntry.ChangeType.MODIFY).build()
            );
            assertThat(versioningManager.getLiveRevision().readConfigurationFile("file_modified.yml")).hasValue("content_A");
            assertThat(versioningManager.getWorkspaceRevision().readConfigurationFile("file_modified.yml")).hasValue("content_B");
        }
    }

    @Nested
    class GetRevisionById {

        @Test
        public void getRevision() throws GitAPIException, IOException {
            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=a");
            versioningManager.initialize();

            WorkspaceDiff workspaceDiff = versioningManager.getWorkspaceDiff();

            createTestFiles(AbstractFileAccessor.CONFIGURATION_FILES_SUBFOLDER + "/file.yml=b");
            versioningManager.commit("commit");

            RevisionAccess result = versioningManager.getRevisionById(ObjectId.fromString(workspaceDiff.getWorkspaceCommitId()));

            Optional<String> fileContent = result.readConfigurationFile("file.yml");
            assertThat(fileContent).hasValue("a");
        }
    }
}