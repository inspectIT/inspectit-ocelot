package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationPattern;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.opentelemetry.DynamicMultiSpanExporter;
import rocks.inspectit.ocelot.core.opentelemetry.OpenTelemetryControllerImpl;
import rocks.inspectit.ocelot.core.privacy.obfuscation.ObfuscationManager;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for exporting span with obfuscated attributes to ensure that no exceptions are thrown by the {@link rocks.inspectit.ocelot.core.opentelemetry.DynamicMultiSpanExporter} when obfuscation is activated and OTLP trace exporters are enabled.
 */
@DirtiesContext
public class SpanAttributeObfuscationExportIntTest extends SpringTestBase {

    @Autowired
    ObfuscationManager obfuscationManager;

    @Autowired
    OpenTelemetryControllerImpl openTelemetryController;

    @RegisterExtension
    LogCapturer dynamicMultiSpanExporterErrorLogs = LogCapturer.create()
            .captureForType(DynamicMultiSpanExporter.class, Level.ERROR);

    @BeforeEach
    void setup() {
        updateProperties(mps -> {
            mps.setProperty("inspectit.privacy.obfuscation.enabled", true);
            ObfuscationPattern obfuscationPattern = new ObfuscationPattern();
            obfuscationPattern.setPattern("username");
            mps.setProperty("inspectit.privacy.obfuscation.patterns", new ArrayList<ObfuscationPattern>() {
                {
                    add(obfuscationPattern);
                }
            });
            mps.setProperty("inspectit.exporters.tracing.otlp.protocol", TransportProtocol.GRPC);
            mps.setProperty("inspectit.exporters.tracing.otlp.endpoint", "localhost:4317");
            mps.setProperty("inspectit.exporters.tracing.otlp.enabled", ExporterEnabledState.ENABLED);
            mps.setProperty("inspectit.exporters.tracing.logging.enabled", ExporterEnabledState.ENABLED);
        });
    }

    @Test
    void testMe() {
        assertThat(obfuscationManager.obfuscatorySupplier().get()).isNotNull();

        Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> openTelemetryController.isActive());

        Span span = OpenTelemetryUtils.getTracer().spanBuilder("my-span").startSpan();

        try (Scope s = span.makeCurrent()) {
            obfuscationManager.obfuscatorySupplier()
                    .get()
                    .putSpanAttribute(span, "exception", new RuntimeException("this-is-runtime"));
            obfuscationManager.obfuscatorySupplier().get().putSpanAttribute(span, "username", "inspectit-ocelot");

        } catch (Exception e) {
            System.err.println(e);
        } finally {
            span.end();
        }
        OpenTelemetryUtils.flush();

        dynamicMultiSpanExporterErrorLogs.assertDoesNotContain("execute");
        assertThat(span.getSpanContext().isValid()).isTrue();

    }

    @Test
    void testExceptionThrown() {
        Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> openTelemetryController.isActive());

        // add unsupported attribute value to the span
        Span span = OpenTelemetryUtils.getTracer().spanBuilder("exception-span").startSpan();
        try (Scope s = span.makeCurrent()) {
            AttributeKey attributeKey = AttributeKey.stringKey("exception");
            span.setAttribute(attributeKey, new RuntimeException("this-is-an-exception"));
        } finally {
            span.end();
        }
        OpenTelemetryUtils.flush();
        // assert that the DynamicMultiSpanExporter throws the underlying ClassCastException in the execute method.
        dynamicMultiSpanExporterErrorLogs.assertContains("execute");
    }

}
