package rocks.inspectit.ocelot.core.metrics.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void shouldRemoveInvocation() {
        manager.addInvocation(OPERATION);
        manager.addInvocation(OPERATION);
        manager.removeInvocation(OPERATION);

        Map<String, Long> invocations = manager.getActiveInvocations();

        assertEquals(1, invocations.size());
    }

    @Test
    void shouldRemoveInvocationCompletelyWhenReachingZero() {
        manager.addInvocation(OPERATION);
        manager.removeInvocation(OPERATION);

        Map<String, Long> invocations = manager.getActiveInvocations();

        assertEquals(0, invocations.size());
    }

    @Test
    void shouldStayEmptyWhenNoInvocationsExists() {
        manager.removeInvocation(OPERATION);

        Map<String, Long> invocations = manager.getActiveInvocations();

        assertEquals(0, invocations.size());
    }
}
