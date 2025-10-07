package rocks.inspectit.ocelot.utils;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.opentelemetry.NoopOpenTelemetryController;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Utils to set up and read OpenTelemetry metric data
 */
public class MetricTestUtils {

    /**
     * MetricReader which is able to read recorded metrics instantly
     */
    private static InMemoryMetricReader metricReader;

    private final static String instrumentationQueueSize = "inspectit_self_instrumentation_queue_size";

    private final static String instrumentedClassesSize = "inspectit_self_instrumented_classes";

    /**
     * Initializes the {@link InMemoryMetricReader} once and registered it at the {@code OpenTelemetryControllerImpl}.
     * This should happen only once at the start of the system tests! After that we will reuse the configured
     * {@code OpenTelemetryControllerImpl} with our {@link InMemoryMetricReader}.
     */
    public static void initializeMetricReader() {
        // if OTel was already initialized with the MetricReader, just return it
        if (null != metricReader && NoopOpenTelemetryController.INSTANCE != Instances.openTelemetryController) {
            metricReader.forceFlush();
            return;
        }

        // wait until OTel is initialized
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> NoopOpenTelemetryController.INSTANCE != Instances.openTelemetryController);

        // create an InMemoryMetricReader and register it with OTel
        metricReader = InMemoryMetricReader.create();
        Instances.openTelemetryController.registerMetricReader(metricReader, "InMemoryMetricReader");
    }

    /**
     * Checks, if we have instrumented classes and there are no classes in the instrumentation-queue left
     */
    public static void waitForInstrumentationToComplete() {
        await().atMost(20, TimeUnit.SECONDS).ignoreExceptions().untilAsserted(() -> {
            assertThat(getInstrumentationClassesCount()).isGreaterThan(0);
            assertThat(getInstrumentationQueueSize()).isZero();
            Thread.sleep(500); // to ensure that new-class-discovery has been executed
            assertThat(getInstrumentationQueueSize()).isZero();
            Thread.sleep(500);
        });
    }

    /**
     * @return The point data for the gauge view with the provided attributes
     */
    public static PointData getDataForView(String viewName, Map<String, String> attributes) {
        Collection<MetricData> filteredData = getFilteredData(viewName);

        Collection<PointData> results = filteredData.stream()
                .flatMap(metricData -> metricData.getData().getPoints().stream())
                .filter(pointData -> containsAttributes(pointData.getAttributes(), attributes))
                .collect(Collectors.toList());

        Optional<PointData> maybeResult = results.stream().findFirst();
        return maybeResult.orElse(null);
    }

    /**
     * @return The point data for the histogram view with the provided attributes
     */
    public static HistogramPointData getDataForHistogramView(String viewName, Map<String, String> attributes) {
        Collection<MetricData> filteredData = getFilteredData(viewName);

        Collection<HistogramPointData> results = filteredData.stream()
                .flatMap(metricData -> metricData.getHistogramData().getPoints().stream())
                .filter(pointData -> containsAttributes(pointData.getAttributes(), attributes))
                .collect(Collectors.toList());

        Optional<HistogramPointData> maybeResult = results.stream().findFirst();
        return maybeResult.orElse(null);
    }

    private static Collection<MetricData> getFilteredData(String viewName) {
        Collection<MetricData> data = getRecordedMetrics();

        return data.stream()
                .filter(metricData -> metricData.getName().equals(viewName))
                .collect(Collectors.toList());
    }

    public static long getInstrumentationQueueSize() {
        return getLongData(instrumentationQueueSize);
    }

    public static long getInstrumentationClassesCount() {
        return getLongData(instrumentedClassesSize);
    }

    private static long getLongData(String metricName) {
        Collection<MetricData> data = getRecordedMetrics();
        Collection<MetricData> filteredData = data.stream()
                .filter(metricData -> metricData.getName().equals(metricName))
                .collect(Collectors.toList());

        Optional<Long> maybeSize = filteredData.stream()
                .map(MetricTestUtils::getLastLongPointDataValue).findFirst();

        if (maybeSize.isPresent())
            return maybeSize.get();
        else
            throw new RuntimeException("No point data found for " + metricName);
    }

    private static long getLastLongPointDataValue(MetricData metricData) {
        List<LongPointData> pointDataList = new ArrayList<>(metricData.getLongGaugeData().getPoints());
        LongPointData pointData = pointDataList.get(pointDataList.size() - 1);
        return pointData.getValue();
    }

    /**
     * Checks, if the attributes contains all expected key value pairs
     */
    private static boolean containsAttributes(Attributes attributes, Map<String, String> expected) {
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            AttributeKey<String> key = stringKey(entry.getKey());
            String expectedValue = entry.getValue();
            String attributeValue = attributes.get(key);

            if (attributeValue == null || !attributeValue.equals(expectedValue)) {
                return false;
            }
        }
        return true;
    }

    private static Collection<MetricData> getRecordedMetrics() {
        return metricReader.collectAllMetrics();
    }
}
