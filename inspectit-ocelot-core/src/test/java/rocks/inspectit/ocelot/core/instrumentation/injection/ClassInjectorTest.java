package rocks.inspectit.ocelot.core.instrumentation.injection;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.Modifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.testutils.DummyClassLoader;
import rocks.inspectit.ocelot.core.testutils.GcUtils;

import java.lang.instrument.Instrumentation;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClassInjectorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitEnvironment env;

    @Mock
    Instrumentation instrumentation;

    @Mock
    JigsawModuleInstrumenter moduleManager;

    @InjectMocks
    ClassInjector injector;

    byte[] getByteCodeAndRename(Class<?> clazz, String name) {
        try {
            ClassPool cp = new ClassPool();
            cp.insertClassPath(new ClassClassPath(clazz));

            CtClass ctClazz = null;
            ctClazz = cp.get(clazz.getName());
            ctClazz.setName(name);
            ctClazz.setModifiers(Modifier.setPublic(ctClazz.getModifiers()));

            return ctClazz.toBytecode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class Inject {

        @Test
        public void testBootstrapInjection() throws Exception {
            InjectedClass<?> clazz42 = injector.inject("id", java.lang.String.class, (name) -> getByteCodeAndRename(ClassToInject42.class, name));
            InjectedClass<?> clazz7 = injector.inject("id", java.lang.String.class, (name) -> getByteCodeAndRename(ClassToInject7.class, name));

            assertThat(clazz42.getInjectedClassObject().get().getMethod("getValue").invoke(null)).isEqualTo(42);
            assertThat(clazz7.getInjectedClassObject().get().getMethod("getValue").invoke(null)).isEqualTo(7);
        }

        @Test
        public void testBootstrapReuseForSameIdentifier() throws Exception {
            when(env.getCurrentConfig().getInstrumentation().getInternal().isRecyclingOldActionClasses()).thenReturn(true);

            AtomicReference<byte[]> bytecodeFor7 = new AtomicReference<>();
            InjectedClass<?> clazz42 = injector.inject("id", java.lang.String.class, (name) -> getByteCodeAndRename(ClassToInject42.class, name));
            assertThat(clazz42.getInjectedClassObject().get().getMethod("getValue").invoke(null)).isEqualTo(42);

            Class<?> injectedClass = clazz42.getInjectedClassObject().get();
            WeakReference<InjectedClass<?>> weakClazz42 = new WeakReference<>(clazz42);
            clazz42 = null;
            GcUtils.waitUntilCleared(weakClazz42);

            InjectedClass<?> clazz7 = injector.inject("id", java.lang.String.class, (name) -> {
                bytecodeFor7.set(getByteCodeAndRename(ClassToInject7.class, name));
                return bytecodeFor7.get();
            });

            assertThat(clazz7.getInjectedClassObject().get()).isSameAs(injectedClass);
            verify(instrumentation, times(1)).redefineClasses(any());
        }

        @Test
        public void testRecyclingDisabled() throws Exception {
            when(env.getCurrentConfig().getInstrumentation().getInternal().isRecyclingOldActionClasses()).thenReturn(false);

            AtomicReference<byte[]> bytecodeFor7 = new AtomicReference<>();
            InjectedClass<?> clazz42 = injector.inject("id", java.lang.String.class, (name) -> getByteCodeAndRename(ClassToInject42.class, name));
            assertThat(clazz42.getInjectedClassObject().get().getMethod("getValue").invoke(null)).isEqualTo(42);

            Class<?> injectedClass = clazz42.getInjectedClassObject().get();
            WeakReference<InjectedClass<?>> weakClazz42 = new WeakReference<>(clazz42);
            clazz42 = null;
            GcUtils.waitUntilCleared(weakClazz42);

            InjectedClass<?> clazz7 = injector.inject("id", java.lang.String.class, (name) -> {
                bytecodeFor7.set(getByteCodeAndRename(ClassToInject7.class, name));
                return bytecodeFor7.get();
            });

            assertThat(clazz7.getInjectedClassObject().get()).isNotSameAs(injectedClass);
            verify(instrumentation, times(0)).redefineClasses(any());
        }

        @Test
        public void testNormalClassloaderInjection() throws Exception {
            DummyClassLoader dummy = new DummyClassLoader(ClassToInject7.class);
            Class<?> neighbor = Class.forName(ClassToInject7.class.getName(), false, dummy);

            InjectedClass<?> clazz42 = injector.inject("id", neighbor, (name) -> getByteCodeAndRename(ClassToInject42.class, name));
            InjectedClass<?> clazz7 = injector.inject("id", neighbor, (name) -> getByteCodeAndRename(ClassToInject7.class, name));

            verify(moduleManager, times(2)).openModule(same(neighbor));
            assertThat(clazz42.getInjectedClassObject().get().getMethod("getValue").invoke(null)).isEqualTo(42);
            assertThat(clazz42.getInjectedClassObject().get().getClassLoader()).isSameAs(dummy);
            assertThat(clazz7.getInjectedClassObject().get().getMethod("getValue").invoke(null)).isEqualTo(7);
            assertThat(clazz7.getInjectedClassObject().get().getClassLoader()).isSameAs(dummy);
        }

        @Test
        public void testDefaultPackageInjection() throws Exception {
            DummyClassLoader dummy = new DummyClassLoader();
            dummy.defineNewClass("Test", getByteCodeAndRename(ClassToInject7.class, "Test"));
            Class<?> neighbor = Class.forName("Test", false, dummy);

            assertThatThrownBy(() -> injector.inject("id", neighbor, (name) -> getByteCodeAndRename(ClassToInject42.class, name)))
                    .hasMessageContaining("default package");
        }

        @Test
        public void testReuseWithExceptionPassThrough() throws Exception {
            when(env.getCurrentConfig().getInstrumentation().getInternal().isRecyclingOldActionClasses()).thenReturn(true);

            DummyClassLoader dummy = new DummyClassLoader(ClassToInject7.class);
            Class<?> neighbor = Class.forName(ClassToInject7.class.getName(), false, dummy);

            InjectedClass<?> clazz42 = injector.inject("id", neighbor, (name) -> getByteCodeAndRename(ClassToInject42.class, name));
            assertThat(clazz42.getInjectedClassObject().get().getMethod("getValue").invoke(null)).isEqualTo(42);
            assertThat(clazz42.getInjectedClassObject().get().getClassLoader()).isSameAs(dummy);

            Class<?> injectedClass = clazz42.getInjectedClassObject().get();
            WeakReference<InjectedClass<?>> weakClazz42 = new WeakReference<>(clazz42);
            clazz42 = null;
            GcUtils.waitUntilCleared(weakClazz42);

            RuntimeException exception = new RuntimeException("blub");
            assertThatThrownBy(() -> injector.inject("id", neighbor, (name) -> {
                throw exception;
            })).isSameAs(exception);

            AtomicReference<byte[]> bytecodeFor7 = new AtomicReference<>();
            InjectedClass<?> clazz7 = injector.inject("id", neighbor, (name) -> getByteCodeAndRename(ClassToInject7.class, name));

            assertThat(clazz7.getInjectedClassObject().get()).isSameAs(injectedClass);
            verify(instrumentation, times(1)).redefineClasses(any());
        }

        @Test
        public void testNormalClassloaderReuseForSameIdentifier() throws Exception {
            when(env.getCurrentConfig().getInstrumentation().getInternal().isRecyclingOldActionClasses()).thenReturn(true);

            DummyClassLoader dummy = new DummyClassLoader(ClassToInject7.class);
            Class<?> neighbor = Class.forName(ClassToInject7.class.getName(), false, dummy);

            InjectedClass<?> clazz42 = injector.inject("id", neighbor, (name) -> getByteCodeAndRename(ClassToInject42.class, name));
            assertThat(clazz42.getInjectedClassObject().get().getMethod("getValue").invoke(null)).isEqualTo(42);
            assertThat(clazz42.getInjectedClassObject().get().getClassLoader()).isSameAs(dummy);

            Class<?> injectedClass = clazz42.getInjectedClassObject().get();
            WeakReference<InjectedClass<?>> weakClazz42 = new WeakReference<>(clazz42);
            clazz42 = null;
            GcUtils.waitUntilCleared(weakClazz42);

            AtomicReference<byte[]> bytecodeFor7 = new AtomicReference<>();
            InjectedClass<?> clazz7 = injector.inject("id", neighbor, (name) -> {
                bytecodeFor7.set(getByteCodeAndRename(ClassToInject7.class, name));
                return bytecodeFor7.get();
            });

            assertThat(clazz7.getInjectedClassObject().get()).isSameAs(injectedClass);
            verify(instrumentation, times(1)).redefineClasses(any());
        }

        @Test
        public void testNoReuseForDifferentIdentifier() throws Exception {
            DummyClassLoader dummy = new DummyClassLoader(ClassToInject7.class);
            Class<?> neighbor = Class.forName(ClassToInject7.class.getName(), false, dummy);

            AtomicReference<byte[]> bytecodeFor7 = new AtomicReference<>();
            InjectedClass<?> clazz42 = injector.inject("id", neighbor, (name) -> getByteCodeAndRename(ClassToInject42.class, name));
            assertThat(clazz42.getInjectedClassObject().get().getMethod("getValue").invoke(null)).isEqualTo(42);
            assertThat(clazz42.getInjectedClassObject().get().getClassLoader()).isSameAs(dummy);

            Class<?> injectedClass = clazz42.getInjectedClassObject().get();
            WeakReference<InjectedClass<?>> weakClazz42 = new WeakReference<>(clazz42);
            clazz42 = null;
            GcUtils.waitUntilCleared(weakClazz42);

            InjectedClass<?> clazz7 = injector.inject("other", neighbor, (name) -> {
                bytecodeFor7.set(getByteCodeAndRename(ClassToInject7.class, name));
                return bytecodeFor7.get();
            });

            assertThat(clazz7.getInjectedClassObject().get()).isNotSameAs(injectedClass);
            assertThat(clazz7.getInjectedClassObject().get().getMethod("getValue").invoke(null)).isEqualTo(7);
            assertThat(clazz7.getInjectedClassObject().get().getClassLoader()).isSameAs(dummy);
        }

    }
}

class ClassToInject42 {

    public static int getValue() {
        return 42;
    }
}

class ClassToInject7 {

    public static int getValue() {
        return 7;
    }
}