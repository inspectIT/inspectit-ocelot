package rocks.inspectit.ocelot.core.command.handler.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.impl.InstrumentationFeedbackCommand;
import rocks.inspectit.ocelot.core.selfmonitoring.instrumentation.InstrumentationFeedbackService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InstrumentationFeedbackExecutorTest {

    @InjectMocks
    private InstrumentationFeedbackCommandExecutor executor;

    @Mock
    private InstrumentationFeedbackService service;

    @BeforeEach
    void setUp() {
        Map<String, InstrumentationFeedbackCommand.ClassInstrumentation> instrumentation = new HashMap<>();
        InstrumentationFeedbackCommand.ClassInstrumentation classInstrumentation = new InstrumentationFeedbackCommand.ClassInstrumentation();
        instrumentation.put(InstrumentationFeedbackExecutorTest.class.getName(), classInstrumentation);
        when(service.getInstrumentation()).thenReturn(instrumentation);
    }

    @Test
    void shouldExecuteCommand() {
        Command command = new InstrumentationFeedbackCommand();

        InstrumentationFeedbackCommand.Response response = (InstrumentationFeedbackCommand.Response) executor.execute(command);

        assertThat(response.getCommandId()).isEqualTo(command.getCommandId());
        assertThat(response.getInstrumentationFeedback()).hasSize(1);
    }
}
