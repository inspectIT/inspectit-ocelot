package rocks.inspectit.ocelot;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "inspectit.working-directory=temp_work_dir")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class IntegrationTestBase {

    @Autowired
    protected TestRestTemplate rest;

    @AfterAll
    static void deleteWorkDir() throws Exception {
        FileUtils.deleteDirectory(new File("temp_work_dir"));
    }
}
