package rocks.inspectit.oce.eum.server.exporters.beacon;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class BeaconHttpExporterAnnotationsTest {

    @TestPropertySource(properties = {"inspectit-eum-server.exporters.beacons.http.enabled=ENABLED"})
    @Nested
    public class Enabled {

        @Autowired
        BeaconHttpExporter exporter;

        @Autowired
        ExportWorkerFactory factory;

        @Test
        public void testBeanWasCreated() {
            assertThat(exporter).isNotNull();
            assertThat(factory).isNotNull();
        }
    }

    @TestPropertySource(properties = {"inspectit-eum-server.exporters.beacons.http.enabled=DISABLED"})
    @Nested
    public class Disabled {

        @Autowired(required = false)
        BeaconHttpExporter exporter;

        @Autowired(required = false)
        ExportWorkerFactory factory;

        @Test
        public void testBeanWasNotCreated() {
            assertThat(exporter).isNull();
            assertThat(factory).isNull();
        }
    }

}
