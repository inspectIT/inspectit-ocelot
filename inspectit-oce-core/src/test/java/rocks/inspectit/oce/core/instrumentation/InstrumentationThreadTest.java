package rocks.inspectit.oce.core.instrumentation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.*;

class InstrumentationThreadTest {

    @Mock
    InstrumentationManager instrumentationManager;

    @InjectMocks
    InstrumentationThread thread;

    @Nested
    public class Run {

        @Test
        void waitForConfiguration() {
            MockitoAnnotations.initMocks(InstrumentationThreadTest.this);
            when(instrumentationManager.isNewInstrumentationAvailable()).thenReturn(false);

            assertTimeout(ofSeconds(5), () -> {
                thread.start();


                while (thread.getState() != Thread.State.WAITING) {
                    Thread.yield();
                    Thread.sleep(1);
                }

                verify(instrumentationManager).isNewInstrumentationAvailable();
                verifyNoMoreInteractions(instrumentationManager);
            });
        }

        @Test
        void triggerInstrumentation() {
            MockitoAnnotations.initMocks(InstrumentationThreadTest.this);
            when(instrumentationManager.isNewInstrumentationAvailable()).thenReturn(true, false);

            assertTimeout(ofSeconds(5), () -> {
                thread.start();

                while (thread.getState() != Thread.State.WAITING) {
                    Thread.yield();
                    Thread.sleep(1);
                }

                InOrder inOrder = Mockito.inOrder(instrumentationManager);
                inOrder.verify(instrumentationManager).isNewInstrumentationAvailable();
                inOrder.verify(instrumentationManager).resetInstrumentation();
                inOrder.verify(instrumentationManager).applyInstrumentation(anyBoolean());
                inOrder.verify(instrumentationManager).isNewInstrumentationAvailable();
                verifyNoMoreInteractions(instrumentationManager);
            });
        }

        @Test
        void removeInstrumentation() {
            MockitoAnnotations.initMocks(InstrumentationThreadTest.this);
            when(instrumentationManager.isNewInstrumentationAvailable()).thenReturn(false);

            assertTimeout(ofSeconds(5), () -> {
                thread.start();

                while (thread.getState() != Thread.State.WAITING) {
                    Thread.yield();
                    Thread.sleep(1);
                }

                thread.setExitFlag(true);
                synchronized (thread) {
                    thread.notify();
                }

                while (thread.getState() != Thread.State.TERMINATED) {
                    Thread.yield();
                    Thread.sleep(1);
                }

                verify(instrumentationManager).isNewInstrumentationAvailable();
                verify(instrumentationManager).resetInstrumentation();
                verifyNoMoreInteractions(instrumentationManager);
            });
        }

        @Test
        void onInterruptException() {
            MockitoAnnotations.initMocks(InstrumentationThreadTest.this);
            when(instrumentationManager.isNewInstrumentationAvailable()).thenReturn(false);

            assertTimeout(ofSeconds(5), () -> {
                thread.start();

                while (thread.getState() != Thread.State.WAITING) {
                    Thread.yield();
                    Thread.sleep(1);
                }

                thread.interrupt();

                while (thread.getState() != Thread.State.TERMINATED) {
                    Thread.yield();
                    Thread.sleep(1);
                }

                verify(instrumentationManager).isNewInstrumentationAvailable();
                verify(instrumentationManager).resetInstrumentation();
                verifyNoMoreInteractions(instrumentationManager);
            });
        }
    }
}