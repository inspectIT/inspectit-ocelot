package rocks.inspectit.ocelot.core.instrumentation.hook;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IMethodHook;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.NoopMethodHook;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.selfmonitoring.ActionScopeFactory;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class HookManagerTest {

    @InjectMocks
    private HookManager manager;

    @Mock
    private InstrumentationConfigurationResolver configResolver;

    @Mock
    private SelfMonitoringService selfMonitoring;

    @Mock
    private MethodHookGenerator hookGenerator;

    @Nested
    public class GetHook {

        @Test
        public void hookNotExisting() {
            IMethodHook result = manager.getHook(HookManagerTest.class, "stark");

            assertThat(result).isSameAs(NoopMethodHook.INSTANCE);
        }

        @Test
        public void hookExisting() {
            // create map with mock hook
            MethodHook hook = MethodHook.builder().actionScopeFactory(mock(ActionScopeFactory.class)).build();
            Map<String, MethodHook> methodHookMap = new HashMap<>();
            methodHookMap.put("lannister", hook);
            Map<Class<?>, Map<String, MethodHook>> hooks = new HashMap<>();
            hooks.put(HookManagerTest.class, methodHookMap);

            // inject map
            ReflectionTestUtils.setField(manager, "hooks", hooks);

            IMethodHook result = manager.getHook(HookManagerTest.class, "lannister");

            assertThat(result).isSameAs(hook);
        }

        @Test
        public void preventRecursion() {
            // create map with mock hook
            MethodHook hook = MethodHook.builder().actionScopeFactory(mock(ActionScopeFactory.class)).build();
            Map<String, MethodHook> methodHookMap = new HashMap<>();
            methodHookMap.put("lannister", hook);
            Map<Class<?>, Map<String, MethodHook>> hooks = new HashMap<>();
            hooks.put(HookManagerTest.class, methodHookMap);

            // inject map
            ReflectionTestUtils.setField(manager, "hooks", hooks);

            IMethodHook resultFirst = manager.getHook(HookManagerTest.class, "lannister");
            assertThat(resultFirst).isSameAs(hook);

            // set recursion gate
            HookManager.RECURSION_GATE.set(true);

            IMethodHook resultSecond = manager.getHook(HookManagerTest.class, "lannister");
            assertThat(resultSecond).isSameAs(NoopMethodHook.INSTANCE);
        }
    }
}
