package rocks.inspectit.oce.core.instrumentation.dataprovider;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.oce.bootstrap.instrumentation.IGenericDataProvider;
import rocks.inspectit.oce.core.SpringTestBase;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedGenericDataProviderConfig;
import rocks.inspectit.oce.core.instrumentation.dataprovider.generic.DataProviderGenerator;
import rocks.inspectit.oce.core.instrumentation.dataprovider.generic.GenericDataProviderTemplate;
import rocks.inspectit.oce.core.instrumentation.injection.InjectedClass;
import rocks.inspectit.oce.core.testutils.DummyClassLoader;
import rocks.inspectit.oce.core.testutils.GcUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class DataProviderGeneratorIntTest extends SpringTestBase {

    @Autowired
    DataProviderGenerator generator;

    private static DummyClassLoader dummyLoader;
    private static Class<?> dummyClass;

    @BeforeAll
    static void initDummyClassloader() throws Exception {
        dummyLoader = new DummyClassLoader(DataProviderGeneratorIntTest.class.getClassLoader(), MyDummyClass.class);
        dummyClass = Class.forName(MyDummyClass.class.getName(), false, dummyLoader);
    }

    IGenericDataProvider getInstance(InjectedClass<? extends IGenericDataProvider> clazz) {
        try {
            return (IGenericDataProvider) clazz.getInjectedClassObject().get().getField("INSTANCE").get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DirtiesContext
    void testSyntaxErrorHandling() {
        ResolvedGenericDataProviderConfig config = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .valueBody("i am not java code")
                .build();

        InjectedClass<?> provider = generator.getOrGenerateDataProvider(config, dummyClass);
        assertThat(provider.getInjectedClassObject().get()).isSameAs(GenericDataProviderTemplate.class);
    }

    @Test
    @DirtiesContext
    void testProviderCaching() {
        ResolvedGenericDataProviderConfig config = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .valueBody("return \"dummyresult\";")
                .build();

        InjectedClass<? extends IGenericDataProvider> providerA = generator.getOrGenerateDataProvider(config, dummyClass);
        assertThat(getInstance(providerA).execute(null, null, null, null, null)).isEqualTo("dummyresult");

        InjectedClass<? extends IGenericDataProvider> providerB = generator.getOrGenerateDataProvider(config, dummyClass);
        assertThat(providerB).isSameAs(providerA);
    }


    @Test
    @DirtiesContext
    void testProviderReuse() throws Exception {
        ResolvedGenericDataProviderConfig configA = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .valueBody("return \"providerA\";")
                .build();

        InjectedClass<? extends IGenericDataProvider> providerA = generator.getOrGenerateDataProvider(configA, dummyClass);
        assertThat(getInstance(providerA).execute(null, null, null, null, null)).isEqualTo("providerA");

        Class<?> providerAClass = providerA.getInjectedClassObject().get();
        WeakReference<Object> providerAWeak = new WeakReference<>(providerA);
        providerA = null;

        GcUtils.waitUntilCleared(providerAWeak);

        ResolvedGenericDataProviderConfig configB = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .valueBody("return \"providerB\";")
                .build();

        InjectedClass<? extends IGenericDataProvider> providerB = generator.getOrGenerateDataProvider(configB, dummyClass);

        assertThat(providerB.getInjectedClassObject().get()).isSameAs(providerAClass);
        //we cannot check the return value because redefineClass is mocked
        verify(mockInstrumentation, atLeastOnce()).redefineClasses(any());
    }


    @Test
    @DirtiesContext
    void testMethodArgumentPassingAndCasting() {
        HashMap<Integer, String> argumentTypes = new HashMap<>();
        argumentTypes.put(0, "String");
        argumentTypes.put(2, "int");
        ResolvedGenericDataProviderConfig config = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .expectedArgumentTypes(argumentTypes)
                .valueBody("return new Integer(arg0.length() + arg2);")
                .build();

        Object[] args = new Object[]{"1234", null, 6, null};
        InjectedClass<? extends IGenericDataProvider> provider = generator.getOrGenerateDataProvider(config, dummyClass);
        assertThat(getInstance(provider).execute(args, null, null, null, null)).isEqualTo(10);

    }


    @Test
    @DirtiesContext
    void testArgsArrayPassing() {
        ResolvedGenericDataProviderConfig config = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .usesArgsArray(true)
                .valueBody("return new Integer(((String)args[0]).length() + ((Integer)args[2]).intValue());")
                .build();

        Object[] args = new Object[]{"1234", null, 6, null};
        InjectedClass<? extends IGenericDataProvider> provider = generator.getOrGenerateDataProvider(config, dummyClass);
        assertThat(getInstance(provider).execute(args, null, null, null, null)).isEqualTo(10);
    }


    @Test
    @DirtiesContext
    void testPrimitveArrayReturnValue() {
        ResolvedGenericDataProviderConfig config = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .expectedReturnValueType("int[]")
                .valueBody("return new Integer(returnValue[0] + returnValue[1] +returnValue[2]);")
                .build();

        InjectedClass<? extends IGenericDataProvider> provider = generator.getOrGenerateDataProvider(config, dummyClass);
        assertThat(getInstance(provider).execute(null, null, new int[]{1, 2, 3}, null, null)).isEqualTo(6);
    }


    @Test
    @DirtiesContext
    void testExceptionPassing() {
        ResolvedGenericDataProviderConfig config = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .usesThrown(true)
                .valueBody("return thrown.getMessage();")
                .build();

        InjectedClass<? extends IGenericDataProvider> provider = generator.getOrGenerateDataProvider(config, dummyClass);
        RuntimeException re = new RuntimeException("runtime");
        assertThat(getInstance(provider).execute(null, null, null, re, null))
                .isEqualTo("runtime");
    }


    @Test
    @DirtiesContext
    void testTargetClassloaderAccessPassing() throws Exception {
        dummyClass.getField("MY_VALUE").set(null, 42);
        assertThat(MyDummyClass.MY_VALUE).isNotEqualTo(42);

        ResolvedGenericDataProviderConfig config = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .importedPackages(Arrays.asList(MyDummyClass.class.getPackage().getName()))
                .valueBody("return new Integer(MyDummyClass.MY_VALUE);")
                .build();

        InjectedClass<? extends IGenericDataProvider> provider = generator.getOrGenerateDataProvider(config, dummyClass);
        assertThat(getInstance(provider).execute(null, null, null, null, null))
                .isEqualTo(42);
    }


    @Test
    @DirtiesContext
    void testCustomArgumentPassingAndCasting() {
        List<Pair<String, String>> argumentTypes = new ArrayList<>();
        argumentTypes.add(Pair.of("x", "long"));
        argumentTypes.add(Pair.of("myvalue", "int"));
        ResolvedGenericDataProviderConfig config = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .additionalArgumentTypes(argumentTypes)
                .valueBody("return new Long(x * myvalue);")
                .build();

        Object[] args = new Object[]{3L, 12};
        InjectedClass<? extends IGenericDataProvider> provider = generator.getOrGenerateDataProvider(config, dummyClass);
        assertThat(getInstance(provider).execute(null, null, null, null, args)).isEqualTo(36L);
    }

    @Test
    @DirtiesContext
    void testThizPassingAndCasting() {
        ResolvedGenericDataProviderConfig config = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .expectedThisType("String")
                .valueBody("return thiz + \"!\";")
                .build();

        InjectedClass<? extends IGenericDataProvider> provider = generator.getOrGenerateDataProvider(config, dummyClass);
        assertThat(getInstance(provider).execute(null, "hello world", null, null, null))
                .isEqualTo("hello world!");
    }


    @Test
    @DirtiesContext
    void testReturnValuePassingAndCasting() {
        ResolvedGenericDataProviderConfig config = ResolvedGenericDataProviderConfig.builder()
                .name("my-provider")
                .expectedReturnValueType("String")
                .valueBody("return returnValue + \"!\";")
                .build();

        InjectedClass<? extends IGenericDataProvider> provider = generator.getOrGenerateDataProvider(config, dummyClass);
        assertThat(getInstance(provider).execute(null, null, "hello world", null, null))
                .isEqualTo("hello world!");
    }
}

