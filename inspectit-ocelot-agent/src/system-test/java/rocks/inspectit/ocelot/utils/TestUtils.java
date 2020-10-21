package rocks.inspectit.ocelot.utils;

import com.google.common.cache.Cache;
import io.opencensus.impl.internal.DisruptorEventQueue;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.Metrics;
import io.opencensus.metrics.export.TimeSeries;
import io.opencensus.stats.*;
import io.opencensus.tags.InternalUtils;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.awaitility.core.ConditionTimeoutException;
import rocks.inspectit.ocelot.bootstrap.AgentManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class TestUtils {

    private static Cache<Class<?>, Object> activeInstrumentations = null;

    public static ConcurrentHashMap<Class<?>, Long> instrumentationTimeStamp = new ConcurrentHashMap<>();

    static {
        Thread poller = new Thread(() -> {
            while (true) {
                for (Class<?> cl : getInstrumentationCache().asMap().keySet()) {
                    if (!instrumentationTimeStamp.containsKey(cl)) {
                        //we remember when a class first appeared in the cache
                        instrumentationTimeStamp.put(cl, System.currentTimeMillis());
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        poller.setDaemon(true);
        poller.start();
    }

    public static Object sink;

    private static Field getField(Class clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static synchronized Map<Class<?>, Object> getHooksMap() {
        waitForAgentInitialization();
        try {
            Object agentInstance = getField(AgentManager.class, "agentInstance").get(null);
            Object ctx = getField(agentInstance.getClass(), "ctx").get(agentInstance);

            Method getBean = ctx.getClass().getMethod("getBean", String.class);
            getBean.setAccessible(true);
            Object hookManager = getBean.invoke(ctx, "hookManager");

            return (Map<Class<?>, Object>) getField(hookManager.getClass(), "hooks").get(hookManager);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static synchronized Cache<Class<?>, Object> getInstrumentationCache() {
        if (activeInstrumentations == null) {
            // to prevent race conditions
            waitForAgentInitialization();
            try {
                Object agentInstance = getField(AgentManager.class, "agentInstance").get(null);
                Object ctx = getField(agentInstance.getClass(), "ctx").get(agentInstance);

                Method getBean = ctx.getClass().getMethod("getBean", String.class);
                getBean.setAccessible(true);
                Object instrumentationManager = getBean.invoke(ctx, "instrumentationManager");

                activeInstrumentations = (Cache<Class<?>, Object>) getField(instrumentationManager.getClass(), "activeInstrumentations")
                        .get(instrumentationManager);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return activeInstrumentations;
    }

    /**
     * See {@link #waitForClassInstrumentations(List, int, TimeUnit)}
     */
    public static void waitForClassInstrumentation(Class clazz, boolean waitForHooks, int duration, TimeUnit timeUnit) {
        waitForClassInstrumentations(Collections.singletonList(clazz), waitForHooks, duration, timeUnit);
    }

    /**
     * This methods will wait until all specified classes are present in the inspectIT agents activeInstrumentation cache.
     * After the specified time, the method will cause the current test to fail.
     */
    public static void waitForClassInstrumentations(List<Class> clazzes, boolean waitForHooks, int duration, TimeUnit timeUnit) {
        try {
            await().atMost(duration, timeUnit).ignoreExceptions().untilAsserted(() -> {
                for (Class clazz : clazzes) {
                    sink = Class.forName(clazz.getName(), true, clazz.getClassLoader());
                    Long timeStamp = instrumentationTimeStamp.get(clazz);
                    assertThat(timeStamp).isNotNull();
                }
            });
            if (waitForHooks) {
                await().atMost(duration, timeUnit).until(() -> {
                    Map<Class<?>, Object> hooks = getHooksMap();
                    return clazzes.stream().allMatch(hooks::containsKey);
                });
            }
        } catch (ConditionTimeoutException ex) {
            for (Class clazz : clazzes) {
                Long timeStamp = instrumentationTimeStamp.get(clazz);
                if (timeStamp == null) {
                    System.out.println(clazz.getName() + " was not instrumented!");
                }
            }
            throw ex;
        }
    }

    /**
     * OpenCensus internally manages a queue of events.
     * We simply add an event to the queue and wait until it is processed.
     */
    public static void waitForOpenCensusQueueToBeProcessed() {
        CountDownLatch latch = new CountDownLatch(1);
        DisruptorEventQueue.getInstance().enqueue(latch::countDown);
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void waitForAgentInitialization() {
        try {
            while (!AgentManager.isInitialized()) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
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
     *
     * @return the found aggregation data, null otherwise
     */
    public static AggregationData getDataForView(String viewName, Map<String, String> tags) {
        ViewManager viewManager = Stats.getViewManager();
        ViewData view = viewManager.getView(View.Name.create(viewName));
        List<String> orderedTagKeys = view.getView()
                .getColumns()
                .stream()
                .map(TagKey::getName)
                .collect(Collectors.toList());
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

    public static TimeSeries getTimeseries(String metricName, Map<String, String> tags) {
        Optional<TimeSeries> series = Metrics.getExportComponent()
                .getMetricProducerManager()
                .getAllMetricProducer()
                .stream()
                .flatMap(mp -> mp.getMetrics().stream())
                .filter(m -> m.getMetricDescriptor().getName().equals(metricName))
                .flatMap(m -> {
                    List<String> orderedTagKeys = m.getMetricDescriptor()
                            .getLabelKeys().stream()
                            .map(LabelKey::getKey)
                            .collect(Collectors.toList());
                    assertThat(orderedTagKeys).contains(tags.keySet().toArray(new String[]{}));
                    List<String> expectedTagValues = orderedTagKeys.stream()
                            .map(tags::get)
                            .collect(Collectors.toList());
                    return m.getTimeSeriesList().stream()
                            .filter(ts -> {
                                List<LabelValue> tagValues = ts.getLabelValues();
                                for (int i = 0; i < tagValues.size(); i++) {
                                    String regex = expectedTagValues.get(i);
                                    LabelValue tagValue = tagValues.get(i);
                                    if (regex != null && (tagValue == null || !tagValue.getValue().matches(regex))) {
                                        return false;
                                    }
                                }
                                return true;
                            });
                })
                .findFirst();
        assertThat(series).isNotEmpty();
        return series.get();
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

