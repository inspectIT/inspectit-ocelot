package rocks.inspectit.ocelot.file;


import org.apache.commons.io.FileExistsException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.dirmanagers.GitDirManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileManagerTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");

    private static final Charset ENCODING = StandardCharsets.UTF_8;
    static final String FILES_SUBFOLDER = "files/configuration";


    @InjectMocks
    private FileManager fm;

    @Mock
    private GitDirManager gdm;

    @Mock
    ApplicationEventPublisher eventPublisher;

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
        InspectitServerSettings conf = new InspectitServerSettings();
        conf.setWorkingDirectory(fmRoot.toString());
        fm.config = conf;
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
                    Files.createDirectories(fmRoot.resolve(FileManager.FILES_SUBFOLDER).resolve(path));
                } else {
                    String[] splitted = path.split("=");
                    Path file = fmRoot.resolve(FileManager.FILES_SUBFOLDER).resolve(splitted[0]);
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

    private static String readFile(String path) throws IOException {
        Path file = fmRoot.resolve(FILES_SUBFOLDER).resolve(path);
        return new String(Files.readAllBytes(file), ENCODING);
    }

    @FunctionalInterface
    private interface Verfification<T> {
        void verify(T t) throws Exception;
    }


    @Nested
    class GetFilesInDirectory {

        @Test
        void listRootFilesRecursive() throws Exception {
            setupTestFiles("topA/fileA=", "topA/nested", "topB");
            Verfification<String> checkForCall = (path) ->
                    assertThat(fm.getFilesInDirectory(path, true))
                            .hasSize(2)
                            .anySatisfy((f) -> {
                                assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                assertThat(f.getName()).isEqualTo("topA");
                                assertThat(f.getChildren())
                                        .hasSize(2)
                                        .anySatisfy((f2) -> {
                                            assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                            assertThat(f2.getName()).isEqualTo("nested");
                                            assertThat(f2.getChildren()).isEmpty();
                                        })
                                        .anySatisfy((f2) -> {
                                            assertThat(f2.getType()).isEqualTo(FileInfo.Type.FILE);
                                            assertThat(f2.getName()).isEqualTo("fileA");
                                        });
                            })
                            .anySatisfy((f) -> {
                                assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                assertThat(f.getName()).isEqualTo("topB");
                                assertThat(f.getChildren()).isEmpty();
                            });

            checkForCall.verify(null);
            checkForCall.verify("");
            checkForCall.verify("./");
        }

        @Test
        void listRootFilesNonRecursive() throws Exception {
            setupTestFiles("topA/fileA=", "topA/nested", "topB");
            Verfification<String> checkForCall = (path) ->
                    assertThat(fm.getFilesInDirectory(path, false))
                            .hasSize(2)
                            .anySatisfy((f) -> {
                                assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                assertThat(f.getName()).isEqualTo("topA");
                                assertThat(f.getChildren()).isNull();
                            })
                            .anySatisfy((f) -> {
                                assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                assertThat(f.getName()).isEqualTo("topB");
                                assertThat(f.getChildren()).isNull();
                            });

            checkForCall.verify(null);
            checkForCall.verify("");
            checkForCall.verify("./");
        }

        @Test
        void listNonRootFiles() throws Exception {
            setupTestFiles("topA/fileA=", "topA/nested/sub", "topB");
            assertThat(fm.getFilesInDirectory("topA", true))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("nested");
                        assertThat(f.getChildren())
                                .hasSize(1)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("sub");
                                    assertThat(f2.getChildren()).isEmpty();
                                });
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(f.getName()).isEqualTo("fileA");
                    });
        }


        @Test
        void verifyFilesOutsideWorkdirNotAccessible() {
            setupTestFiles("top");

            assertThatThrownBy(() -> fm.getFilesInDirectory("../", true))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.getFilesInDirectory("top/../../", true))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.getFilesInDirectory("../../", true))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    class CreateDirectory {

        @Test
        void createDirInRootDirect() throws Exception {
            setupTestFiles("topA", "topB");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.createDirectory("myDir");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("myDir");
                    });

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }


        @Test
        void createDirInRootIndirect() throws Exception {
            setupTestFiles("topA", "topB");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.createDirectory("topB/.././myDir");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("myDir");
                    });

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }


        @Test
        void createDirInSubFolder() throws Exception {
            setupTestFiles("topA", "topB");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.createDirectory("topA/subA/subB");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topA");
                        assertThat(f.getChildren())
                                .hasSize(1)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("subA");
                                    assertThat(f2.getChildren())
                                            .hasSize(1)
                                            .anySatisfy((f3) -> {
                                                assertThat(f3.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                                assertThat(f3.getName()).isEqualTo("subB");
                                                assertThat(f3.getChildren()).isEmpty();
                                            });
                                });
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    });

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }

        @Test
        void createDirOnExistingDir() throws Exception {
            setupTestFiles("topA/subA", "topB");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.createDirectory("topA/subA");

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }

        @Test
        void createDirOnExistingFile() {
            setupTestFiles("topA=content");

            assertThatThrownBy(() -> fm.createDirectory("topA"))
                    .isInstanceOf(IOException.class);
        }


        @Test
        void createDirBeneathExistingFile() {
            setupTestFiles("topA=content");

            assertThatThrownBy(() -> fm.createDirectory("topA/subDir"))
                    .isInstanceOf(IOException.class);
        }

        @Test
        void verifyDirOutsideWorkdirNotCreateable() {
            setupTestFiles("top");

            assertThatThrownBy(() -> fm.createDirectory(null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createDirectory(""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createDirectory("./"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createDirectory("../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createDirectory("top/../../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> fm.createDirectory("../../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }


    @Nested
    class DeleteDirectory {

        @Test
        void deleteDirInRootDirect() throws Exception {
            setupTestFiles("topA", "topB");

            fm.deleteDirectory("topA");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(1)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    });

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }


        @Test
        void deleteDirInRootIndirect() throws Exception {
            setupTestFiles("topA", "topB");

            fm.deleteDirectory("topB/../topA");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(1)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    });

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }


        @Test
        void deleteDirInSubFolderWithContents() throws Exception {
            setupTestFiles("topA/subA/subB/somedir", "topA/subA/somefile=foo", "topB");

            fm.deleteDirectory("topA/subA");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    });

            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
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
            when(gdm.readFile(any())).thenReturn("");

            assertThat(fm.readFile("fileA")).isEqualTo("");
            assertThat(fm.readFile("./fileA")).isEqualTo("");
        }

        @Test
        void readNonEmptyFile() throws Exception {
            setupTestFiles("fileA=", "sub/fileB=something\nsomething else");
            when(gdm.readFile(any())).thenReturn("something\nsomething else");

            assertThat(fm.readFile("sub/fileB")).isEqualTo("something\nsomething else");
            assertThat(fm.readFile("./sub/../sub/fileB")).isEqualTo("something\nsomething else");
        }
    }


    @Nested
    class CreateOrReplaceFile {

        @Test
        void createNewFileInRootDirectory() throws Exception {
            setupTestFiles("fileA=foo", "topB");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.createOrReplaceFile("myFile", "content");

            assertThat(readFile("myFile")).isEqualTo("content");
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }

        @Test
        void createNewFileInSubDirectory() throws Exception {
            setupTestFiles("fileA=foo", "topB");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.createOrReplaceFile("topB/../topB/./sub/myFile", "content");

            assertThat(readFile("topB/sub/myFile")).isEqualTo("content");
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }


        @Test
        void removeFileContent() throws Exception {
            setupTestFiles("fileA=foo", "topB");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.createOrReplaceFile("fileA", "");

            assertThat(readFile("fileA")).isEqualTo("");
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }

        @Test
        void replaceFileContent() throws Exception {
            setupTestFiles("topA/fileA=foo", "topB");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.createOrReplaceFile("topA/fileA", "bar");

            assertThat(readFile("topA/fileA")).isEqualTo("bar");
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
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

            assertThat(fm.getFilesInDirectory("", true))
                    .noneSatisfy((f) ->
                            assertThat(f.getName()).isEqualTo("topA")
                    );
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }


        @Test
        void deleteFileInRootIndirect() throws Exception {
            setupTestFiles("topA=foo", "topB");

            fm.deleteFile("topB/.././topA");

            assertThat(fm.getFilesInDirectory("", true))
                    .noneSatisfy((f) ->
                            assertThat(f.getName()).isEqualTo("topA")
                    );
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }


        @Test
        void deleteEmptyFile() throws Exception {
            setupTestFiles("topA/subA/myFile=", "topA/subB");

            fm.deleteFile("topA/subA/myFile");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(1)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topA");
                        assertThat(f.getChildren())
                                .hasSize(2)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("subA");
                                    assertThat(f2.getChildren()).isEmpty();
                                })
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("subB");
                                    assertThat(f2.getChildren()).isEmpty();
                                });
                    });
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
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
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.move("foo", "bar");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("top");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(f.getName()).isEqualTo("bar");
                    });
            assertThat(readFile("bar")).isEqualTo("foo");
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }


        @Test
        void renameFolder() throws Exception {
            setupTestFiles("top", "foo/sub/something=text");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.move("foo/sub", "foo/bar");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("top");
                        assertThat(f.getChildren()).isEmpty();
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("foo");
                        assertThat(f.getChildren())
                                .hasSize(1)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("bar");
                                    assertThat(f2.getChildren())
                                            .hasSize(1)
                                            .anySatisfy((f3) -> {
                                                assertThat(f3.getType()).isEqualTo(FileInfo.Type.FILE);
                                                assertThat(f3.getName()).isEqualTo("something");
                                            });
                                });
                    });
            assertThat(readFile("foo/bar/something")).isEqualTo("text");
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }

        @Test
        void moveDirectoryWithContents() throws Exception {
            setupTestFiles("top", "foo/sub/something=text", "foo/sub/a/b");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.move("foo/sub", "sub");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("top");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("foo");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("sub");
                        assertThat(f.getChildren())
                                .hasSize(2)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("a");
                                    assertThat(f2.getChildren())
                                            .hasSize(1)
                                            .anySatisfy((f3) -> {
                                                assertThat(f3.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                                assertThat(f3.getName()).isEqualTo("b");
                                                assertThat(f3.getChildren()).isEmpty();
                                            });
                                })
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.FILE);
                                    assertThat(f2.getName()).isEqualTo("something");
                                });
                    });
            assertThat(readFile("sub/something")).isEqualTo("text");
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }


        @Test
        void moveFile() throws Exception {
            setupTestFiles("file=");
            when(gdm.commitAllChanges()).thenReturn(true);

            fm.move("file", "a/b/file");

            assertThat(fm.getFilesInDirectory("", true))
                    .hasSize(1)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("a");
                        assertThat(f.getChildren())
                                .hasSize(1)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("b");
                                    assertThat(f2.getChildren())
                                            .hasSize(1)
                                            .anySatisfy((f3) -> {
                                                assertThat(f3.getType()).isEqualTo(FileInfo.Type.FILE);
                                                assertThat(f3.getName()).isEqualTo("file");
                                            });
                                });
                    });
            assertThat(readFile("a/b/file")).isEqualTo("");
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }


        @Test
        void moveNonExistingFile() throws Exception {
            setupTestFiles("file=");

            assertThatThrownBy(() -> fm.move("someFile", "anotherFile"))
                    .isInstanceOf(FileNotFoundException.class);
        }

        @Test
        void moveOntoExistingFile() throws Exception {
            setupTestFiles("file=", "someFile=");

            assertThatThrownBy(() -> fm.move("someFile", "file"))
                    .isInstanceOf(FileExistsException.class);
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
