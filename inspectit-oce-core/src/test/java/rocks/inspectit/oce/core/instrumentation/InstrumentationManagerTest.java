package rocks.inspectit.oce.core.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rocks.inspectit.oce.core.TestUtils;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.selfmonitoring.SelfMonitoringService;

import java.lang.instrument.Instrumentation;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InstrumentationManagerTest {

    @Mock
    InspectitEnvironment env;

    @Mock
    Instrumentation instrumentation;

    @Mock
    List<SpecialSensor> specialSensors;

    @Mock
    SelfMonitoringService selfMonitoring;

    @InjectMocks
    InstrumentationManager manager;

    @Nested
    public class ClassExcludeMatcher {

        @Test
        public void excludeInstpectITClassloader() {
            MockitoAnnotations.initMocks(InstrumentationManagerTest.this);

            AgentBuilder.RawMatcher classExcludeMatcher = TestUtils.resolve(manager, "classExcludeMatcher");
            ClassLoader inspectITClassLoader = InstrumentationManager.class.getClassLoader();

            boolean result = classExcludeMatcher.matches(null, inspectITClassLoader, null, null, null);

            assertThat(result).isTrue();
        }

        @Test
        public void incldueAnyClassloader() {
            MockitoAnnotations.initMocks(InstrumentationManagerTest.this);

            AgentBuilder.RawMatcher classExcludeMatcher = TestUtils.resolve(manager, "classExcludeMatcher");
            ClassLoader classLoader = mock(ClassLoader.class);

            boolean result = classExcludeMatcher.matches(null, classLoader, null, null, null);

            assertThat(result).isFalse();
        }

        @Test
        public void excludeDefinedClasses() {
            MockitoAnnotations.initMocks(InstrumentationManagerTest.this);

            String[] input = new String[]{
                    "io.opencensus.nested.TestClass",
                    "rocks.inspectit.nested.TestClass",
                    "io.grpc.nested.TestClass",
                    "com.lmax.disruptor.nested.TestClass",
                    "com.google.nested.TestClass"
            };

            AgentBuilder.RawMatcher classExcludeMatcher = TestUtils.resolve(manager, "classExcludeMatcher");

            for (int i = 0; input.length < 5; i++) {
                TypeDescription typeDescription = mock(TypeDescription.class);
                when(typeDescription.getName()).thenReturn(input[i]);

                boolean result = classExcludeMatcher.matches(typeDescription, null, null, null, null);

                assertThat(result).isTrue();
            }
        }
    }
}
