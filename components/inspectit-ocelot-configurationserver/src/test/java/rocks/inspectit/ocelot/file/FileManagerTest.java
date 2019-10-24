package rocks.inspectit.ocelot.file;


import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.dirmanagers.GitDirectoryManager;
import rocks.inspectit.ocelot.file.dirmanagers.WorkingDirectoryManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileManagerTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");
    private static final Path filesRoot = fmRoot.resolve("files");

    private static final Charset ENCODING = StandardCharsets.UTF_8;
    static final String FILES_SUBFOLDER = "files/configuration";

    @InjectMocks
    private FileManager fm;

    @Mock
    private GitDirectoryManager gdm;

    @Mock
    private WorkingDirectoryManager wdm;


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

    public synchronized List<FileInfo> getFilesInDirectory(String path, boolean recursive) throws IOException {
        Path dir;
        if (StringUtils.isEmpty(path)) {
            dir = filesRoot;
        } else {
            dir = filesRoot.resolve(path);
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<FileInfo> result = new ArrayList<>();
            for (Path child : files.collect(Collectors.toList())) {
                boolean isDirectory = Files.isDirectory(child);
                FileInfo.FileInfoBuilder builder = FileInfo.builder()
                        .name(child.getFileName().toString())
                        .type(isDirectory ? FileInfo.Type.DIRECTORY : FileInfo.Type.FILE);
                if (isDirectory && recursive) {
                    builder.children(getFilesInDirectory(filesRoot.relativize(child.normalize())
                            .toString()
                            .replace(child.getFileSystem().getSeparator(), "/"), true));
                }
                result.add(builder.build());
            }
            return result;
        }
    }

    @FunctionalInterface
    private interface Verfification<T> {
        void verify(T t) throws Exception;
    }

    @Nested
    class CreateDirectory {

        @Test
        void createDirInRootDirect() throws Exception {
            setupTestFiles("topA", "topB");

            fm.createDirectory("myDir");

            assertThat(getFilesInDirectory("", true))
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

            fm.createDirectory("topB/.././myDir");

            assertThat(getFilesInDirectory("", true))
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

            fm.createDirectory("topA/subA/subB");

            assertThat(getFilesInDirectory("", true))
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

            assertThat(getFilesInDirectory("", true))
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

            assertThat(getFilesInDirectory("", true))
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

            assertThat(getFilesInDirectory("", true))
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
    class ReadSpecialFile {

        @Test
        void readEmptyFileVersioning() throws Exception {
            setupTestFiles("fileA=", "fileB=something");
            when(gdm.readFile(any())).thenReturn("");

            assertThat(fm.readSpecialFile("fileA", true)).isEqualTo("");
            assertThat(fm.readSpecialFile("./fileA", true)).isEqualTo("");
        }

        @Test
        void readNonEmptyFileVersioning() throws Exception {
            setupTestFiles("fileA=", "sub/fileB=something\nsomething else");
            when(gdm.readFile(any())).thenReturn("something\nsomething else");

            assertThat(fm.readSpecialFile("sub/fileB", true)).isEqualTo("something\nsomething else");
            assertThat(fm.readSpecialFile("./sub/../sub/fileB", true)).isEqualTo("something\nsomething else");
        }

        @Test
        void readEmptyFile() throws Exception {
            setupTestFiles("fileA=", "fileB=something");
            when(wdm.readFile(any())).thenReturn("");

            assertThat(fm.readSpecialFile("fileA", false)).isEqualTo("");
            assertThat(fm.readSpecialFile("./fileA", false)).isEqualTo("");
        }

        @Test
        void readNonEmpty() throws Exception {
            setupTestFiles("fileA=", "sub/fileB=something\nsomething else");
            when(wdm.readFile(any())).thenReturn("something\nsomething else");

            assertThat(fm.readSpecialFile("sub/fileB", false)).isEqualTo("something\nsomething else");
            assertThat(fm.readSpecialFile("./sub/../sub/fileB", false)).isEqualTo("something\nsomething else");
        }
    }

    @Nested
    class DeleteFile {

        @Test
        void deleteFileInRootDirect() throws Exception {
            setupTestFiles("topA=foo", "topB");

            fm.deleteFile("topA");

            assertThat(getFilesInDirectory("", true))
                    .noneSatisfy((f) ->
                            assertThat(f.getName()).isEqualTo("topA")
                    );
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }

        @Test
        void deleteFileInRootIndirect() throws Exception {
            setupTestFiles("topA=foo", "topB");

            fm.deleteFile("topB/.././topA");

            assertThat(getFilesInDirectory("", true))
                    .noneSatisfy((f) ->
                            assertThat(f.getName()).isEqualTo("topA")
                    );
            verify(eventPublisher).publishEvent(any(FileChangedEvent.class));
        }

        @Test
        void deleteEmptyFile() throws Exception {
            setupTestFiles("topA/subA/myFile=", "topA/subB");

            fm.deleteFile("topA/subA/myFile");

            assertThat(getFilesInDirectory("", true))
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
}
