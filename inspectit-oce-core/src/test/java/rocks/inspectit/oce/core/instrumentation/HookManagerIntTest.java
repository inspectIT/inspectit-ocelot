package rocks.inspectit.oce.core.instrumentation;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.bootstrap.instrumentation.IMethodHook;
import rocks.inspectit.oce.bootstrap.instrumentation.noop.NoopMethodHook;
import rocks.inspectit.oce.core.SpringTestBase;
import rocks.inspectit.oce.core.testutils.Dummy;
import rocks.inspectit.oce.core.testutils.DummyClassLoader;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
        "inspectit.instrumentation.internal.inter-batch-delay=1ms", //for faster responses of the test
        "inspectit.instrumentation.scopes.scA.type.name=rocks.inspectit.oce.core.testutils.Dummy",
        "inspectit.instrumentation.scopes.scA.methods[0].name=methodA",

        "inspectit.instrumentation.scopes.scB.type.name=rocks.inspectit.oce.core.testutils.Dummy",

        "inspectit.instrumentation.rules.r1.scopes.scA=true"

})
public class HookManagerIntTest extends SpringTestBase {


    @Autowired
    InstrumentationTriggerer triggerer;

    private Class<?> dummyClass;

    private Class<?> dummyClassWithoutBootstrapAccess;


    @BeforeEach
    void intitializeDummyClass() throws Exception {


        String className = Dummy.class.getName();
        // we need to load the target class from a different classloader because the "this" classloader is ignored
        // the dummyclassloader however needs access to the bootstrap classes
        dummyClass = Class.forName(className, true, new DummyClassLoader(getClass().getClassLoader(), Dummy.class));
        dummyClassWithoutBootstrapAccess = Class.forName(className, true, new DummyClassLoader(Dummy.class));

        when(mockInstrumentation.getAllLoadedClasses()).thenReturn(new Class<?>[]{dummyClass, dummyClassWithoutBootstrapAccess});
        when(mockInstrumentation.isModifiableClass(any())).thenReturn(true);
        triggerer.onNewClassesDiscovered(ImmutableSet.of(dummyClass, dummyClassWithoutBootstrapAccess));
    }

    void waitForHookingToFinish() {
        await().atMost(10, TimeUnit.SECONDS).until(() -> triggerer.pendingClasses.size() == 0);
    }


    @Test
    @DirtiesContext
    void testInstrumentationRemoval() {
        waitForHookingToFinish();

        IMethodHook hookA = Instances.hookManager.getHook(dummyClass, "methodA()");
        IMethodHook hookB = Instances.hookManager.getHook(dummyClass, "methodB()");
        assertThat(hookA).isNotSameAs(NoopMethodHook.INSTANCE);
        assertThat(hookB).isSameAs(NoopMethodHook.INSTANCE);

        updateProperties(ps ->
                ps.setProperty("inspectit.instrumentation.rules.r1.scopes.scA", "false")
        );

        waitForHookingToFinish();

        hookA = Instances.hookManager.getHook(dummyClass, "methodA()");
        hookB = Instances.hookManager.getHook(dummyClass, "methodB()");
        assertThat(hookA).isSameAs(NoopMethodHook.INSTANCE);
        assertThat(hookB).isSameAs(NoopMethodHook.INSTANCE);
    }


    @Test
    @DirtiesContext
    void testInstrumentationAdding() {
        waitForHookingToFinish();

        updateProperties(ps ->
                ps.setProperty("inspectit.instrumentation.rules.r2.scopes.scB", "true")
        );

        waitForHookingToFinish();

        IMethodHook hookB = Instances.hookManager.getHook(dummyClass, "methodB()");
        IMethodHook constructorHook = Instances.hookManager.getHook(dummyClass, "<init>()");
        assertThat(hookB).isNotSameAs(NoopMethodHook.INSTANCE);
        assertThat(constructorHook).isNotSameAs(NoopMethodHook.INSTANCE);
    }

    @Test
    @DirtiesContext
    void testIgnoresRespected() {
        waitForHookingToFinish();

        updateProperties(ps -> {
            ps.setProperty("inspectit.instrumentation.rules.r2.scopes.scB", "true");
            ps.setProperty("inspectit.instrumentation.ignored-packages.rocks", "true");
        });

        waitForHookingToFinish();

        IMethodHook hookB = Instances.hookManager.getHook(dummyClass, "methodB()");
        IMethodHook constructorHook = Instances.hookManager.getHook(dummyClass, "<init>()");
        assertThat(hookB).isSameAs(NoopMethodHook.INSTANCE);
        assertThat(constructorHook).isSameAs(NoopMethodHook.INSTANCE);
    }


    @Test
    @DirtiesContext
    void testNotAffectedHooksNotRebuild() {
        waitForHookingToFinish();

        IMethodHook hookB = Instances.hookManager.getHook(dummyClass, "methodB()");

        updateProperties(ps -> {
            ps.setProperty("inspectit.instrumentation.scopes.scC.type.name", "blub");
            ps.setProperty("inspectit.instrumentation.rules.r2.scopes.scC", "true");
        });

        waitForHookingToFinish();

        IMethodHook newHookB = Instances.hookManager.getHook(dummyClass, "methodB()");
        assertThat(newHookB).isSameAs(hookB);
    }

}
