package rocks.inspectit.ocelot.rest.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.file.FileMoveDescription;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MoveControllerIntTest extends IntegrationTestBase {

    @Autowired
    private MoveController controller;

    @Nested
    class MoveFileOrDirectory {

        @Test
        public void srcNotExisting() throws Exception {
            FileMoveDescription content = FileMoveDescription.builder()
                    .source("src")
                    .target("trgt")
                    .build();

            MvcResult mvcResult = mockMvc.perform(
                    put("/api/v1/move")
                            .with(httpBasic("admin", "admin"))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(content)))
                    .andExpect(status().isNotFound())
                    .andReturn();
        }

        @Test
        public void targetExists() throws Exception {
            createTestFiles("files/trgt/file.yml", "files/src/file.yml");

            FileMoveDescription content = FileMoveDescription.builder()
                    .source("src")
                    .target("trgt")
                    .build();

            MvcResult mvcResult = mockMvc.perform(
                    put("/api/v1/move")
                            .with(httpBasic("admin", "admin"))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(content)))
                    .andExpect(status().isConflict())
                    .andReturn();
        }

        @Test
        public void successfulMove() throws Exception {
            createTestFiles("files/src/file.yml");

            FileMoveDescription content = FileMoveDescription.builder()
                    .source("src")
                    .target("trgt")
                    .build();

            assertThat(Paths.get(settings.getWorkingDirectory(), "files/src/file.yml")).exists();
            assertThat(Paths.get(settings.getWorkingDirectory(), "files/trgt/file.yml")).doesNotExist();

            MvcResult mvcResult = mockMvc.perform(
                    put("/api/v1/move")
                            .with(httpBasic("admin", "admin"))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(content)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(Paths.get(settings.getWorkingDirectory(), "files/src/file.yml")).doesNotExist();
            assertThat(Paths.get(settings.getWorkingDirectory(), "files/trgt/file.yml")).exists();
        }
    }
}