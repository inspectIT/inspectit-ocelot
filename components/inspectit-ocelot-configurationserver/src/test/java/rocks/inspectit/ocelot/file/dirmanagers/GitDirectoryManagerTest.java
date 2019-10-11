package rocks.inspectit.ocelot.file.dirmanagers;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileVersionResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GitDirectoryManagerTest {

    @InjectMocks
    private GitDirectoryManager gitDirectoryManager;

    @Mock
    private VersionController versionController;

    @Nested
    public class GetAllCommits {
        @Test
        void noCommits() throws IOException, GitAPIException {
            when(versionController.getAllCommits()).thenReturn(Collections.emptyList());

            List<FileVersionResponse> output = gitDirectoryManager.getAllCommits();

            assertThat(output).isEqualTo(Collections.emptyList());
        }

        @Test
        void hasCommits() throws IOException, GitAPIException {
            ObjectId mockObjectId = mock(ObjectId.class);
            List<ObjectId> mockObjectIdList = Arrays.asList(mockObjectId);
            when(versionController.getAllCommits()).thenReturn(mockObjectIdList);
            when(versionController.getFullMessageOfCommit(mockObjectId)).thenReturn("test message");
            when(versionController.getPathsOfCommit(mockObjectId)).thenReturn(Collections.emptyList());
            when(versionController.getTimeOfCommit(mockObjectId)).thenReturn(1);
            when(versionController.getAuthorOfCommit(mockObjectId)).thenReturn("test author");
            GitDirectoryManager spyGitDirectoryManager = spy(gitDirectoryManager);
            doReturn("testName").when(spyGitDirectoryManager).getNameOfObjectId(any());

            List<FileVersionResponse> output = spyGitDirectoryManager.getAllCommits();

            assertThat(output.size()).isEqualTo(1);
            FileVersionResponse response = output.get(0);
            assertThat(response.getCommitTitle()).isEqualTo("test message");
            assertThat(response.getCommitId()).isEqualTo("testName");
            assertThat(((List) response.getCommitContent())).isEqualTo(Collections.emptyList());
            assertThat(response.getTimeinMilis()).isEqualTo(1);
            assertThat(response.getAuthor()).isEqualTo("test author");
        }
    }

    @Nested
    public class GetCommitsOfFile {
        @Test
        void noCommits() throws IOException, GitAPIException {
            when(versionController.getAllCommits()).thenReturn(Collections.emptyList());

            List<FileVersionResponse> output = gitDirectoryManager.getAllCommits();

            assertThat(output).isEqualTo(Collections.emptyList());
        }

        @Test
        void hasCommits() throws IOException, GitAPIException {
            ObjectId mockObjectId = mock(ObjectId.class);
            List<ObjectId> mockObjectIdList = Arrays.asList(mockObjectId);
            when(versionController.getCommitsOfFile("testPath")).thenReturn(mockObjectIdList);
            when(versionController.getFullMessageOfCommit(mockObjectId)).thenReturn("test message");
            when(versionController.getPathsOfCommit(mockObjectId)).thenReturn(Collections.emptyList());
            when(versionController.getTimeOfCommit(mockObjectId)).thenReturn(1);
            when(versionController.getAuthorOfCommit(mockObjectId)).thenReturn("test author");
            GitDirectoryManager spyGitDirectoryManager = spy(gitDirectoryManager);
            doReturn("testName").when(spyGitDirectoryManager).getNameOfObjectId(any());

            List<FileVersionResponse> output = spyGitDirectoryManager.getCommitsOfFile("testPath");

            assertThat(output.size()).isEqualTo(1);
            FileVersionResponse response = output.get(0);
            assertThat(response.getCommitTitle()).isEqualTo("test message");
            assertThat(response.getCommitId()).isEqualTo("testName");
            assertThat(((List) response.getCommitContent())).isEqualTo(Collections.emptyList());
            assertThat(response.getTimeinMilis()).isEqualTo(1);
            assertThat(response.getAuthor()).isEqualTo("test author");
        }

    }

}
