package rocks.inspectit.ocelot.utils;

import com.google.common.cache.Cache;
import io.opentelemetry.api.baggage.Baggage;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.bootstrap.AgentManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * We access data from inside the agent via reflection to check for specific events like
 * instrumenting a class
 */
public class TestUtils {
    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    /**
     * Active instrumentations from the {@code InstrumentationManager} in inspectit-ocelot-core
     */
    private static Cache<Class<?>, Object> activeInstrumentations = null;

    /**
     * Stores for each class the time, when we have discovered its instrumentation
     */
    public static ConcurrentHashMap<Class<?>, Long> instrumentationTimeStamp = new ConcurrentHashMap<>();

    /**
     * Object used to access specific Class objects
     */
    public static Object sink;

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

                activeInstrumentations = (Cache<Class<?>, Object>) getField(instrumentationManager.getClass(), "activeInstrumentations").get(instrumentationManager);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return activeInstrumentations;
    }

    /**
     * Waits until all specified classes were instrumented.
     * This does not wait for potential hooks which will be created.
     */
    public static void waitForClassInstrumentations(Class<?>... clazz) {
        waitForClassInstrumentations(Arrays.asList(clazz), false, 15, TimeUnit.SECONDS);
    }

    /**
     * Waits until a hook for each of the given classes exist.
     */
    public static void waitForClassHooks(Class<?>... clazz) {
        waitForClassHooks(Arrays.asList(clazz), 10, TimeUnit.SECONDS);
    }

    /**
     * See {@link #waitForClassInstrumentations(List, boolean, int, TimeUnit)}
     */
    public static void waitForClassInstrumentation(Class<?> clazz, boolean waitForHooks, int duration, TimeUnit timeUnit) {
        waitForClassInstrumentations(Collections.singletonList(clazz), waitForHooks, duration, timeUnit);
    }

    /**
     * This methods will wait until all specified classes are present in the inspectIT agents activeInstrumentation cache.
     * After the specified time, the method will cause the current test to fail.
     */
    public static void waitForClassInstrumentations(List<Class<?>> clazzes, boolean waitForHooks, int duration, TimeUnit timeUnit) {
        waitForClassInstrumentations(clazzes, duration, timeUnit);
        if (waitForHooks) {
            waitForClassHooks(clazzes, duration, timeUnit);
        }
    }

    public static void waitForClassInstrumentations(List<Class<?>> clazzes, int duration, TimeUnit timeUnit) {
        try {
            await().atMost(duration, timeUnit).ignoreExceptions().untilAsserted(() -> {
                for (Class<?> clazz : clazzes) {
                    sink = Class.forName(clazz.getName(), true, clazz.getClassLoader());
                    Long timeStamp = instrumentationTimeStamp.get(clazz);
                    assertThat(timeStamp).isNotNull();
                }
            });
        } catch (ConditionTimeoutException ex) {
            int missingClassCount = 0;
            for (Class<?> clazz : clazzes) {
                Long timeStamp = instrumentationTimeStamp.get(clazz);
                if (timeStamp == null) {
                    logger.info("{} was not instrumented!", clazz.getName());
                    missingClassCount++;
                }
            }

            if (missingClassCount > 0) { // it may be the case that all required classes are loaded now
                throw ex;
            }
        }
    }

    /**
     * Waits a certain amount of time until a hook exists for each given class.
     */
    public static void waitForClassHooks(List<Class<?>> clazzes, int duration, TimeUnit timeUnit) {
        try {
            await().atMost(duration, timeUnit).until(() -> {
                Map<Class<?>, Object> hooks = getHooksMap();
                return clazzes.stream().allMatch(hooks::containsKey);
            });
        } catch (ConditionTimeoutException ex) {
            Map<Class<?>, Object> hooksMap = getHooksMap();
            for (Class<?> clazz : clazzes) {
                if (!hooksMap.containsKey(clazz)) {
                    logger.info("No hookes were created for class {}", clazz.getName());
                }
            }
            throw ex;
        }
    }

    /**
     * Checks, if we have instrumented classes and there are no classes in the instrumentation-queue left
     */
    public static void waitForInstrumentationToComplete() {
        await().atMost(30, TimeUnit.SECONDS).ignoreExceptions().untilAsserted(() -> {
            assertThat(MetricTestUtils.getInstrumentationClassesCount()).isGreaterThan(0);
            assertThat(MetricTestUtils.getInstrumentationQueueSize()).isZero();
            Thread.sleep(500); // to ensure that new-class-discovery has been executed
            assertThat(MetricTestUtils.getInstrumentationQueueSize()).isZero();
            Thread.sleep(500);
        });
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

    /**
     * Waits until the {@code TimeWindowRecorder} has recorded all values
     */
    public static void waitForTimeWindowRecorder() {
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Collection<?> recordsQueue = getTimeWindowRecordsQueue();
            assertThat(recordsQueue.isEmpty());
        });
    }

    private static Collection<?> getTimeWindowRecordsQueue() {
        waitForAgentInitialization();
        try {
            Object agentInstance = getField(AgentManager.class, "agentInstance").get(null);
            Object ctx = getField(agentInstance.getClass(), "ctx").get(agentInstance);

            Method getBean = ctx.getClass().getMethod("getBean", String.class);
            getBean.setAccessible(true);
            Object timeWindowRecorder = getBean.invoke(ctx, "timeWindowRecorder");

            return (Collection<?>) getField(timeWindowRecorder.getClass(), "recordsQueue").get(timeWindowRecorder);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Map<String, String> getCurrentAttributesAsMap() {
        Map<String, String> result = new HashMap<>();
        Baggage current = Baggage.current();
        current.asMap().forEach((key, valueEntry) -> result.put(key, valueEntry.getValue()));
        return result;
    }
}
