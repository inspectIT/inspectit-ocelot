package rocks.inspectit.ocelot.core.testutils;

import io.opencensus.trace.Tracing;
import io.opencensus.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.lang.reflect.Method;

public class OpenCensusUtils {

    private static final Logger logger = LoggerFactory.getLogger(OpenCensusUtils.class);

    /**
     * Flushes the span exporters.
     *
     * This method is no longer supported. Please us {@link OpenTelemetryUtils#flush()} instead.
     */
    @Deprecated
    public static void flushSpanExporter() {
        try {
            SpanExporter exporter = Tracing.getExportComponent().getSpanExporter();
            Method flushMethod = exporter.getClass().getDeclaredMethod("flush");
            flushMethod.setAccessible(true);
            flushMethod.invoke(exporter);
        } catch (Exception e) {
            logger.warn("Could not flush OpenCensus span exporters.", e);
        }
    }
}
