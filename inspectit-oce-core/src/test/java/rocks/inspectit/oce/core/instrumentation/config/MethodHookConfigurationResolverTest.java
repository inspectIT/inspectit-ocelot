package rocks.inspectit.oce.core.instrumentation.config;

import com.google.common.collect.Sets;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.DataProviderCallSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedDataProviderCall;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedGenericDataProviderConfig;
import rocks.inspectit.oce.core.testutils.Dummy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class MethodHookConfigurationResolverTest {

    @InjectMocks
    MethodHookConfigurationResolver resolver;

    private final Class<?> DUMMY_CLASS = Dummy.class;
    private final TypeDescription DUMMY_TYPE = TypeDescription.ForLoadedType.of(Dummy.class);
    private final MethodDescription DUMMY_METHOD = DUMMY_TYPE.getDeclaredMethods().stream()
            .filter(m -> m.getName().equals("methodA")).findFirst().get();


    @Nested
    class BuildHookConfiguration {

        ResolvedGenericDataProviderConfig providerA;
        ResolvedDataProviderCall callToA1;
        ResolvedDataProviderCall callToA2;
        ResolvedGenericDataProviderConfig providerB;
        ResolvedDataProviderCall callToB;

        @BeforeEach
        void initTestData() {
            providerA = ResolvedGenericDataProviderConfig.builder()
                    .name("providerA")
                    .build();

            DataProviderCallSettings seta1 = new DataProviderCallSettings();
            seta1.setProvider("providerA");
            callToA1 = ResolvedDataProviderCall.builder()
                    .callSettings(seta1)
                    .provider(providerA).build();

            DataProviderCallSettings seta2 = new DataProviderCallSettings();
            seta2.setProvider("providerA");
            callToA2 = ResolvedDataProviderCall.builder()
                    .callSettings(seta2)
                    .provider(providerA).build();

            providerB = ResolvedGenericDataProviderConfig.builder()
                    .name("providerB")
                    .build();

            DataProviderCallSettings setb1 = new DataProviderCallSettings();
            setb1.setProvider("providerB");
            callToB = ResolvedDataProviderCall.builder()
                    .callSettings(setb1)
                    .provider(providerB).build();
        }

        @Test
        void verifyConflictsDetected() {
            InstrumentationRule r1 = InstrumentationRule.builder().entryProvider("my_key", callToA1).build();
            InstrumentationRule r2 = InstrumentationRule.builder().entryProvider("my_key", callToB).build();

            assertThatThrownBy(() -> resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD, Sets.newHashSet(r1, r2)))
                    .isInstanceOf(MethodHookConfigurationResolver.ConflictingDataDefinitionsException.class);
        }

        @Test
        void verifyNoConflictsForSameCall() throws Exception {
            InstrumentationRule r1 = InstrumentationRule.builder().entryProvider("my_key", callToA1).build();
            InstrumentationRule r2 = InstrumentationRule.builder().entryProvider("my_key", callToA2).build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD, Sets.newHashSet(r1, r2));
            assertThat(conf.getEntryProviders()).containsExactly(Pair.of("my_key", callToA1));
        }


        @Test
        void verifyProvidersOrderedByDependencies() throws Exception {
            DataProviderCallSettings dependingOnFirst = new DataProviderCallSettings();
            dependingOnFirst.setProvider("providerA");
            dependingOnFirst.setDataInput(Maps.newHashMap("someArgument", "first_key"));
            ResolvedDataProviderCall depFirst = ResolvedDataProviderCall.builder()
                    .callSettings(dependingOnFirst)
                    .provider(providerA).build();

            DataProviderCallSettings dependingOnSecond = new DataProviderCallSettings();
            dependingOnSecond.setProvider("providerA");
            dependingOnSecond.setDataInput(Maps.newHashMap("someArgument", "second_key"));
            ResolvedDataProviderCall depSecond = ResolvedDataProviderCall.builder()
                    .callSettings(dependingOnSecond)
                    .provider(providerA).build();

            InstrumentationRule r1 = InstrumentationRule.builder()
                    .entryProvider("first_key", callToA1)
                    .entryProvider("third_key", depSecond).build();
            InstrumentationRule r2 = InstrumentationRule.builder()
                    .entryProvider("second_key", depFirst).build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD, Sets.newHashSet(r1, r2));
            assertThat(conf.getEntryProviders()).containsExactly(
                    Pair.of("first_key", callToA1),
                    Pair.of("second_key", depFirst),
                    Pair.of("third_key", depSecond)
            );
        }


    }

    @Nested
    class GetInTopologicalOrder {

        @Test
        void testElementsPreserved() throws Exception {
            Map<String, Set<String>> dependencies = new HashMap<>();
            dependencies.put("A", Sets.newHashSet("B"));
            dependencies.put("B", Sets.newHashSet());
            dependencies.put("C", Sets.newHashSet());
            List<String> result = resolver.getInTopologicalOrder(dependencies.keySet(), dependencies::get);

            assertThat(result).containsExactlyInAnyOrder("A", "B", "C");
        }

        @Test
        void testTransitiveOrdering() throws Exception {
            Map<String, Set<String>> dependencies = new HashMap<>();
            dependencies.put("A", Sets.newHashSet("B"));
            dependencies.put("B", Sets.newHashSet("C"));
            dependencies.put("C", Sets.newHashSet("D"));
            dependencies.put("D", Sets.newHashSet());
            List<String> result = resolver.getInTopologicalOrder(dependencies.keySet(), dependencies::get);

            assertThat(result).containsExactly("D", "C", "B", "A");
        }

        @Test
        void testDAGOrdering() throws Exception {
            Map<String, Set<String>> dependencies = new HashMap<>();
            dependencies.put("A", Sets.newHashSet("B", "C"));
            dependencies.put("B", Sets.newHashSet("E"));
            dependencies.put("C", Sets.newHashSet("D"));
            dependencies.put("D", Sets.newHashSet("E"));
            dependencies.put("E", Sets.newHashSet());
            List<String> result = resolver.getInTopologicalOrder(dependencies.keySet(), dependencies::get);

            assertThat(result).containsExactlyInAnyOrder("A", "B", "C", "D", "E");
            assertThat(result).containsSubsequence("B", "A");
            assertThat(result).containsSubsequence("C", "A");
            assertThat(result).containsSubsequence("E", "B");
            assertThat(result).containsSubsequence("D", "C");
            assertThat(result).containsSubsequence("E", "D");
        }

        @Test
        void testDirectCycleDetected() {
            Map<String, Set<String>> dependencies = new HashMap<>();
            dependencies.put("A", Sets.newHashSet("B"));
            dependencies.put("B", Sets.newHashSet("A"));
            dependencies.put("C", Sets.newHashSet("A"));
            assertThatThrownBy(() -> resolver.getInTopologicalOrder(dependencies.keySet(), dependencies::get))
                    .isInstanceOf(MethodHookConfigurationResolver.CyclicDataDependencyException.class);
        }

        @Test
        void testIndirectCycleDetected() {
            Map<String, Set<String>> dependencies = new HashMap<>();
            dependencies.put("A", Sets.newHashSet("B"));
            dependencies.put("B", Sets.newHashSet("C"));
            dependencies.put("C", Sets.newHashSet("A"));
            assertThatThrownBy(() -> resolver.getInTopologicalOrder(dependencies.keySet(), dependencies::get))
                    .isInstanceOf(MethodHookConfigurationResolver.CyclicDataDependencyException.class);
        }

    }
}
