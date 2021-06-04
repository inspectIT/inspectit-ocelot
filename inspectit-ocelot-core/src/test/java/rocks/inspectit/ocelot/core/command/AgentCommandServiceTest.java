package rocks.inspectit.ocelot.core.command;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.config.ConfigSettings;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentCommandServiceTest {

    @InjectMocks
    private AgentCommandService poller;

    @Mock
    private ScheduledExecutorService executor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitConfig configuration;

    @Nested
    public class DoEnable {

        @Test
        public void successfullyEnabled() {
            when(configuration.getAgentCommands().getPollingInterval()).thenReturn(Duration.ofSeconds(1));
            ScheduledFuture futureMock = mock(ScheduledFuture.class);
            when(executor.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(futureMock);

            boolean result = poller.doEnable(configuration);

            assertTrue(result);
            verify(executor).scheduleWithFixedDelay(poller, 1000, 1000, TimeUnit.MILLISECONDS);
            verify(configuration).getAgentCommands();
            verifyNoMoreInteractions(executor, configuration);
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

}
