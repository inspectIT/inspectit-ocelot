package rocks.inspectit.ocelot;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "inspectit.working-directory=temp_work_dir")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class IntegrationTestBase {

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected InspectitServerSettings serverConf;

    /**
     * Authenticated restTemplate;
     */
    protected TestRestTemplate authRest;

    @BeforeAll
    static void setupWorkdir() throws Exception {
        Files.createDirectories(Paths.get("temp_work_dir"));
    }

    @BeforeEach
    void setupAuthentication() {
        authRest = rest.withBasicAuth(serverConf.getDefaultUser().getName(), serverConf.getDefaultUser().getPassword());
    }

    @AfterAll
    static void deleteWorkDir() throws Exception {
        File temp_work_dir = new File("temp_work_dir");
        FileUtils.deleteQuietly(temp_work_dir);
        FileUtils.forceDeleteOnExit(temp_work_dir);
    }
}
