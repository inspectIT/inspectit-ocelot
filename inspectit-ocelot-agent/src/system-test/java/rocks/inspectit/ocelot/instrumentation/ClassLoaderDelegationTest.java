package rocks.inspectit.ocelot.instrumentation;

import io.opencensus.common.Scope;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.DummyClassLoader;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassLoaderDelegationTest extends InstrumentationSysTestBase {

    static class BadClassLoader extends DummyClassLoader {

        private Set<String> addedClasses = new HashSet<>();

        @Override
        public void defineNewClass(String name, byte[] code) {
            addedClasses.add(name);
            super.defineNewClass(name, code);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (addedClasses.contains(name) || name.startsWith("java.")) {
                return super.loadClass(name);
            }
            throw new ClassNotFoundException("haha!");
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (addedClasses.contains(name) || name.startsWith("java.")) {
                return super.loadClass(name, resolve);
            }
            throw new ClassNotFoundException("haha!");
        }
    }

    /**
     * We duplicated {@link BadClassLoader} to make sure that InherentlyBadClassLoader2 is not directly instrument
     * during the BadClassLoader test.
     */
    static class BadClassLoader2 extends DummyClassLoader {

        private Set<String> addedClasses = new HashSet<>();

        @Override
        public void defineNewClass(String name, byte[] code) {
            addedClasses.add(name);
            super.defineNewClass(name, code);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (addedClasses.contains(name) || name.startsWith("java.")) {
                return super.loadClass(name);
            }
            throw new ClassNotFoundException("haha!");
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (addedClasses.contains(name) || name.startsWith("java.")) {
                return super.loadClass(name, resolve);
            }
            throw new ClassNotFoundException("haha!");
        }
    }

    static class InherentlyBadClassLoader extends BadClassLoader2 {
    }


    static class StackCheckingClassLoader extends DummyClassLoader {

        private static class SecurityManagerEx extends SecurityManager {
            @Override
            protected Class[] getClassContext() {
                return super.getClassContext();
            }
        }

        private static SecurityManagerEx sm = new SecurityManagerEx();


        private Set<String> addedClasses = new HashSet<>();

        @Override
        public void defineNewClass(String name, byte[] code) {
            addedClasses.add(name);
            super.defineNewClass(name, code);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            //we check if the callstack contains an oce.core class and in this case do not delegate the classloading
            //this is similar to OSGi implicit bootstrap delegation
            boolean calledFromOceCore = false;
            for (Class<?> clazz : sm.getClassContext()) {
                if (clazz.getName().contains("inspectit.ocelot.core")) {
                    calledFromOceCore = true;
                }
            }
            if (addedClasses.contains(name) || name.startsWith("java.") || calledFromOceCore) {
                return super.loadClass(name, resolve);
            }
            throw new ClassNotFoundException("haha!");
        }
    }


    @Test
    void verifyCLDForOverriddenLoadClass() throws Exception {
        BadClassLoader bcl = new BadClassLoader();
        bcl.loadCopiesOfClasses(MyExecutor.class);
        Class<?> execClass = Class.forName(MyExecutor.class.getName(), true, bcl);
        Constructor<?> constr = execClass.getDeclaredConstructor();
        constr.setAccessible(true);
        Executor exec = (Executor) constr.newInstance();

        TestUtils.waitForClassInstrumentation(execClass, 15, TimeUnit.SECONDS);

        try (Scope tcb = Tags.getTagger().emptyBuilder()
                .putLocal(TagKey.create("test_key"), TagValue.create("test_value"))
                .buildScoped()) {
            exec.execute(() -> {
                Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
                assertThat(tags).containsEntry("test_key", "test_value");
            });
        }
        Method runCommand = exec.getClass().getDeclaredMethod("runCommand");
        runCommand.setAccessible(true);
        runCommand.invoke(exec);
    }


    @Test
    void verifyCLDForInheritedOverriddenLoadClass() throws Exception {
        InherentlyBadClassLoader bcl = new InherentlyBadClassLoader();
        bcl.loadCopiesOfClasses(MyExecutor.class);
        Class<?> execClass = Class.forName(MyExecutor.class.getName(), true, bcl);
        Constructor<?> constr = execClass.getDeclaredConstructor();
        constr.setAccessible(true);
        Executor exec = (Executor) constr.newInstance();

        TestUtils.waitForClassInstrumentation(execClass, 15, TimeUnit.SECONDS);

        try (Scope tcb = Tags.getTagger().emptyBuilder()
                .putLocal(TagKey.create("test_key"), TagValue.create("test_value"))
                .buildScoped()) {
            exec.execute(() -> {
                Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
                assertThat(tags).containsEntry("test_key", "test_value");
            });
        }
        Method runCommand = exec.getClass().getDeclaredMethod("runCommand");
        runCommand.setAccessible(true);
        runCommand.invoke(exec);
    }


    @Test
    void verifyCLDForImplicitBootstrapDelegation() throws Exception {
        StackCheckingClassLoader bcl = new StackCheckingClassLoader();
        bcl.loadCopiesOfClasses(MyExecutor.class);
        Class<?> execClass = Class.forName(MyExecutor.class.getName(), true, bcl);
        Constructor<?> constr = execClass.getDeclaredConstructor();
        constr.setAccessible(true);
        Executor exec = (Executor) constr.newInstance();

        TestUtils.waitForClassInstrumentation(execClass, 15, TimeUnit.SECONDS);

        try (Scope tcb = Tags.getTagger().emptyBuilder()
                .putLocal(TagKey.create("test_key"), TagValue.create("test_value"))
                .buildScoped()) {
            exec.execute(() -> {
                Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
                assertThat(tags).containsEntry("test_key", "test_value");
            });
        }
        Method runCommand = exec.getClass().getDeclaredMethod("runCommand");
        runCommand.setAccessible(true);
        runCommand.invoke(exec);
    }
}


class MyExecutor implements Executor {

    private Runnable command;

    @Override
    public void execute(Runnable command) {
        this.command = command;
    }

    public void runCommand() {
        command.run();
    }
}


