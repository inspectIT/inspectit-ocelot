package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentationRuleResolverTest {

    @Mock
    private InstrumentationScopeResolver scopeResolver;

    @InjectMocks
    private InstrumentationRuleResolver ruleResolver;

    @Nested
    public class Resolve {

        @Test
        public void emptySettings() {
            InstrumentationSettings settings = new InstrumentationSettings();

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).isEmpty();
        }

        @Test
        public void resolveRulesNoScopes() {
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setScopes(Collections.singletonMap("scope-key", true));
            ruleSettings.setEnabled(true);
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            when(scopeResolver.resolve(settings)).thenReturn(Collections.emptyMap());

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getName).contains("rule-key");
            assertThat(result).flatExtracting(InstrumentationRule::getScopes).isEmpty();
            verify(scopeResolver).resolve(settings);
            verifyNoMoreInteractions(scopeResolver);
        }

        @Test
        public void resolveRules() {
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setScopes(Collections.singletonMap("scope-key", true));
            ruleSettings.setEnabled(true);
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            InstrumentationScope scope = new InstrumentationScope(null, null);

            when(scopeResolver.resolve(settings)).thenReturn(Collections.singletonMap("scope-key", scope));

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getName).contains("rule-key");
            assertThat(result).flatExtracting(InstrumentationRule::getScopes).contains(scope);
            verify(scopeResolver).resolve(settings);
            verifyNoMoreInteractions(scopeResolver);
        }

        @Test
        public void resolveRulesDisabledScope() {
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setScopes(Collections.singletonMap("scope-key", false));
            ruleSettings.setEnabled(true);
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            InstrumentationScope scope = new InstrumentationScope(null, null);

            when(scopeResolver.resolve(settings)).thenReturn(Collections.singletonMap("scope-key", scope));

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getName).contains("rule-key");
            assertThat(result).flatExtracting(InstrumentationRule::getScopes).isEmpty();
            verify(scopeResolver).resolve(settings);
            verifyNoMoreInteractions(scopeResolver);
        }


        @Test
        public void verifyPreEntryActionsPreserved() {
            ActionCallSettings first = Mockito.mock(ActionCallSettings.class);
            ActionCallSettings second = Mockito.mock(ActionCallSettings.class);
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setEnabled(true);
            ruleSettings.setPreEntry(ImmutableMap.of("first", first, "second", second));
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getPreEntryActions)
                    .hasSize(2)
                    .anySatisfy((ac) -> verifyActionCall(ac, "first", first))
                    .anySatisfy((ac) -> verifyActionCall(ac, "second", second));
        }


        @Test
        public void verifyEntryActionsPreserved() {
            ActionCallSettings first = Mockito.mock(ActionCallSettings.class);
            ActionCallSettings second = Mockito.mock(ActionCallSettings.class);
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setEnabled(true);
            ruleSettings.setEntry(ImmutableMap.of("first", first, "second", second));
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getEntryActions)
                    .hasSize(2)
                    .anySatisfy((ac) -> verifyActionCall(ac, "first", first))
                    .anySatisfy((ac) -> verifyActionCall(ac, "second", second));
        }


        @Test
        public void verifyPostEntryActionsPreserved() {
            ActionCallSettings first = Mockito.mock(ActionCallSettings.class);
            ActionCallSettings second = Mockito.mock(ActionCallSettings.class);
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setEnabled(true);
            ruleSettings.setPostEntry(ImmutableMap.of("first", first, "second", second));
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getPostEntryActions)
                    .hasSize(2)
                    .anySatisfy((ac) -> verifyActionCall(ac, "first", first))
                    .anySatisfy((ac) -> verifyActionCall(ac, "second", second));
        }

        @Test
        public void verifyPreExitActionsPreserved() {
            ActionCallSettings first = Mockito.mock(ActionCallSettings.class);
            ActionCallSettings second = Mockito.mock(ActionCallSettings.class);
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setEnabled(true);
            ruleSettings.setPreExit(ImmutableMap.of("first", first, "second", second));
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getPreExitActions)
                    .hasSize(2)
                    .anySatisfy((ac) -> verifyActionCall(ac, "first", first))
                    .anySatisfy((ac) -> verifyActionCall(ac, "second", second));
        }


        @Test
        public void verifyExitActionsPreserved() {
            ActionCallSettings first = Mockito.mock(ActionCallSettings.class);
            ActionCallSettings second = Mockito.mock(ActionCallSettings.class);
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setEnabled(true);
            ruleSettings.setExit(ImmutableMap.of("first", first, "second", second));
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getExitActions)
                    .hasSize(2)
                    .anySatisfy((ac) -> verifyActionCall(ac, "first", first))
                    .anySatisfy((ac) -> verifyActionCall(ac, "second", second));
        }


        @Test
        public void verifyPostExitActionsPreserved() {
            ActionCallSettings first = Mockito.mock(ActionCallSettings.class);
            ActionCallSettings second = Mockito.mock(ActionCallSettings.class);
            InstrumentationRuleSettings ruleSettings = new InstrumentationRuleSettings();
            ruleSettings.setEnabled(true);
            ruleSettings.setPostExit(ImmutableMap.of("first", first, "second", second));
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setRules(Collections.singletonMap("rule-key", ruleSettings));

            Set<InstrumentationRule> result = ruleResolver.resolve(settings, Collections.emptyMap());

            assertThat(result).hasSize(1);
            assertThat(result).flatExtracting(InstrumentationRule::getPostExitActions)
                    .hasSize(2)
                    .anySatisfy((ac) -> verifyActionCall(ac, "first", first))
                    .anySatisfy((ac) -> verifyActionCall(ac, "second", second));
        }

        private void verifyActionCall(ActionCallConfig ac, String name, ActionCallSettings callSettings) {
            assertThat(ac.getName()).isEqualTo(name);
            assertThat(ac.getCallSettings()).isSameAs(callSettings);
        }

    }

    @Nested
    class ResolveMetricRecordings {

        @Test
        void emptyMetricName() {
            MetricRecordingSettings rec = MetricRecordingSettings.builder()
                    .value("42")
                    .metric("")
                    .build();
            InstrumentationRuleSettings irs = new InstrumentationRuleSettings();
            irs.setMetrics(ImmutableMap.of("default_metric", rec));

            Multiset<MetricRecordingSettings> result = ruleResolver.resolveMetricRecordings(irs);

            assertThat(result).hasOnlyOneElementSatisfying(element -> {
                assertThat(element.getValue()).isEqualTo("42");
                assertThat(element.getMetric()).isEqualTo("default_metric");
            });
        }


        @Test
        void customMetricName() {
            MetricRecordingSettings rec = MetricRecordingSettings.builder()
                    .value("42")
                    .metric("my_metric")
                    .build();
            InstrumentationRuleSettings irs = new InstrumentationRuleSettings();
            irs.setMetrics(ImmutableMap.of("default_metric", rec));

            Multiset<MetricRecordingSettings> result = ruleResolver.resolveMetricRecordings(irs);

            assertThat(result).hasOnlyOneElementSatisfying(element -> {
                assertThat(element.getValue()).isEqualTo("42");
                assertThat(element.getMetric()).isEqualTo("my_metric");
            });
        }


        @Test
        void emptyValue() {
            MetricRecordingSettings rec = MetricRecordingSettings.builder()
                    .value("")
                    .build();
            InstrumentationRuleSettings irs = new InstrumentationRuleSettings();
            irs.setMetrics(ImmutableMap.of("default_metric", rec));

            Multiset<MetricRecordingSettings> result = ruleResolver.resolveMetricRecordings(irs);

            assertThat(result).isEmpty();
        }

        @Test
        void nullValue() {
            MetricRecordingSettings rec = MetricRecordingSettings.builder()
                    .value(null)
                    .build();
            InstrumentationRuleSettings irs = new InstrumentationRuleSettings();
            irs.setMetrics(ImmutableMap.of("default_metric", rec));

            Multiset<MetricRecordingSettings> result = ruleResolver.resolveMetricRecordings(irs);

            assertThat(result).isEmpty();
        }

        @Test
        void tagsCopied() {
            MetricRecordingSettings rec = MetricRecordingSettings.builder()
                    .value("42")
                    .metric("")
                    .constantTags(Collections.singletonMap("constant", "true"))
                    .dataTags(Collections.singletonMap("constant", "false"))
                    .build();
            InstrumentationRuleSettings irs = new InstrumentationRuleSettings();
            irs.setMetrics(ImmutableMap.of("default_metric", rec));

            Multiset<MetricRecordingSettings> result = ruleResolver.resolveMetricRecordings(irs);

            assertThat(result).hasOnlyOneElementSatisfying(settings -> {
                assertThat(settings.getConstantTags()).hasSize(1).containsEntry("constant", "true");
                assertThat(settings.getDataTags()).hasSize(1).containsEntry("constant", "false");

                // no modification allowed
                assertThat(catchThrowable(() -> settings.getConstantTags().clear())).isNotNull();
                assertThat(catchThrowable(() -> settings.getDataTags().clear())).isNotNull();
            });

        }

    }

}