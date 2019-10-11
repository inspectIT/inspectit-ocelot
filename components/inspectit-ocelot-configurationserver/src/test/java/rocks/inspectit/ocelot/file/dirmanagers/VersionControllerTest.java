package rocks.inspectit.ocelot.file.dirmanagers;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VersionControllerTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");
    private static final String FILES_SUBFOLDER = "files";

    @InjectMocks
    VersionController versionController;

    @InjectMocks
    VersionController versionControllerTest;

    @Spy
    private Git git;

    @Spy
    private Repository repo;

    @BeforeEach
    private void resetMockito() {
        Mockito.reset();
    }

    @Nested
    public class CommitAllChanges {
        @Test
        void testCommit() throws GitAPIException {
            AddCommand mockAddCommand = mock(AddCommand.class, Answers.RETURNS_SELF);
            when(git.add()).thenReturn(mockAddCommand);
            CommitCommand mockCommitCommand = mock(CommitCommand.class, Answers.RETURNS_SELF);
            when(git.commit()).thenReturn(mockCommitCommand);

            versionController.commitAllChanges();

            verify(git).add();
            verify(mockAddCommand).addFilepattern(".");
            verify(mockAddCommand).call();
            verify(git).commit();
            verify(mockCommitCommand).setAll(true);
            verify(mockCommitCommand).setMessage(any());
            verify(mockCommitCommand).call();
            verify(git).reset();
            verifyNoMoreInteractions(mockAddCommand, mockCommitCommand, git);
        }

        //TODO Tests wie hier unten stehend schreiben
        @Test
        void throwsException() throws GitAPIException {
            AddCommand mockAddCommand = mock(AddCommand.class, Answers.RETURNS_SELF);
            when(git.add()).thenReturn(mockAddCommand);
            CommitCommand mockCommitCommand = mock(CommitCommand.class, Answers.RETURNS_SELF);
            when(git.commit()).thenReturn(mockCommitCommand);
            when(mockCommitCommand.call()).thenThrow(WrongRepositoryStateException.class);

            assertThatExceptionOfType(GitAPIException.class)
                    .isThrownBy(() -> versionController.commitAllChanges());

            verify(git).add();
            verify(mockAddCommand).addFilepattern(".");
            verify(mockAddCommand).call();
            verify(git).commit();
            verify(mockCommitCommand).setAll(true);
            verify(mockCommitCommand).setMessage(any());
            verify(mockCommitCommand).call();
            verifyNoMoreInteractions(mockAddCommand, mockCommitCommand, git);
        }
    }

    @Nested
    public class CommitFile {
        @Test
        void singleFileCommit() throws IOException, GitAPIException {
            AddCommand mockAddCommand = mock(AddCommand.class, Answers.RETURNS_SELF);
            when(git.add()).thenReturn(mockAddCommand);
            CommitCommand mockCommitCommand = mock(CommitCommand.class, Answers.RETURNS_SELF);
            when(mockCommitCommand.call()).thenThrow(WrongRepositoryStateException.class);
            when(git.commit()).thenReturn(mockCommitCommand);
            VersionController mockedController = spy(new VersionController());
            RevWalk mockRevWalk = mock(RevWalk.class);
            doReturn(mockRevWalk)
                    .when(mockedController)
                    .getRevWalk();

            versionController.readFile("configuration/a");

            verify(git).add();
            verify(mockAddCommand).addFilepattern(".");
            verify(mockAddCommand).call();
            verify(git).commit();
            verify(mockCommitCommand).setAll(true);
            verify(mockCommitCommand).setMessage(any());
            verify(mockCommitCommand).call();
            verifyNoMoreInteractions(mockAddCommand, mockCommitCommand, git);
        }
    }

    @Nested
    public class listFiles {
        @Test
        void testListConfigOnly() throws IOException {
            TreeWalk mockTreeWalk = mock(TreeWalk.class);
            when(mockTreeWalk.next()).thenReturn(true);
            when(mockTreeWalk.getPathString()).thenReturn("testFile");
            VersionController mockedController = Mockito.spy(versionControllerTest);
            doReturn(mockTreeWalk)
                    .when(mockedController)
                    .getTreeWalk();
            //Just return a value that is not null
            when(repo.resolve(Constants.HEAD)).thenReturn(new ObjectId(1, 2, 3, 4, 5));

            mockedController.listFiles("", true);

        }

        @Test
        void emptyHead() throws IOException {
            when(repo.resolve(Constants.HEAD)).thenReturn(null);

            Object output = versionController.listFiles("", true);

            assertThat(output).isEqualTo(Collections.emptyList());
            verify(repo).resolve(Constants.HEAD);
            verifyNoMoreInteractions(repo);
        }
    }

    @Nested
    public class ReadFile {
        @Test
        void readExistingFile() throws IOException, GitAPIException {
            when(repo.resolve(Constants.HEAD)).thenReturn(new ObjectId(1, 2, 3, 4, 5));

            assertThat(versionController.readFile("configuration/a")).isEqualTo("testContent");
        }

        @Test
        void readNonExistingFile() throws IOException, GitAPIException {
            assertThat(versionController.readFile("configuration/b")).isEqualTo(null);
        }
    }

}
