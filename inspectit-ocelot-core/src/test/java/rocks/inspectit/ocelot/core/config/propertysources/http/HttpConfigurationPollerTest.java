package rocks.inspectit.ocelot.core.config.propertysources.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.config.ConfigSettings;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpConfigurationPollerTest {

    @InjectMocks
    private HttpConfigurationPoller poller;

    @Mock
    private InspectitEnvironment env;

    @Mock
    private ScheduledExecutorService executor;

    @Nested
    public class DoEnable {

        @Test
        public void successfullyEnabled() {
            InspectitConfig configuration = new InspectitConfig();
            configuration.setConfig(new ConfigSettings());
            configuration.getConfig().setHttp(new HttpConfigSettings());
            configuration.getConfig().getHttp().setFrequency(Duration.ofMillis(5000L));
            ScheduledFuture future = Mockito.mock(ScheduledFuture.class);
            when(executor.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);

            boolean result = poller.doEnable(configuration);

            assertTrue(result);
            verify(executor).scheduleWithFixedDelay(poller, 5000L, 5000L, TimeUnit.MILLISECONDS);
            verifyNoMoreInteractions(executor);
        }

    }

    @Nested
    public class DoDisable {

        @Test
        public void notEnabled() {
            boolean result = poller.doDisable();

            assertTrue(result);
        }

        @Test
        public void isEnabled() {
            InspectitConfig configuration = new InspectitConfig();
            configuration.setConfig(new ConfigSettings());
            configuration.getConfig().setHttp(new HttpConfigSettings());
            configuration.getConfig().getHttp().setFrequency(Duration.ofMillis(5000L));
            ScheduledFuture future = Mockito.mock(ScheduledFuture.class);
            when(executor.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);

            poller.doEnable(configuration);

            boolean result = poller.doDisable();

            assertTrue(result);
            verify(future).cancel(true);
        }
    }

    @Nested
    public class Run {

        @Mock
        private HttpPropertySourceState currentState;

        @BeforeEach
        public void beforeEach() throws Exception {
            Field field = poller.getClass().getDeclaredField("currentState");
            field.setAccessible(true);
            field.set(poller, currentState);
        }

        @Test
        public void stateNotUpdated() {
            doReturn(false).when(currentState).update(anyBoolean());

            poller.run();

            verify(currentState).update(eq(false));
            verifyNoMoreInteractions(currentState);
            verifyZeroInteractions(env);
        }

        @Test
        public void stateUpdated() {
            doReturn(true).when(currentState).update(anyBoolean());

            poller.run();

            verify(currentState).update(eq(false));
            verify(env).updatePropertySources(any());
            verifyNoMoreInteractions(currentState, env);
        }
    }
}