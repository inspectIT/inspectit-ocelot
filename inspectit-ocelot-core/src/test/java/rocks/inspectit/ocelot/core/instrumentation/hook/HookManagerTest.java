package rocks.inspectit.ocelot.core.instrumentation.hook;

import net.bytebuddy.description.method.MethodDescription;
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
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.selfmonitoring.ActionScopeFactory;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.utils.CoreUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Nested
    public class LazyHooking {

        final Method testCase_methodA = LazyHooking.class.getDeclaredMethod("methodA");

        private MethodHook dummyHook;

        private String methodASignature;

        private void initLazyHooking() {
            MethodDescription description = new MethodDescription.ForLoadedMethod(testCase_methodA);
            MethodHookConfiguration hookConfiguration = MethodHookConfiguration.builder().build();
            HashMap<MethodDescription, MethodHookConfiguration> hookConfigs = new HashMap<>();
            hookConfigs.put(description, hookConfiguration);
            methodASignature = CoreUtils.getSignature(description);
            dummyHook = MethodHook.builder().actionScopeFactory(mock(ActionScopeFactory.class)).build();

            when(configResolver.getHookConfigurations(any(Class.class))).thenReturn(hookConfigs);
            when(hookGenerator.buildHook(any(), any(), any())).thenReturn(dummyHook);

            ReflectionTestUtils.setField(manager, "isLazyHookingEnabled", true);
        }

        public void methodA() {

        }

        LazyHooking() throws NoSuchMethodException {
        }

        @Test
        void testHooksLoadLazy() {

            initLazyHooking();

            IMethodHook lazyHook = manager.getHook(HookManagerTest.class, methodASignature);

            Map<Class<?>, Map<String, MethodHook>> lazyLoadedHooks = (Map<Class<?>, Map<String, MethodHook>>) ReflectionTestUtils.getField(manager, "lazyLoadedHooks");
            Set<Class<?>> lazyHookingPerformed = (Set<Class<?>>) ReflectionTestUtils.getField(manager, "lazyHookingPerformed");

            assertThat(lazyHookingPerformed).contains(HookManagerTest.class);
            assertThat(lazyLoadedHooks).containsKey(HookManagerTest.class);
            assertThat(lazyHook).isEqualTo(dummyHook);
        }

        @Test
        void testLazyHooksMergedIntoHooksMap() {

            initLazyHooking();

            IMethodHook lazyHook = manager.getHook(HookManagerTest.class, methodASignature);

            HookManager.HookUpdate hookUpdate = manager.startUpdate();
            hookUpdate.updateHooksForClass(HookManagerTest.class);
            hookUpdate.commitUpdate();

            IMethodHook regularHook = manager.getHook(HookManagerTest.class, methodASignature);

            Map<Class<?>, Map<String, MethodHook>> hooks = (Map<Class<?>, Map<String, MethodHook>>) ReflectionTestUtils.getField(manager, "hooks");
            Map<Class<?>, Map<String, MethodHook>> lazyLoadedHooks = (Map<Class<?>, Map<String, MethodHook>>) ReflectionTestUtils.getField(manager, "lazyLoadedHooks");
            Set<Class<?>> lazyHookingPerformed = (Set<Class<?>>) ReflectionTestUtils.getField(manager, "lazyHookingPerformed");

            assertThat(lazyHookingPerformed).contains(HookManagerTest.class);
            assertThat(hooks).containsKey(HookManagerTest.class);
            assertThat(lazyLoadedHooks).isEmpty();
            assertThat(regularHook).isEqualTo(lazyHook);
        }
    }

}
