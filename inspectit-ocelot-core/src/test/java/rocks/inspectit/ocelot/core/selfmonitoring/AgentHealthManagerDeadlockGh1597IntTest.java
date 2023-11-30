package rocks.inspectit.ocelot.core.selfmonitoring;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.logging.logback.LogbackInitializer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Regression test for <a href="https://github.com/inspectIT/inspectit-ocelot/issues/1597">Github issue 1597</a>.
 */
@DirtiesContext
public class AgentHealthManagerDeadlockGh1597IntTest extends SpringTestBase {

    private static final Logger logger = LoggerFactory.getLogger("test-logger");

    @Autowired
    private AgentHealthManager cut;

    @Test
    void testLogging() {
        // This installs InternalProcessingAppender which together with AgentHealthManager caused a deadlock
        LogbackInitializer.initDefaultLogging();

        int millisToRun = 500;
        Thread logThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            boolean shouldLogError = true;
            while (System.currentTimeMillis() - start < millisToRun) {
                if (shouldLogError) {
                    logger.error("Reporting an error");
                    shouldLogError = false;
                } else {
                    logger.info("Reporting an info");
                    shouldLogError = true;
                }

            }
        });

        AtomicBoolean isInvalidationThreadDone = new AtomicBoolean(false);

        Thread invalidationThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < millisToRun) {
                cut.invalidateIncident(cut.getClass(), "Invalidation due to invalidator event");
            }
            isInvalidationThreadDone.getAndSet(true);
        });

        // letting those two threads run for a while caused the deadlock on the developers machine before the fix.
        // This is far from an ideal test, but the best we came up with.
        logThread.start();
        invalidationThread.start();

        Awaitility.waitAtMost(millisToRun * 2, TimeUnit.MILLISECONDS).until(isInvalidationThreadDone::get);
    }

}
