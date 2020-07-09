package rocks.inspectit.ocelot.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class FileInfoTest {

    @Nested
    class GetAbsoluteFilePaths {

        @Test
        void testRootFile() {
            FileInfo file = FileInfo.builder().name("my_file.test").type(FileInfo.Type.FILE).build();

            List<String> paths = file.getAbsoluteFilePaths("").collect(Collectors.toList());

            assertThat(paths).containsExactlyInAnyOrder("my_file.test");
        }

        @Test
        void testNonRootFile() {
            FileInfo file = FileInfo.builder().name("my_file.test").type(FileInfo.Type.FILE).build();

            List<String> paths = file.getAbsoluteFilePaths("some/dir").collect(Collectors.toList());

            assertThat(paths).containsExactlyInAnyOrder("some/dir/my_file.test");
        }

        @Test
        void testRootDir() {
            FileInfo file = FileInfo.builder()
                    .name("someDir")
                    .type(FileInfo.Type.DIRECTORY)
                    .children(Arrays.asList(FileInfo.builder()
                            .name("subFile")
                            .type(FileInfo.Type.FILE)
                            .build(), FileInfo.builder()
                            .name("subDir")
                            .type(FileInfo.Type.DIRECTORY)
                            .children(Collections.singletonList(FileInfo.builder()
                                    .name("subSubFile")
                                    .type(FileInfo.Type.FILE)
                                    .build()))
                            .build()))
                    .build();

            List<String> paths = file.getAbsoluteFilePaths("").collect(Collectors.toList());

            assertThat(paths).containsExactlyInAnyOrder("someDir/subFile", "someDir/subDir/subSubFile");
        }

        @Test
        void testNonRootDir() {
            FileInfo file = FileInfo.builder()
                    .name("someDir")
                    .type(FileInfo.Type.DIRECTORY)
                    .children(Arrays.asList(FileInfo.builder()
                            .name("subFile")
                            .type(FileInfo.Type.FILE)
                            .build(), FileInfo.builder()
                            .name("subDir")
                            .type(FileInfo.Type.DIRECTORY)
                            .children(Collections.singletonList(FileInfo.builder()
                                    .name("subSubFile")
                                    .type(FileInfo.Type.FILE)
                                    .build()))
                            .build()))
                    .build();

            List<String> paths = file.getAbsoluteFilePaths("root/dir").collect(Collectors.toList());

            assertThat(paths).containsExactlyInAnyOrder("root/dir/someDir/subFile", "root/dir/someDir/subDir/subSubFile");
        }
    }
}
