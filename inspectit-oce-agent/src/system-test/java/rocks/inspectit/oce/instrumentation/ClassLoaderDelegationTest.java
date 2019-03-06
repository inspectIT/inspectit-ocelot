package rocks.inspectit.oce.instrumentation;

import io.opencensus.common.Scope;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.utils.DummyClassLoader;
import rocks.inspectit.oce.utils.TestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassLoaderDelegationTest {


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

    @Test
    void verifyInspectitBootstrapClassesAccessible() throws Exception {
        BadClassLoader bcl = new BadClassLoader();
        bcl.loadCopiesOfClasses(MyExecutor.class);
        Class<?> execClass = Class.forName(MyExecutor.class.getName(), true, bcl);
        Constructor<?> constr = execClass.getDeclaredConstructor();
        constr.setAccessible(true);
        Executor exec = (Executor) constr.newInstance();

        TestUtils.waitForInstrumentationToComplete();

        try (Scope tcb = Tags.getTagger().emptyBuilder()
                .put(TagKey.create("test_key"), TagValue.create("test_value"))
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


