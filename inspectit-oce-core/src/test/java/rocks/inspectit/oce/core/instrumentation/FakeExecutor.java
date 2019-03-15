package rocks.inspectit.oce.core.instrumentation;

import java.util.concurrent.Executor;

/**
 * Used for {@link InstrumentationTriggererIntTest}.
 */
public class FakeExecutor implements Executor {
    @Override
    public void execute(Runnable command) {

    }
}
