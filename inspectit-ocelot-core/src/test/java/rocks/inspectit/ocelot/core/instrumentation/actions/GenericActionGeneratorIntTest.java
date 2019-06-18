package rocks.inspectit.ocelot.core.instrumentation.actions;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.instrumentation.actions.template.GenericActionTemplate;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;
import rocks.inspectit.ocelot.core.testutils.DummyClassLoader;
import rocks.inspectit.ocelot.core.testutils.GcUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class GenericActionGeneratorIntTest extends SpringTestBase {

    @Autowired
    GenericActionGenerator generator;

    private static DummyClassLoader dummyLoader;
    private static Class<?> dummyClass;

    @BeforeAll
    static void initDummyClassloader() throws Exception {
        dummyLoader = new DummyClassLoader(GenericActionGeneratorIntTest.class.getClassLoader(), MyDummyClass.class);
        dummyClass = Class.forName(MyDummyClass.class.getName(), false, dummyLoader);
    }

    IGenericAction getInstance(InjectedClass<? extends IGenericAction> clazz) {
        try {
            return (IGenericAction) clazz.getInjectedClassObject().get().getField("INSTANCE").get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DirtiesContext
    void testSyntaxErrorHandling() {
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .valueBody("i am not java code")
                .build();

        InjectedClass<?> action = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(action.getInjectedClassObject().get()).isSameAs(GenericActionTemplate.class);
    }

    @Test
    @DirtiesContext
    void testActionCaching() {
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .valueBody("return \"dummyresult\";")
                .build();

        InjectedClass<? extends IGenericAction> actionA = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(getInstance(actionA).execute(null, null, null, null, null)).isEqualTo("dummyresult");

        InjectedClass<? extends IGenericAction> actionB = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(actionB).isSameAs(actionA);
    }


    @Test
    @DirtiesContext
    void testActionReuse() throws Exception {
        GenericActionConfig configA = GenericActionConfig.builder()
                .name("my-action")
                .valueBody("return \"actionA\";")
                .build();

        InjectedClass<? extends IGenericAction> actionA = generator.getOrGenerateGenericAction(configA, dummyClass);
        assertThat(getInstance(actionA).execute(null, null, null, null, null)).isEqualTo("actionA");

        Class<?> actionAClass = actionA.getInjectedClassObject().get();
        WeakReference<Object> actionAWeak = new WeakReference<>(actionA);
        actionA = null;

        GcUtils.waitUntilCleared(actionAWeak);

        GenericActionConfig configB = GenericActionConfig.builder()
                .name("my-action")
                .valueBody("return \"actionB\";")
                .build();

        InjectedClass<? extends IGenericAction> actionB = generator.getOrGenerateGenericAction(configB, dummyClass);

        assertThat(actionB.getInjectedClassObject().get()).isSameAs(actionAClass);
        //we cannot check the return value because redefineClass is mocked
        verify(mockInstrumentation, atLeastOnce()).redefineClasses(any());
    }


    @Test
    @DirtiesContext
    void testMethodArgumentPassingAndCasting() {
        HashMap<Integer, String> argumentTypes = new HashMap<>();
        argumentTypes.put(0, "String");
        argumentTypes.put(2, "int");
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .expectedArgumentTypes(argumentTypes)
                .valueBody("return new Integer(_arg0.length() + _arg2);")
                .build();

        Object[] args = new Object[]{"1234", null, 6, null};
        InjectedClass<? extends IGenericAction> action = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(getInstance(action).execute(args, null, null, null, null)).isEqualTo(10);

    }


    @Test
    @DirtiesContext
    void testVoidActionReturnsNull() {
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .isVoid(true)
                .usesArgsArray(true)
                .valueBody("((java.util.concurrent.atomic.AtomicLong)_args[0]).incrementAndGet();")
                .build();

        AtomicLong myLong = new AtomicLong(42);
        Object[] args = new Object[]{myLong};
        InjectedClass<? extends IGenericAction> action = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(getInstance(action).execute(args, null, null, null, null)).isNull();
        assertThat(myLong.get()).isEqualTo(43);
    }

    @Test
    @DirtiesContext
    void testArgsArrayPassing() {
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .usesArgsArray(true)
                .valueBody("return new Integer(((String)_args[0]).length() + ((Integer)_args[2]).intValue());")
                .build();

        Object[] args = new Object[]{"1234", null, 6, null};
        InjectedClass<? extends IGenericAction> action = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(getInstance(action).execute(args, null, null, null, null)).isEqualTo(10);
    }


    @Test
    @DirtiesContext
    void testPrimitveArrayReturnValue() {
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .expectedReturnValueType("int[]")
                .valueBody("return new Integer(_returnValue[0] + _returnValue[1] +_returnValue[2]);")
                .build();

        InjectedClass<? extends IGenericAction> action = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(getInstance(action).execute(null, null, new int[]{1, 2, 3}, null, null)).isEqualTo(6);
    }


    @Test
    @DirtiesContext
    void testExceptionPassing() {
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .usesThrown(true)
                .valueBody("return _thrown.getMessage();")
                .build();

        InjectedClass<? extends IGenericAction> action = generator.getOrGenerateGenericAction(config, dummyClass);
        RuntimeException re = new RuntimeException("runtime");
        assertThat(getInstance(action).execute(null, null, null, re, null))
                .isEqualTo("runtime");
    }


    @Test
    @DirtiesContext
    void testTargetClassloaderAccessPassing() throws Exception {
        dummyClass.getField("MY_VALUE").set(null, 42);
        assertThat(MyDummyClass.MY_VALUE).isNotEqualTo(42);

        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .importedPackages(Arrays.asList(MyDummyClass.class.getPackage().getName()))
                .valueBody("return new Integer(MyDummyClass.MY_VALUE);")
                .build();

        InjectedClass<? extends IGenericAction> action = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(getInstance(action).execute(null, null, null, null, null))
                .isEqualTo(42);
    }


    @Test
    @DirtiesContext
    void testCustomArgumentPassingAndCasting() {
        List<Pair<String, String>> argumentTypes = new ArrayList<>();
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                //Alphabetical order, so that this order corresponds to the index
                .additionalArgumentType("myvalue", "int")
                .additionalArgumentType("x", "long")
                .valueBody("return new Long(x * myvalue);")
                .build();

        Object[] args = new Object[]{12, 3L};
        InjectedClass<? extends IGenericAction> action = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(getInstance(action).execute(null, null, null, null, args)).isEqualTo(36L);
    }

    @Test
    @DirtiesContext
    void testThizPassingAndCasting() {
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .expectedThisType("String")
                .valueBody("return _this + \"!\";")
                .build();

        InjectedClass<? extends IGenericAction> action = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(getInstance(action).execute(null, "hello world", null, null, null))
                .isEqualTo("hello world!");
    }


    @Test
    @DirtiesContext
    void testReturnValuePassingAndCasting() {
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .expectedReturnValueType("String")
                .valueBody("return _returnValue + \"!\";")
                .build();

        InjectedClass<? extends IGenericAction> action = generator.getOrGenerateGenericAction(config, dummyClass);
        assertThat(getInstance(action).execute(null, null, "hello world", null, null))
                .isEqualTo("hello world!");
    }


    @Test
    @DirtiesContext
    void testCacheWorking() {
        GenericActionConfig config = GenericActionConfig.builder()
                .name("my-action")
                .additionalArgumentType("val", "Object")
                .expectedReturnValueType("Object")
                .valueBody("return _cache.put(\"myKey\",val);")
                .build();

        InjectedClass<? extends IGenericAction> action = generator.getOrGenerateGenericAction(config, dummyClass);

        Object cachedValue = 42;

        assertThat(getInstance(action).execute(null, null, new int[]{1, 2, 3}, null, new Object[]{cachedValue})).isNull();
        assertThat(getInstance(action).execute(null, null, new int[]{1, 2, 3}, null, new Object[]{"something"})).isSameAs(cachedValue);
    }

}

