package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.opencensus.trace.Span;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.callsorting.GenericActionCallSorter;
import rocks.inspectit.ocelot.core.instrumentation.config.model.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class MethodHookConfigurationResolverTest {

    @Spy
    GenericActionCallSorter scheduler = new GenericActionCallSorter();

    @InjectMocks
    MethodHookConfigurationResolver resolver;


    @Nested
    class BuildHookConfiguration {

        InstrumentationConfiguration config;

        GenericActionConfig providerA;
        ActionCallConfig callToA1;
        ActionCallConfig callToA2;
        GenericActionConfig providerB;
        ActionCallConfig callToB;

        @BeforeEach
        void initTestData() {

            config = InstrumentationConfiguration.builder().build();

            providerA = GenericActionConfig.builder()
                    .name("providerA")
                    .build();

            ActionCallSettings seta1 = new ActionCallSettings();
            seta1.setAction("providerA");
            callToA1 = ActionCallConfig.builder()
                    .name("my_key")
                    .callSettings(seta1)
                    .action(providerA).build();

            ActionCallSettings seta2 = new ActionCallSettings();
            seta2.setAction("providerA");
            callToA2 = ActionCallConfig.builder()
                    .name("my_key")
                    .callSettings(seta2)
                    .action(providerA).build();

            providerB = GenericActionConfig.builder()
                    .name("providerB")
                    .build();

            ActionCallSettings setb1 = new ActionCallSettings();
            setb1.setAction("providerB");
            callToB = ActionCallConfig.builder()
                    .name("my_key")
                    .callSettings(setb1)
                    .action(providerB).build();
        }

        @Test
        void verifyMetricConflictsDetected() {
            InstrumentationRule r1 = InstrumentationRule.builder().metric("my_metric", "dataA").build();
            InstrumentationRule r2 = InstrumentationRule.builder().metric("my_metric", "dataB").build();

            assertThatThrownBy(() -> resolver.buildHookConfiguration(config, Sets.newHashSet(r1, r2)))
                    .isInstanceOf(MethodHookConfigurationResolver.ConflictingDefinitionsException.class);
        }

        @Test
        void verifyMetricsMasterSwitchRespected() throws Exception {
            InstrumentationRule r1 = InstrumentationRule.builder().metric("my_metric", "dataA").build();

            Map<String, String> result = resolver.buildHookConfiguration(
                    config.toBuilder().metricsEnabled(false).build(), Sets.newHashSet(r1)).getDataMetrics();
            assertThat(result).isEmpty();
        }

        @Test
        void verifyMetricsMerged() throws Exception {
            InstrumentationRule r1 = InstrumentationRule.builder().metric("my_metric", "dataA").build();
            InstrumentationRule r2 = InstrumentationRule.builder().metric("my_other_metric", "dataB").build();


            Map<String, String> result = resolver.buildHookConfiguration(
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

            MethodHookConfiguration result = resolver.buildHookConfiguration(
                    config.toBuilder().tracingEnabled(false).build(), Sets.newHashSet(r1));

            assertThat(result.getTracing().getStartSpan()).isFalse();
            assertThat(result.getTracing().getAttributes()).isEmpty();
        }


        @Test
        void verifyTracingInformationMerged() throws Exception {
            InstrumentationRule r1 = InstrumentationRule.builder()
                    .tracing(RuleTracingSettings.builder()
                            .startSpan(true)
                            .kind(Span.Kind.SERVER)
                            .storeSpan("store_span")
                            .continueSpan("my_span")
                            .attributes(Maps.newHashMap("attr", "dataX"))
                            .build())
                    .build();
            InstrumentationRule r2 = InstrumentationRule.builder()
                    .tracing(RuleTracingSettings.builder()
                            .startSpan(true)
                            .name("data_name")
                            .endSpan(false)
                            .attributes(Maps.newHashMap("attr2", "dataY"))
                            .build())
                    .build();

            RuleTracingSettings result = resolver.buildHookConfiguration(
                    config, Sets.newHashSet(r1, r2)).getTracing();

            assertThat(result.getStartSpan()).isTrue();
            assertThat(result.getStoreSpan()).isEqualTo("store_span");
            assertThat(result.getContinueSpan()).isEqualTo("my_span");
            assertThat(result.getEndSpan()).isFalse();
            assertThat(result.getKind()).isEqualTo(Span.Kind.SERVER);
            assertThat(result.getName()).isEqualTo("data_name");
            assertThat(result.getAttributes())
                    .hasSize(2)
                    .containsEntry("attr", "dataX")
                    .containsEntry("attr2", "dataY");
        }


        @Test
        void verifyTracingCustomSamplingProbabilityRespected() throws Exception {
            config = InstrumentationConfiguration.builder().defaultTraceSampleProbability(0.5).build();
            InstrumentationRule r1 = InstrumentationRule.builder()
                    .tracing(RuleTracingSettings.builder()
                            .startSpan(true)
                            .sampleProbability("foo")
                            .build())
                    .build();

            RuleTracingSettings result = resolver.buildHookConfiguration(
                    config, Sets.newHashSet(r1)).getTracing();

            assertThat(result.getSampleProbability()).isEqualTo("foo");
        }

        @Test
        void verifyTracingDefaultSamplingProbabilityAvailable() throws Exception {
            config = InstrumentationConfiguration.builder().defaultTraceSampleProbability(0.5).build();

            InstrumentationRule r1 = InstrumentationRule.builder()
                    .tracing(RuleTracingSettings.builder()
                            .startSpan(true)
                            .sampleProbability(null)
                            .build())
                    .build();

            RuleTracingSettings result = resolver.buildHookConfiguration(
                    config, Sets.newHashSet(r1)).getTracing();

            assertThat(result.getSampleProbability()).isEqualTo("0.5");
        }


        @Test
        void verifyProvidersOrderedByDependencies() throws Exception {
            ActionCallSettings dependingOnFirst = new ActionCallSettings();
            dependingOnFirst.setAction("providerA");
            dependingOnFirst.setDataInput(Maps.newHashMap("someArgument", "my_key"));
            ActionCallConfig depFirst = ActionCallConfig.builder()
                    .name("second_key")
                    .callSettings(dependingOnFirst)
                    .action(providerA).build();

            ActionCallSettings dependingOnSecond = new ActionCallSettings();
            dependingOnSecond.setAction("providerA");
            dependingOnSecond.setDataInput(Maps.newHashMap("someArgument", "second_key"));
            ActionCallConfig depSecond = ActionCallConfig.builder()
                    .name("second_key")
                    .callSettings(dependingOnSecond)
                    .action(providerA).build();

            InstrumentationRule r1 = InstrumentationRule.builder()
                    .entryAction(callToA1)
                    .entryAction(depSecond).build();
            InstrumentationRule r2 = InstrumentationRule.builder()
                    .entryAction(depFirst).build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(config, Sets.newHashSet(r1, r2));
            assertThat(conf.getEntryActions()).containsExactly(
                    callToA1,
                    depFirst,
                    depSecond
            );
        }


        @Test
        void verifyPreEntryProvidersPreserved() throws Exception {
            ActionCallSettings firstSettings = new ActionCallSettings();
            firstSettings.setAction("providerA");
            ActionCallConfig first = ActionCallConfig.builder()
                    .name("first")
                    .callSettings(firstSettings)
                    .action(providerA).build();

            ActionCallSettings secondSettings = new ActionCallSettings();
            secondSettings.setAction("providerB");
            secondSettings.setDataInput(ImmutableMap.of("somearg", "first"));
            ActionCallConfig second = ActionCallConfig.builder()
                    .name("second")
                    .callSettings(secondSettings)
                    .action(providerB).build();

            InstrumentationRule r1 = InstrumentationRule.builder()
                    .preEntryAction(first)
                    .preEntryAction(second)
                    .build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(config, Sets.newHashSet(r1));
            assertThat(conf.getPreEntryActions()).containsExactly(
                    first,
                    second
            );
        }

        @Test
        void verifyEntryProvidersPreserved() throws Exception {
            ActionCallSettings firstSettings = new ActionCallSettings();
            firstSettings.setAction("providerA");
            ActionCallConfig first = ActionCallConfig.builder()
                    .name("first")
                    .callSettings(firstSettings)
                    .action(providerA).build();

            ActionCallSettings secondSettings = new ActionCallSettings();
            secondSettings.setAction("providerB");
            secondSettings.setDataInput(ImmutableMap.of("somearg", "first"));
            ActionCallConfig second = ActionCallConfig.builder()
                    .name("second")
                    .callSettings(secondSettings)
                    .action(providerB).build();

            InstrumentationRule r1 = InstrumentationRule.builder()
                    .entryAction(first)
                    .entryAction(second)
                    .build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(config, Sets.newHashSet(r1));
            assertThat(conf.getEntryActions()).containsExactly(
                    first,
                    second
            );
        }


        @Test
        void verifyPostEntryProvidersPreserved() throws Exception {
            ActionCallSettings firstSettings = new ActionCallSettings();
            firstSettings.setAction("providerA");
            ActionCallConfig first = ActionCallConfig.builder()
                    .name("first")
                    .callSettings(firstSettings)
                    .action(providerA).build();

            ActionCallSettings secondSettings = new ActionCallSettings();
            secondSettings.setAction("providerB");
            secondSettings.setDataInput(ImmutableMap.of("somearg", "first"));
            ActionCallConfig second = ActionCallConfig.builder()
                    .name("second")
                    .callSettings(secondSettings)
                    .action(providerB).build();

            InstrumentationRule r1 = InstrumentationRule.builder()
                    .postEntryAction(first)
                    .postEntryAction(second)
                    .build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(config, Sets.newHashSet(r1));
            assertThat(conf.getPostEntryActions()).containsExactly(
                    first,
                    second
            );
        }

        @Test
        void verifyPreExitProvidersPreserved() throws Exception {
            ActionCallSettings firstSettings = new ActionCallSettings();
            firstSettings.setAction("providerA");
            ActionCallConfig first = ActionCallConfig.builder()
                    .name("first")
                    .callSettings(firstSettings)
                    .action(providerA).build();

            ActionCallSettings secondSettings = new ActionCallSettings();
            secondSettings.setAction("providerB");
            secondSettings.setDataInput(ImmutableMap.of("somearg", "first"));
            ActionCallConfig second = ActionCallConfig.builder()
                    .name("second")
                    .callSettings(secondSettings)
                    .action(providerB).build();

            InstrumentationRule r1 = InstrumentationRule.builder()
                    .preExitAction(first)
                    .preExitAction(second)
                    .build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(config, Sets.newHashSet(r1));
            assertThat(conf.getPreExitActions()).containsExactly(
                    first,
                    second
            );
        }

        @Test
        void verifyExitProvidersPreserved() throws Exception {
            ActionCallSettings firstSettings = new ActionCallSettings();
            firstSettings.setAction("providerA");
            ActionCallConfig first = ActionCallConfig.builder()
                    .name("first")
                    .callSettings(firstSettings)
                    .action(providerA).build();

            ActionCallSettings secondSettings = new ActionCallSettings();
            secondSettings.setAction("providerB");
            secondSettings.setDataInput(ImmutableMap.of("somearg", "first"));
            ActionCallConfig second = ActionCallConfig.builder()
                    .name("second")
                    .callSettings(secondSettings)
                    .action(providerB).build();

            InstrumentationRule r1 = InstrumentationRule.builder()
                    .exitAction(first)
                    .exitAction(second)
                    .build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(config, Sets.newHashSet(r1));
            assertThat(conf.getExitActions()).containsExactly(
                    first,
                    second
            );
        }


        @Test
        void verifyPostExitProvidersPreserved() throws Exception {
            ActionCallSettings firstSettings = new ActionCallSettings();
            firstSettings.setAction("providerA");
            ActionCallConfig first = ActionCallConfig.builder()
                    .name("first")
                    .callSettings(firstSettings)
                    .action(providerA).build();

            ActionCallSettings secondSettings = new ActionCallSettings();
            secondSettings.setAction("providerB");
            secondSettings.setDataInput(ImmutableMap.of("somearg", "first"));
            ActionCallConfig second = ActionCallConfig.builder()
                    .name("second")
                    .callSettings(secondSettings)
                    .action(providerB).build();

            InstrumentationRule r1 = InstrumentationRule.builder()
                    .postExitAction(first)
                    .postExitAction(second)
                    .build();

            MethodHookConfiguration conf = resolver.buildHookConfiguration(config, Sets.newHashSet(r1));
            assertThat(conf.getPostExitActions()).containsExactly(
                    first,
                    second
            );
        }

    }

}
