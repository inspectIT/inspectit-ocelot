package rocks.inspectit.ocelot.core.config.filebased;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ConfigDirWatchingIntTest {

    static final String tmpDir = "tmpconf_int_test";

    @BeforeAll
    static void createConfigDir() {
        new File(tmpDir).mkdir();
    }

    @BeforeEach
    void cleanConfigDir() throws Exception {
        FileUtils.cleanDirectory(new File(tmpDir));
    }

    @AfterAll
    static void deleteConfigDir() throws Exception {
        FileUtils.deleteDirectory(new File(tmpDir));
    }

    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {
            "inspectit.config.file-based.path=" + ConfigDirWatchingIntTest.tmpDir,
            "inspectit.config.file-based.frequency=50ms"
    })
    class Polling extends SpringTestBase {

        @Autowired
        InspectitEnvironment env;

        @Test
        public void testNewFilesDetected() throws Exception {
            FileUtils.write(new File(tmpDir + "/A.properties"), "inspectit.service-name=FromAproperties", Charset.defaultCharset());
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("FromAproperties")
            );
        }


        @Test
        public void testFileChangesDetected() throws Exception {
            FileUtils.write(new File(tmpDir + "/A.yml"), "inspectit.service-name: FromAproperties", Charset.defaultCharset());
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("FromAproperties")
            );
            FileUtils.write(new File(tmpDir + "/A.yml"), "inspectit.service-name: newname", Charset.defaultCharset());
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("newname")
            );
        }

        @Test
        public void testFileDeleted() throws Exception {
            FileUtils.write(new File(tmpDir + "/A.yml"), "inspectit.service-name: FromAproperties", Charset.defaultCharset());
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("FromAproperties")
            );
            await().atMost(15, TimeUnit.SECONDS).until(() ->
                    FileUtils.deleteQuietly(new File(tmpDir + "/A.yml")));
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("InspectIT Agent")
            );
        }
    }


    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {
            "inspectit.config.file-based.path=" + ConfigDirWatchingIntTest.tmpDir,
            "inspectit.config.file-based.frequency=0ms"
    })
    class Watching extends SpringTestBase {

        @Autowired
        InspectitEnvironment env;

        @Test
        public void testNewFilesDetected() throws Exception {
            FileUtils.write(new File(tmpDir + "/A.properties"), "inspectit.service-name=FromAproperties", Charset.defaultCharset());
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("FromAproperties")
            );
        }


        @Test
        public void testFileChangesDetected() throws Exception {
            FileUtils.write(new File(tmpDir + "/A.yml"), "inspectit.service-name: FromAproperties", Charset.defaultCharset());
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("FromAproperties")
            );
            FileUtils.write(new File(tmpDir + "/A.yml"), "inspectit.service-name: newname", Charset.defaultCharset());
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("newname")
            );
        }

        @Test
        public void testFileDeleted() throws Exception {
            FileUtils.write(new File(tmpDir + "/A.yml"), "inspectit.service-name: FromAproperties", Charset.defaultCharset());
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("FromAproperties")
            );
            await().atMost(15, TimeUnit.SECONDS).until(() ->
                    FileUtils.deleteQuietly(new File(tmpDir + "/A.yml")));
            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("InspectIT Agent")
            );
        }
    }


}
