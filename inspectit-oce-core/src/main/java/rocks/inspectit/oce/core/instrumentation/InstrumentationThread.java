package rocks.inspectit.oce.core.instrumentation;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This thread will trigger the runtime instrumentations, thus, they will be executed asynchronously.
 */
@Slf4j
public class InstrumentationThread extends Thread {

    private static final String THREAD_NAME = "inspectit-instrumenter";

    /**
     * A flag indicating that the instrumentation should be removed and this thread terminated.
     */
    @Setter
    private boolean exitFlag = false;

    private InstrumentationManager instrumentationManager;

    public InstrumentationThread(InstrumentationManager instrumentationManager) {
        this.instrumentationManager = instrumentationManager;
        setName(THREAD_NAME);
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!exitFlag) {
            synchronized (this) {
                while (!exitFlag && !instrumentationManager.isNewInstrumentationAvailable()) {
                    try {
                        wait();
                    } catch (InterruptedException exc) {
                        log.error("Exiting instrumentation thread due to interrupt");
                        exitFlag = true;
                    }
                }
            }

            instrumentationManager.resetInstrumentation();

            //only apply a new instrumentation if we are not exiting
            if (!exitFlag) {
                instrumentationManager.applyInstrumentation(true);
            }
        }
    }
}
