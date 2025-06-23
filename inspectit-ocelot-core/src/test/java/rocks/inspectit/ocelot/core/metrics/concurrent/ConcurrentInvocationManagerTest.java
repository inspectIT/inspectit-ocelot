package rocks.inspectit.ocelot.core.metrics.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static rocks.inspectit.ocelot.core.metrics.concurrent.ConcurrentInvocationManager.DEFAULT_OPERATION;

public class ConcurrentInvocationManagerTest {

    private ConcurrentInvocationManager manager;

    private final String OPERATION = "operation-name";

    @BeforeEach
    void beforeEach() {
        manager = new ConcurrentInvocationManager();
    }

    @Test
    void shouldAddInvocation() {
        manager.addInvocation(OPERATION);

        Map<String, Long> invocations = manager.getActiveInvocations();

        assertEquals(1, invocations.size());
        assertEquals(1, invocations.get(OPERATION));
    }

    @Test
    void shouldAddDefaultInvocationWhenNull() {
        manager.addInvocation(null);

        Map<String, Long> invocations = manager.getActiveInvocations();

        assertEquals(1, invocations.size());
        assertEquals(1, invocations.get(DEFAULT_OPERATION));
    }

    @Test
    void shouldAddDefaultInvocationWhenEmpty() {
        manager.addInvocation("");

        Map<String, Long> invocations = manager.getActiveInvocations();

        assertEquals(1, invocations.size());
        assertEquals(1, invocations.get(DEFAULT_OPERATION));
    }

    @Test
    void shouldRemoveInvocation() {
        manager.addInvocation(OPERATION);
        manager.removeInvocation(OPERATION);

        Map<String, Long> invocations = manager.getActiveInvocations();

        assertEquals(1, invocations.size());
        assertEquals(0, invocations.get(OPERATION));
    }

    @Test
    void shouldRemoveDefaultInvocationWhenNull() {
        manager.addInvocation(null);
        manager.removeInvocation(null);

        Map<String, Long> invocations = manager.getActiveInvocations();

        assertEquals(1, invocations.size());
        assertEquals(0, invocations.get(DEFAULT_OPERATION));
    }

    @Test
    void shouldRemoveDefaultInvocationWhenEmpty() {
        manager.addInvocation("");
        manager.removeInvocation("");

        Map<String, Long> invocations = manager.getActiveInvocations();

        assertEquals(1, invocations.size());
        assertEquals(0, invocations.get(DEFAULT_OPERATION));
    }

    @Test
    void shouldStayEmptyWhenNoInvocationsExists() {
        manager.removeInvocation(OPERATION);

        Map<String, Long> invocations = manager.getActiveInvocations();

        assertEquals(0, invocations.size());
    }
}
