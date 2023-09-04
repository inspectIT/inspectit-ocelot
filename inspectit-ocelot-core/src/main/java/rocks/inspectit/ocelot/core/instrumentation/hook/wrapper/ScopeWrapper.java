package rocks.inspectit.ocelot.core.instrumentation.hook.wrapper;

import io.opentelemetry.context.Scope;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ScopeWrapper implements Scope {

    /**
     * Object, which is wrapped by this class
     */
    private Scope scope;

    /**
     * Additional Autocloseable to define custom close()-functions
     */
    private AutoCloseable autoCloseable;

    @Override
    public void close() {
        // If present, call custom close()
        if(autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Throwable e) {
                log.error("Error closing span scope", e);
            }
        }
        // Call ordinary close()
        scope.close();
    }
}
