package rocks.inspectit.ocelot.core.command.handler.impl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.command.impl.ListClassesCommand;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.core.instrumentation.NewClassDiscoveryService;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListClassesCommandExecutorTest {

    @InjectMocks
    private ListClassesCommandExecutor executor;

    @Mock
    private NewClassDiscoveryService discoveryService;

    @Nested
    public class Execute {

        @Test
        public void success(){
            Set<Class<?>> set = new HashSet<>();
            set.add(String.class);
            when(discoveryService.getKnownClasses()).thenReturn(set);

            ListClassesCommand command = new ListClassesCommand();
            command.setFilter("com");

            CommandResponse response = executor.execute(command);

            System.out.println(response);
        }

    }
}