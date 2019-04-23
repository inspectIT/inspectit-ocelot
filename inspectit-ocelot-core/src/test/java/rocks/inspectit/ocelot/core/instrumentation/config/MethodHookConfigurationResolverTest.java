package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.collect.Sets;
import io.opencensus.trace.Span;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.DataProviderCallSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.*;
import rocks.inspectit.ocelot.core.testutils.Dummy;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class MethodHookConfigurationResolverTest {

    @Spy
    DataProviderCallSorter scheduler = new DataProviderCallSorter();

    @InjectMocks
    MethodHookConfigurationResolver resolver;

    private final Class<?> DUMMY_CLASS = Dummy.class;
    private final TypeDescription DUMMY_TYPE = TypeDescription.ForLoadedType.of(Dummy.class);
    private final MethodDescription DUMMY_METHOD = DUMMY_TYPE.getDeclaredMethods().stream()
            .filter(m -> m.getName().equals("methodA")).findFirst().get();


    @Nested
    class BuildHookConfiguration {

        InstrumentationConfiguration config;

        GenericDataProviderConfig providerA;
        DataProviderCallConfig callToA1;
        DataProviderCallConfig callToA2;
        GenericDataProviderConfig providerB;
        DataProviderCallConfig callToB;

        @BeforeEach
        void initTestData() {

            config = InstrumentationConfiguration.builder().build();

            providerA = GenericDataProviderConfig.builder()
                    .name("providerA")
                    .build();

            DataProviderCallSettings seta1 = new DataProviderCallSettings();
            seta1.setProvider("providerA");
            callToA1 = DataProviderCallConfig.builder()
                    .name("my_key")
                    .callSettings(seta1)
                    .provider(providerA).build();

            DataProviderCallSettings seta2 = new DataProviderCallSettings();
            seta2.setProvider("providerA");
            callToA2 = DataProviderCallConfig.builder()
                    .name("my_key")
                    .callSettings(seta2)
                    .provider(providerA).build();

            providerB = GenericDataProviderConfig.builder()
                    .name("providerB")
                    .build();

            DataProviderCallSettings setb1 = new DataProviderCallSettings();
            setb1.setProvider("providerB");
            callToB = DataProviderCallConfig.builder()
                    .name("my_key")
                    .callSettings(setb1)
                    .provider(providerB).build();
        }

        @Test
        void verifyProviderConflictsDetected() {
            InstrumentationRule r1 = InstrumentationRule.builder().entryProvider(callToA1).build();
            InstrumentationRule r2 = InstrumentationRule.builder().entryProvider(callToB).build();

            assertThatThrownBy(() -> resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD, config, Sets.newHashSet(r1, r2)))
                    .isInstanceOf(MethodHookConfigurationResolver.ConflictingDefinitionsException.class);
        }

        @Test
        void verifyNoProviderConflictsForSameCall() throws Exception {
            InstrumentationRule r1 = InstrumentationRule.builder().entryProvider(callToA1).build();
            InstrumentationRule r2 = InstrumentationRule.builder().entryProvider(callToA2).build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD, config, Sets.newHashSet(r1, r2));
            assertThat(conf.getEntryProviders()).containsExactly(callToA1);
        }


        @Test
        void verifyMetricConflictsDetected() {
            InstrumentationRule r1 = InstrumentationRule.builder().metric("my_metric", "dataA").build();
            InstrumentationRule r2 = InstrumentationRule.builder().metric("my_metric", "dataB").build();

            assertThatThrownBy(() -> resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD, config, Sets.newHashSet(r1, r2)))
                    .isInstanceOf(MethodHookConfigurationResolver.ConflictingDefinitionsException.class);
        }

        @Test
        void verifyMetricsMasterSwitchRespected() throws Exception {
            InstrumentationRule r1 = InstrumentationRule.builder().metric("my_metric", "dataA").build();

            Map<String, String> result = resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD,
                    config.toBuilder().metricsEnabled(false).build(), Sets.newHashSet(r1)).getDataMetrics();
            assertThat(result).isEmpty();
        }

        @Test
        void verifyMetricsMerged() throws Exception {
            InstrumentationRule r1 = InstrumentationRule.builder().metric("my_metric", "dataA").build();
            InstrumentationRule r2 = InstrumentationRule.builder().metric("my_other_metric", "dataB").build();


            Map<String, String> result = resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD,
                    config, Sets.newHashSet(r1, r2)).getDataMetrics();
            assertThat(result)
                    .hasSize(2)
                    .containsEntry("my_metric", "dataA")
                    .containsEntry("my_other_metric", "dataB");
        }

        @Test
        void verifyTracingMasterSwitchRespected() throws Exception {
            InstrumentationRule r1 = InstrumentationRule.builder().metric("my_metric", "dataA")
                    .tracing(RuleTracingSettings.builder()
                            .startSpan(true)
                            .attributes(Maps.newHashMap("attr", "dataX"))
                            .build())
                    .build();

            MethodHookConfiguration result = resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD,
                    config.toBuilder().tracingEnabled(false).build(), Sets.newHashSet(r1));

            assertThat(result.getTracing().isStartSpan()).isFalse();
            assertThat(result.getTracing().getAttributes()).isEmpty();
        }


        @Test
        void verifyTracingInformationMerged() throws Exception {
            InstrumentationRule r1 = InstrumentationRule.builder()
                    .tracing(RuleTracingSettings.builder()
                            .startSpan(true)
                            .kind(Span.Kind.SERVER)
                            .attributes(Maps.newHashMap("attr", "dataX"))
                            .build())
                    .build();
            InstrumentationRule r2 = InstrumentationRule.builder()
                    .tracing(RuleTracingSettings.builder()
                            .startSpan(true)
                            .name("data_name")
                            .attributes(Maps.newHashMap("attr2", "dataY"))
                            .build())
                    .build();

            MethodTracingConfiguration result = resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD,
                    config, Sets.newHashSet(r1, r2)).getTracing();

            assertThat(result.isStartSpan()).isTrue();
            assertThat(result.getSpanKind()).isEqualTo(Span.Kind.SERVER);
            assertThat(result.getSpanNameDataKey()).isEqualTo("data_name");
            assertThat(result.getAttributes())
                    .hasSize(2)
                    .containsEntry("attr", "dataX")
                    .containsEntry("attr2", "dataY");
        }


        @Test
        void verifyProvidersOrderedByDependencies() throws Exception {
            DataProviderCallSettings dependingOnFirst = new DataProviderCallSettings();
            dependingOnFirst.setProvider("providerA");
            dependingOnFirst.setDataInput(Maps.newHashMap("someArgument", "my_key"));
            DataProviderCallConfig depFirst = DataProviderCallConfig.builder()
                    .name("third_key")
                    .callSettings(dependingOnFirst)
                    .provider(providerA).build();

            DataProviderCallSettings dependingOnSecond = new DataProviderCallSettings();
            dependingOnSecond.setProvider("providerA");
            dependingOnSecond.setDataInput(Maps.newHashMap("someArgument", "second_key"));
            DataProviderCallConfig depSecond = DataProviderCallConfig.builder()
                    .name("second_key")
                    .callSettings(dependingOnSecond)
                    .provider(providerA).build();

            InstrumentationRule r1 = InstrumentationRule.builder()
                    .entryProvider(callToA1)
                    .entryProvider(depSecond).build();
            InstrumentationRule r2 = InstrumentationRule.builder()
                    .entryProvider(depFirst).build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(DUMMY_CLASS, DUMMY_METHOD, config, Sets.newHashSet(r1, r2));
            assertThat(conf.getEntryProviders()).containsExactly(
                    callToA1,
                    depFirst,
                    depSecond
            );
        }

    }

}
