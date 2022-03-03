package rocks.inspectit.oce.eum.server.exporters.configuration;

import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TraceExportersConfigurationTest {

    @TestPropertySource(properties = {"inspectit-eum-server.exporters.tracing.jaeger.grpc=", "inspectit-eum-server.exporters.tracing.jaeger.enabled=ENABLED"})
    @Nested
    public class MissingGrpcTest {

        @Autowired(required = false)
        JaegerGrpcSpanExporter exporter;

        @Test
        public void testBeanWasNotCreated() {
            assertThat(exporter).isEqualTo(null);
        }
    }

    @TestPropertySource(properties = {"inspectit-eum-server.exporters.tracing.jaeger.grpc=localhost:1234", "inspectit-eum-server.exporters.tracing.jaeger.enabled=DISABLED"})
    @Nested
    public class DisabledTest {

        @Autowired(required = false)
        JaegerGrpcSpanExporter exporter;

        @Test
        public void testBeanWasNotCreated() {
            assertThat(exporter).isEqualTo(null);
        }
    }

    @TestPropertySource(properties = {"inspectit-eum-server.exporters.tracing.jaeger.grpc=localhost:1234", "inspectit-eum-server.exporters.tracing.jaeger.enabled=ENABLED"})
    @Nested
    public class BothAvailableTest {

        @Autowired
        JaegerGrpcSpanExporter exporter;

        @Test
        public void testBeanWasGetCreated() {
            assertThat(exporter).isNotEqualTo(null);
        }
    }

}