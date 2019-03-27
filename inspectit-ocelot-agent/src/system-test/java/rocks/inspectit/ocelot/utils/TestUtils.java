package rocks.inspectit.ocelot.utils;

import io.opencensus.impl.internal.DisruptorEventQueue;
import io.opencensus.stats.*;
import io.opencensus.tags.InternalUtils;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class TestUtils {

    /**
     * OpenCensus internally manages a queue of events.
     * We simply add an event to the queue and wait until it is processed.
     */
    public static void waitForOpenCensusQueueToBeProcessed() {
        CountDownLatch latch = new CountDownLatch(1);
        DisruptorEventQueue.getInstance().enqueue(latch::countDown);
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void waitForInstrumentationToComplete() {
        await().atMost(30, TimeUnit.SECONDS).ignoreExceptions().untilAsserted(() -> {
            assertThat(getInstrumentationClassesCount()).isGreaterThan(0);
            assertThat(getInstrumentationQueueLength()).isZero();
            Thread.sleep(200); //to ensure that new-class-discovery has been executed
            waitForOpenCensusQueueToBeProcessed();
            assertThat(getInstrumentationQueueLength()).isZero();
        });

    }

    public static Map<String, String> getCurrentTagsAsMap() {
        Map<String, String> result = new HashMap<>();
        InternalUtils.getTags(Tags.getTagger().getCurrentTagContext())
                .forEachRemaining(t -> result.put(t.getKey().getName(), t.getValue().asString()));
        return result;
    }

    /**
     * Returns the first found value for the view with the given tag values.
     *
     * @param viewName the name of the views
     * @param tags     the expected tag values
     * @return the found aggregation data, null otherwise
     */
    public static AggregationData getDataForView(String viewName, Map<String, String> tags) {
        ViewManager viewManager = Stats.getViewManager();
        ViewData view = viewManager.getView(View.Name.create(viewName));
        List<String> orderedTagKeys = view.getView().getColumns().stream().map(TagKey::getName).collect(Collectors.toList());
        assertThat(orderedTagKeys).contains(tags.keySet().toArray(new String[]{}));
        List<String> expectedTagValues = orderedTagKeys.stream().map(tags::get).collect(Collectors.toList());

        return view.getAggregationMap().entrySet()
                .stream()
                .filter(e -> {
                    List<TagValue> tagValues = e.getKey();
                    for (int i = 0; i < tagValues.size(); i++) {
                        String regex = expectedTagValues.get(i);
                        TagValue tagValue = tagValues.get(i);
                        if (regex != null && (tagValue == null || !tagValue.asString().matches(regex))) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    private static long getInstrumentationQueueLength() {
        ViewManager viewManager = Stats.getViewManager();
        AggregationData.LastValueDataLong queueSize =
                (AggregationData.LastValueDataLong)
                        viewManager.getView(View.Name.create("inspectit/self/instrumentation-queue-size"))
                                .getAggregationMap().values().stream()
                                .findFirst()
                                .get();
        return queueSize.getLastValue();
    }

    private static long getInstrumentationClassesCount() {
        ViewManager viewManager = Stats.getViewManager();
        AggregationData.LastValueDataLong queueSize =
                (AggregationData.LastValueDataLong)
                        viewManager.getView(View.Name.create("inspectit/self/instrumented-classes"))
                                .getAggregationMap().values().stream()
                                .findFirst()
                                .get();
        return queueSize.getLastValue();
    }

}

