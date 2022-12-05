package rocks.inspectit.ocelot.core.instrumentation.transformer;

import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.TypeDescriptionWithClassLoader;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope;
import rocks.inspectit.ocelot.core.instrumentation.event.ClassInstrumentedEvent;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.testutils.Dummy;
import rocks.inspectit.ocelot.core.testutils.DummyClassLoader;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SyncClassTransformerTest {

    @Mock
    InspectitEnvironment env;

    @Mock
    ApplicationContext ctx;

    @Mock
    Instrumentation instrumentation;

    @Mock
    SelfMonitoringService selfMonitoring;

    @Mock
    InstrumentationConfigurationResolver configResolver;

    @InjectMocks
    SyncClassTransformer transformer = new SyncClassTransformer();

    private static byte[] bytecodeOfDummy;

    private static final String DUMMY_CLASS_NAME = Dummy.class.getName();

    @BeforeAll
    static void readByteCode() {
        bytecodeOfDummy = DummyClassLoader.readByteCode(DummyClassLoader.class);
    }

    @BeforeEach
    void setupTransformer() {
        transformer.classDefinitionListeners = new ArrayList<>();
    }

    @Nested
    public class Transform {

        @Test
        void verifyClassInstrumentedEventPublishedAfterSecondTransform() throws Exception {
            InstrumentationSettings settings = new InstrumentationSettings();
            InspectitConfig conf = new InspectitConfig();
            conf.setInstrumentation(settings);
            when(env.getCurrentConfig()).thenReturn(conf);

            SpecialSensor mockSensor = Mockito.mock(SpecialSensor.class);
            when(mockSensor.instrument(any(), any(), any())).then(invocation -> invocation.getArgument(2));
            InstrumentationScope scope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.any());
            InstrumentationRule rule = InstrumentationRule.builder().scope(scope).build();

            ClassInstrumentationConfiguration mockedConfig = new ClassInstrumentationConfiguration(Collections.singleton(mockSensor), Collections.singleton(rule), null);
            when(configResolver.getClassInstrumentationConfiguration(any(TypeDescriptionWithClassLoader.class))).thenReturn(mockedConfig);
            when(configResolver.getClassInstrumentationConfiguration(any(Class.class))).thenReturn(mockedConfig);

            String className = DUMMY_CLASS_NAME.replace('.', '/');

            transformer.transform(Thread.currentThread()
                    .getContextClassLoader(), className, null, null, bytecodeOfDummy);

            assertThat(transformer.temporaryInstrumentationConfigCache.size()).isEqualTo(1);
            verify(mockSensor).instrument(any(), any(), any());

            Mockito.reset(mockSensor);
            transformer.transform(SyncClassTransformer.class.getClassLoader(), className, Dummy.class, null, bytecodeOfDummy);
            assertThat(transformer.temporaryInstrumentationConfigCache.size()).isEqualTo(0);

            verify(mockSensor, never()).instrument(any(), any(), any());
            verify(ctx).publishEvent(isA(ClassInstrumentedEvent.class));

        }

    }

}
