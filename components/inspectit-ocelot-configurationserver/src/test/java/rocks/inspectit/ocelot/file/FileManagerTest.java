package rocks.inspectit.ocelot.file;


import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class FileManagerTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");

    private FileManager fm;

    @BeforeAll
    private static void setup() throws Exception {
        Files.createDirectories(rootWorkDir);
    }

    @AfterAll
    private static void clean() throws Exception {
        deleteDirectory(rootWorkDir);
    }

    @BeforeEach
    private void setupFileManager() throws Exception {
        fm = new FileManager();
        fm.workingDir = fmRoot.toString();
        fm.init();
    }

    @AfterEach
    private void cleanFileManager() throws Exception {
        deleteDirectory(fmRoot);
    }


    private static void setupTestFiles(String... paths) {
        try {
            for (String path : paths) {
                if (!path.contains("=")) {
                    Files.createDirectories(fmRoot.resolve(path));
                } else {
                    String[] splitted = path.split("=");
                    Path file = fmRoot.resolve(splitted[0]);
                    Files.createDirectories(file.getParent());
                    String content = splitted.length > 1 ? splitted[1] : "";
                    Files.write(file, content.getBytes(FileManager.ENCODING));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        List<Path> files = Files.walk(path).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(files);
        for (Path f : files) {
            Files.delete(f);
        }
    }

    @FunctionalInterface
    private interface Verfification<T> {
        void verify(T t) throws Exception;
    }


    @Nested
    class GetFilesInDirectory {

        @Test
        void listRootFiles() throws Exception {
            setupTestFiles("topA/fileA=", "topA/nested", "topB");
            Verfification<String> checkForCall = (path) ->
                    assertThat(fm.getFilesInDirectory(path))
                            .hasSize(4)
                            .anySatisfy((f) -> {
                                assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                assertThat(f.getPath()).isEqualTo("topA");
                            })
                            .anySatisfy((f) -> {
                                assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                assertThat(f.getPath()).isEqualTo("topB");
                            })
                            .anySatisfy((f) -> {
                                assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                assertThat(f.getPath()).isEqualTo("topA/nested");
                            })
                            .anySatisfy((f) -> {
                                assertThat(f.getType()).isEqualTo(FileInfo.Type.FILE);
                                assertThat(f.getPath()).isEqualTo("topA/fileA");
                            });

            checkForCall.verify(null);
            checkForCall.verify("");
            checkForCall.verify("./");
        }

        @Test
        void listNonRootFiles() throws Exception {
            setupTestFiles("topA/fileA=", "topA/nested/sub", "topB");
            assertThat(fm.getFilesInDirectory("topA"))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA/nested");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA/nested/sub");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(f.getPath()).isEqualTo("topA/fileA");
                    });
        }


        @Test
        void verifyFilesOutsideWorkdirNotAccessible() {
            setupTestFiles("top");

            assertThatThrownBy(() -> fm.getFilesInDirectory("../"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.getFilesInDirectory("top/../../"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.getFilesInDirectory("../../"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    class CreateNewDirectory {

        @Test
        void createDirInRootDirect() throws Exception {
            setupTestFiles("topA", "topB");

            fm.createNewDirectory("myDir");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topB");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("myDir");
                    });
        }


        @Test
        void createDirInRootIndirect() throws Exception {
            setupTestFiles("topA", "topB");

            fm.createNewDirectory("topB/.././myDir");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topB");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("myDir");
                    });
        }


        @Test
        void createDirInSubFolder() throws Exception {
            setupTestFiles("topA", "topB");

            fm.createNewDirectory("topA/subA/subB");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(4)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topB");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA/subA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA/subA/subB");
                    });
        }

        @Test
        void createDirOnExistingDir() {
            setupTestFiles("topA/subA", "topB");

            assertThatThrownBy(() -> fm.createNewDirectory("topA/subA"))
                    .isInstanceOf(FileAlreadyExistsException.class);
        }

        @Test
        void createDirOnExistingFile() {
            setupTestFiles("topA=content");

            assertThatThrownBy(() -> fm.createNewDirectory("topA"))
                    .isInstanceOf(FileAlreadyExistsException.class);
        }


        @Test
        void createDirBeneathExistingFile() {
            setupTestFiles("topA=content");

            assertThatThrownBy(() -> fm.createNewDirectory("topA/subDir"))
                    .isInstanceOf(FileAlreadyExistsException.class);
        }

        @Test
        void verifyDirOutsideWorkdirNotCreateable() {
            setupTestFiles("top");

            assertThatThrownBy(() -> fm.createNewDirectory(null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createNewDirectory(""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createNewDirectory("./"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createNewDirectory("../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createNewDirectory("top/../../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createNewDirectory("../../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }


    @Nested
    class DeleteDirectory {

        @Test
        void deleteDirInRootDirect() throws Exception {
            setupTestFiles("topA", "topB");

            fm.deleteDirectory("topA");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(1)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topB");
                    });
        }


        @Test
        void createDirInRootIndirect() throws Exception {
            setupTestFiles("topA", "topB");

            fm.deleteDirectory("topB/../topA");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(1)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topB");
                    });
        }


        @Test
        void deleteDirInSubFolderWithContents() throws Exception {
            setupTestFiles("topA/subA/subB/somedir", "topA/subA/somefile=foo", "topB");

            fm.deleteDirectory("topA/subA");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topB");
                    });
        }

        @Test
        void deleteNonExistingDir() {
            setupTestFiles("topA/subA", "topB");

            assertThatThrownBy(() -> fm.deleteDirectory("topC"))
                    .isInstanceOf(NotDirectoryException.class);
        }

        @Test
        void deleteFile() {
            setupTestFiles("topA=i_am_a_file", "topB");

            assertThatThrownBy(() -> fm.deleteDirectory("topA"))
                    .isInstanceOf(NotDirectoryException.class);
        }

        @Test
        void verifyDirOutsideWorkdirNotDeleteable() {
            setupTestFiles("top");

            assertThatThrownBy(() -> fm.deleteDirectory(null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.deleteDirectory(""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.deleteDirectory("./"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.deleteDirectory("../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.deleteDirectory("top/../../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.deleteDirectory("../../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }


    @Nested
    class ReadFile {

        @Test
        void readEmptyFile() throws Exception {
            setupTestFiles("fileA=", "fileB=something");

            assertThat(fm.readFile("fileA")).isEqualTo("");
            assertThat(fm.readFile("./fileA")).isEqualTo("");
        }

        @Test
        void readNonEmptyFile() throws Exception {
            setupTestFiles("fileA=", "sub/fileB=something\nsomething else");

            assertThat(fm.readFile("sub/fileB")).isEqualTo("something\nsomething else");
            assertThat(fm.readFile("./sub/../sub/fileB")).isEqualTo("something\nsomething else");
        }


        @Test
        void readDirectory() {
            setupTestFiles("dirA");

            assertThatThrownBy(() -> fm.readFile("dirA"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void verifyFilesOutsideWorkdirNotAccessible() {
            setupTestFiles("top");

            assertThatThrownBy(() -> fm.readFile("../someFile"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.readFile("top/../../foo"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.readFile("../../bar"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }


    @Nested
    class CreateOrReplaceFile {

        @Test
        void createNewFileInRootDirectory() throws Exception {
            setupTestFiles("fileA=foo", "topB");

            fm.createOrReplaceFile("myFile", "content");

            assertThat(fm.readFile("myFile")).isEqualTo("content");
        }

        @Test
        void createNewFileInSubDirectory() throws Exception {
            setupTestFiles("fileA=foo", "topB");

            fm.createOrReplaceFile("topB/../topB/./sub/myFile", "content");

            assertThat(fm.readFile("topB/sub/myFile")).isEqualTo("content");
        }


        @Test
        void removeFileContent() throws Exception {
            setupTestFiles("fileA=foo", "topB");

            fm.createOrReplaceFile("fileA", "");

            assertThat(fm.readFile("fileA")).isEqualTo("");
        }

        @Test
        void replaceFileContent() throws Exception {
            setupTestFiles("topA/fileA=foo", "topB");

            fm.createOrReplaceFile("topA/fileA", "bar");

            assertThat(fm.readFile("topA/fileA")).isEqualTo("bar");
        }

        @Test
        void writeToDirectory() {
            setupTestFiles("topA/subA", "topB");

            assertThatThrownBy(() -> fm.createOrReplaceFile("topA/subA", ""))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void verifyFilesOutsideWorkdirNotCreatable() {
            setupTestFiles("top");

            assertThatThrownBy(() -> fm.createOrReplaceFile(null, ""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createOrReplaceFile("", ""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createOrReplaceFile("./", ""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createOrReplaceFile("../mydir", ""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createOrReplaceFile("top/../../mydir", ""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createOrReplaceFile("../../mydir", ""))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }


    @Nested
    class DeleteFile {

        @Test
        void deleteFileInRootDirect() throws Exception {
            setupTestFiles("topA=foo", "topB");

            fm.deleteFile("topA");

            assertThat(fm.getFilesInDirectory(""))
                    .noneSatisfy((f) ->
                            assertThat(f.getPath()).isEqualTo("topA")
                    );
        }


        @Test
        void deleteFileInRootIndirect() throws Exception {
            setupTestFiles("topA=foo", "topB");

            fm.deleteFile("topB/.././topA");

            assertThat(fm.getFilesInDirectory(""))
                    .noneSatisfy((f) ->
                            assertThat(f.getPath()).isEqualTo("topA")
                    );
        }


        @Test
        void deleteEmptyFile() throws Exception {
            setupTestFiles("topA/subA/myFile=", "topA/subB");

            fm.deleteFile("topA/subA/myFile");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA/subA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("topA/subB");
                    });
        }

        @Test
        void deleteNonExistingFile() {
            setupTestFiles("topA/subA", "topB");

            assertThatThrownBy(() -> fm.deleteFile("topC"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void deleteDirectory() {
            setupTestFiles("topA", "topB");

            assertThatThrownBy(() -> fm.deleteFile("topA"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void verifyDirOutsideWorkdirNotDeletable() {
            setupTestFiles("top");

            assertThatThrownBy(() -> fm.deleteFile(null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.deleteFile(""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.deleteFile("./"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.deleteFile("../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.deleteFile("top/../../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.deleteFile("../../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }


    @Nested
    class Move {

        @Test
        void renameFile() throws Exception {
            setupTestFiles("top", "foo=foo");

            fm.move("foo", "bar");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("top");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(f.getPath()).isEqualTo("bar");
                    });
            assertThat(fm.readFile("bar")).isEqualTo("foo");
        }


        @Test
        void renameFolder() throws Exception {
            setupTestFiles("top", "foo/sub/something=text");

            fm.move("foo/sub", "foo/bar");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(4)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("top");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("foo");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("foo/bar");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(f.getPath()).isEqualTo("foo/bar/something");
                    });
            assertThat(fm.readFile("foo/bar/something")).isEqualTo("text");
        }

        @Test
        void moveDirectoryWithContents() throws Exception {
            setupTestFiles("top", "foo/sub/something=text", "foo/sub/a/b");

            fm.move("foo/sub", "sub");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(6)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("top");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("foo");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("sub");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("sub/a");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("sub/a/b");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(f.getPath()).isEqualTo("sub/something");
                    });
            assertThat(fm.readFile("sub/something")).isEqualTo("text");
        }


        @Test
        void moveFile() throws Exception {
            setupTestFiles("file=");

            fm.move("file", "a/b/file");

            assertThat(fm.getFilesInDirectory(""))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("a");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getPath()).isEqualTo("a/b");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(f.getPath()).isEqualTo("a/b/file");
                    });
            assertThat(fm.readFile("a/b/file")).isEqualTo("");
        }


        @Test
        void moveNonExistingFile() throws Exception {
            setupTestFiles("file=");

            assertThatThrownBy(() -> fm.move("someFile", "anotherFile"))
                    .isInstanceOf(NoSuchFileException.class);
        }

        @Test
        void moveOntoExistingFile() throws Exception {
            setupTestFiles("file=", "someFile=");

            assertThatThrownBy(() -> fm.move("someFile", "file"))
                    .isInstanceOf(FileAlreadyExistsException.class);
        }

        @Test
        void verifyDirOutsideWorkdirNotMoveable() {
            setupTestFiles("top");

            assertThatThrownBy(() -> fm.move(null, "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("/", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("./", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("../myfile", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("top/../../myfile", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("../../myfile", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void verifyCannotMoveOutsideOfWorkDir() {
            setupTestFiles("top");

            assertThatThrownBy(() -> fm.move("top", null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("top", ""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("top", "/"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("top", "./"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("top", "../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("top", "top/../../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.move("top", "../../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}
