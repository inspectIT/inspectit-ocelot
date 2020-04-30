package rocks.inspectit.ocelot.rest.file;

import com.fasterxml.jackson.databind.type.CollectionType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.file.FileInfo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectoryControllerIntTest extends IntegrationTestBase {

    @Autowired
    private DirectoryController controller;

    @Nested
    class ListContents {

        @Test
        public void emptyResponse() throws Exception {

            MvcResult mvcResult = mockMvc
                    .perform(get("/api/v1/directories/")
                            .with(httpBasic("admin", "admin")))
                    .andExpect(status().isOk())
                    .andReturn();

            String contentAsString = mvcResult.getResponse().getContentAsString();

            CollectionType resultType = objectMapper.getTypeFactory().constructCollectionType(List.class, FileInfo.class);
            Collection<FileInfo> result = objectMapper.readValue(contentAsString, resultType);

            assertThat(result).isEmpty();
        }

        @Test
        public void validResponse() throws Exception {
            createTestFiles("files/file.yml");

            MvcResult mvcResult = mockMvc
                    .perform(get("/api/v1/directories/")
                            .with(httpBasic("admin", "admin")))
                    .andExpect(status().isOk())
                    .andReturn();

            String contentAsString = mvcResult.getResponse().getContentAsString();

            CollectionType resultType = objectMapper.getTypeFactory().constructCollectionType(List.class, FileInfo.class);
            List<FileInfo> result = objectMapper.readValue(contentAsString, resultType);

            assertThat(result).hasSize(1);

            FileInfo fileInfo = result.get(0);
            assertThat(fileInfo.getName()).isEqualTo("file.yml");
            assertThat(fileInfo.getType()).isEqualTo(FileInfo.Type.FILE);
            assertThat(fileInfo.getChildren()).isNull();
        }
    }

    @Nested
    class CreateNewDirectory {

        @Test
        public void noDirectorySpecified() throws Exception {
            MvcResult result = mockMvc
                    .perform(
                            put("/api/v1/directories/").with(httpBasic("admin", "admin"))
                    )
                    .andExpect(status().is5xxServerError())
                    .andReturn();
        }

        @Test
        public void createDirectory() throws Exception {
            MvcResult result = mockMvc
                    .perform(put("/api/v1/directories/new_dir")
                            .with(httpBasic("admin", "admin")))
                    .andExpect(status().isOk())
                    .andReturn();

            Path new_dir = Paths.get(settings.getWorkingDirectory(), "files", "new_dir");
            assertThat(new_dir).exists();
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        public void noDirectorySpecified() throws Exception {
            MvcResult result = mockMvc
                    .perform(delete("/api/v1/directories/")
                            .with(httpBasic("admin", "admin")))
                    .andExpect(status().is5xxServerError())
                    .andReturn();
        }

        @Test
        public void deleteDirectory() throws Exception {
            createTestFiles("files/root/target_dir/file.yml");

            assertThat(Paths.get(settings.getWorkingDirectory(), "files", "root", "target_dir")).exists();

            MvcResult result = mockMvc
                    .perform(delete("/api/v1/directories/root/target_dir")
                            .with(httpBasic("admin", "admin")))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(Paths.get(settings.getWorkingDirectory(), "files", "root")).exists();
            assertThat(Paths.get(settings.getWorkingDirectory(), "files", "root", "target_dir")).doesNotExist();
        }
    }
}