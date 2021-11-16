package rocks.inspectit.ocelot.core.selfmonitoring;

/**
 * Dummy no-op {@link IActionScope}
 */
public class NoopActionScope implements IActionScope {

    public static final NoopActionScope INSTANCE = new NoopActionScope();

    private NoopActionScope(){

    }
    
    @Override
    public void start() {

    }

    @Override
    public void start(long startTimeNanos) {

    }

    @Override
    public void close() {

    }
}
