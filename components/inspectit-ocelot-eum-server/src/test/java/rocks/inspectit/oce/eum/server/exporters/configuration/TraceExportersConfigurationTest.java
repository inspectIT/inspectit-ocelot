package rocks.inspectit.oce.eum.server.exporters.configuration;

import io.opencensus.trace.export.SpanExporter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TraceExportersConfigurationTest {

    @TestPropertySource(properties = {"inspectit-eum-server.exporters.tracing.jaeger.grpc=", "inspectit-eum-server.exporters.tracing.jaeger.enabled=ENABLED"})
    @Nested
    public class MissingGrpcTest {

        @Autowired(required = false)
        List<SpanExporter> spanExporters;

        @Test
        public void testDisabled() {
            assertThat(spanExporters).isEqualTo(null);
        }
    }

    @TestPropertySource(properties = {"inspectit-eum-server.exporters.tracing.jaeger.grpc=localhost:1234", "inspectit-eum-server.exporters.tracing.jaeger.enabled=DISABLED"})
    @Nested
    public class DisabledTest {

        @Autowired(required = false)
        List<SpanExporter> spanExporters;

        @Test
        public void testDisabled() {
            assertThat(spanExporters).isEqualTo(null);
        }
    }

    @TestPropertySource(properties = {"inspectit-eum-server.exporters.tracing.jaeger.grpc=localhost:1234", "inspectit-eum-server.exporters.tracing.jaeger.enabled=ENABLED"})
    @Nested
    public class BothAvailableTest {

        @Autowired
        SpanExporter spanExporters;

        @Test
        public void testDisabled() {
            assertThat(spanExporters).isNotEqualTo(null);
        }
    }

}