package rocks.inspectit.ocelot.config.model.instrumentation.rules;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.InternalSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.SpecialSensorSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.validation.Violation;
import rocks.inspectit.ocelot.config.validation.ViolationBuilder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;


public class InstrumentationRuleSettingsTest {

    InspectitConfig root;

    InstrumentationSettings instr;

    MetricsSettings metrics;

    InstrumentationRuleSettings rule;

    @Spy
    ViolationBuilder vios;

    @BeforeEach
    void setupDefaultSettings() {
        instr = new InstrumentationSettings();
        instr.setSpecial(new SpecialSensorSettings());
        instr.setInternal(new InternalSettings());

        rule = new InstrumentationRuleSettings();
        instr.setRules(Maps.newHashMap("my-rule", rule));

        metrics = new MetricsSettings();

        root = new InspectitConfig();
        root.setInstrumentation(instr);
        root.setMetrics(metrics);
    }


    @Nested
    class PerformValidation {

        @Test
        void testNonExistingMetric() {
            Map<String, MetricRecordingSettings> metricUsages = new HashMap<>();
            metricUsages.put("m123456", MetricRecordingSettings.builder().value("42").build());

            rule.setMetrics(metricUsages);

            List<Violation> violations = new ArrayList<>();
            instr.performValidation(root, new ViolationBuilder(violations));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("metric");
            assertThat(violations.get(0).getParameters().values()).contains("m123456");
        }

        @Test
        void testNonExistingScope() {
            rule.setScopes(Collections.singletonMap("my-scope", true));

            List<Violation> violations = new ArrayList<>();
            instr.performValidation(root, new ViolationBuilder(violations));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage())
                    .containsIgnoringCase("scope")
                    .containsIgnoringCase("exist");
        }

        @Test
        void testNonExistingInclude() {
            rule.setInclude(Collections.singletonMap("non-existant-rule", true));

            List<Violation> violations = new ArrayList<>();
            instr.performValidation(root, new ViolationBuilder(violations));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage())
                    .containsIgnoringCase("include")
                    .containsIgnoringCase("exist");
        }

        @Test
        void ensureAllCallsValidated() {
            ActionCallSettings preEntryCall = Mockito.spy(ActionCallSettings.class);
            preEntryCall.setAction("someAction");
            doNothing().when(preEntryCall).performValidation(any(), any());
            ActionCallSettings entryCall = Mockito.spy(ActionCallSettings.class);
            entryCall.setAction("someAction");
            doNothing().when(entryCall).performValidation(any(), any());
            ActionCallSettings postEntryCall = Mockito.spy(ActionCallSettings.class);
            postEntryCall.setAction("someAction");
            doNothing().when(postEntryCall).performValidation(any(), any());


            ActionCallSettings preExitCall = Mockito.spy(ActionCallSettings.class);
            preExitCall.setAction("someAction");
            doNothing().when(preExitCall).performValidation(any(), any());
            ActionCallSettings exitCall = Mockito.spy(ActionCallSettings.class);
            exitCall.setAction("someAction");
            doNothing().when(exitCall).performValidation(any(), any());
            ActionCallSettings postExitCall = Mockito.spy(ActionCallSettings.class);
            postExitCall.setAction("someAction");
            doNothing().when(postExitCall).performValidation(any(), any());

            rule.setPreEntry(Collections.singletonMap("pre_entry_data", preEntryCall));
            rule.setEntry(Collections.singletonMap("entry_data", entryCall));
            rule.setPostEntry(Collections.singletonMap("post_entry_data", postEntryCall));

            rule.setPreExit(Collections.singletonMap("pre_exit_data", preExitCall));
            rule.setExit(Collections.singletonMap("exit_data", exitCall));
            rule.setPostExit(Collections.singletonMap("exit_data", postExitCall));

            List<Violation> violations = new ArrayList<>();
            instr.performValidation(root, new ViolationBuilder(violations));

            assertThat(violations).hasSize(0);
            verify(preEntryCall).performValidation(same(instr), any());
            verify(entryCall).performValidation(same(instr), any());
            verify(postEntryCall).performValidation(same(instr), any());
            verify(preExitCall).performValidation(same(instr), any());
            verify(exitCall).performValidation(same(instr), any());
            verify(postExitCall).performValidation(same(instr), any());
        }
    }

}
