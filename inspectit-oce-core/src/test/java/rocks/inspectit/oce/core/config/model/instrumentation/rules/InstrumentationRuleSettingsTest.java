package rocks.inspectit.oce.core.config.model.instrumentation.rules;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.InternalSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.SpecialSensorSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.DataProviderCallSettings;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;


public class InstrumentationRuleSettingsTest {

    private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();


    InstrumentationSettings instr;

    InstrumentationRuleSettings rule;

    @BeforeEach
    void setupDefaultSettings() {
        instr = new InstrumentationSettings();
        instr.setSpecial(new SpecialSensorSettings());
        instr.setInternal(new InternalSettings());

        rule = new InstrumentationRuleSettings();
        instr.setRules(Maps.newHashMap("my-rule", rule));
    }


    @Nested
    class PerformValidation {
        @Test
        void testNonExistingScope() {
            rule.setScopes(Collections.singletonMap("my-scope", true));

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("scope");
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("exist");
        }

        @Test
        void ensureAllCallsValidated() {
            DataProviderCallSettings entryCall = Mockito.spy(DataProviderCallSettings.class);
            entryCall.setProvider("someProvider");
            doNothing().when(entryCall).performValidation(any(), any());
            DataProviderCallSettings exitCall = Mockito.spy(DataProviderCallSettings.class);
            exitCall.setProvider("someProvider");
            doNothing().when(exitCall).performValidation(any(), any());

            rule.setEntry(Collections.singletonMap("entry_data", entryCall));
            rule.setExit(Collections.singletonMap("exit_data", exitCall));

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
            verify(entryCall, times(1)).performValidation(same(instr), any());
            verify(exitCall, times(1)).performValidation(same(instr), any());
        }
    }

}
