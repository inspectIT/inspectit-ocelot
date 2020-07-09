package rocks.inspectit.ocelot.config.model.instrumentation.actions;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.InternalSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.SpecialSensorSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.ocelot.config.validation.Violation;
import rocks.inspectit.ocelot.config.validation.ViolationBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ActionCallSettingsTest {

    InstrumentationSettings instr;

    ActionCallSettings call;

    GenericActionSettings action;

    ViolationBuilder vios;

    List<Violation> violations;

    @BeforeEach
    void setupDefaultSettings() {
        violations = new ArrayList<>();
        vios = new ViolationBuilder(violations);

        instr = new InstrumentationSettings();
        instr.setSpecial(new SpecialSensorSettings());
        instr.setInternal(new InternalSettings());

        action = new GenericActionSettings();
        action.setValue("null");
        action.setInput(new HashMap<>());
        instr.setActions(Maps.newHashMap("my-action", action));

        call = new ActionCallSettings();
        call.setAction("my-action");
        call.setDataInput(new HashMap<>());
        call.setConstantInput(new HashMap<>());

        InstrumentationRuleSettings rule = new InstrumentationRuleSettings();
        rule.setEntry(Maps.newHashMap("my_data", call));
        instr.setRules(Maps.newHashMap("my-rule", rule));
    }

    @Nested
    class CheckActionExistsDecoded {

        @Test
        void testNonExistingAction() {
            call.setAction("blabla");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getParameters().values()).contains("blabla");
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("action");
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("exist");
        }
    }

    @Nested
    class CheckNoDuplicateAssignments {

        @Test
        void testDuplicateAssignment() {
            action.getInput().put("my-prop", "String");
            call.getConstantInput().put("my-prop", "value");
            call.getDataInput().put("my-prop", "my_data_key");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("same variable");
        }
    }

    @Nested
    class CheckNoMissingAssignments {

        @Test
        void testMissingAssignment() {
            action.getInput().put("my-prop", "String");
            action.getInput().put("other-prop", "String");
            call.getConstantInput().put("my-prop", "value");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getParameters().values()).contains("other-prop");
        }

        @Test
        void testSpecialNotCountedAsAssignment() {
            action.getInput().put("_returnValue", "String");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }
    }

    @Nested
    class CheckNoUnusedAssignments {

        @Test
        void testMissingAssignment() {
            action.getInput().put("p1", "String");
            call.getConstantInput().put("p1", "value");
            call.getConstantInput().put("P1", "other-value");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getParameters().values()).contains("P1");
        }

        @Test
        void testSpecialNotCountedAsAssignment() {
            action.getInput().put("_returnValue", "String");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }
    }

    @Nested
    class CheckNoSpecialVariablesAssigned {

        @Test
        void testNonExistingAction() {
            action.getInput().put("my-prop", "String");
            action.getInput().put("_thrown", "Throwable");
            action.getInput().put("_arg3", "String");
            call.getConstantInput().put("my-prop", "value");
            call.getConstantInput().put("_arg3", "value");
            call.getDataInput().put("_thrown", "my_data_key");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(2);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("special");
            assertThat(violations.get(1).getMessage()).containsIgnoringCase("special");
        }
    }

    @Nested
    class CheckConstantInputsCanBeDecoded {

        @Test
        void testConstantStringAssignment() {
            action.getInput().put("my-prop", "String");
            call.getConstantInput().put("my-prop", "myValue");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testNullStringAssignment() {
            action.getInput().put("my-prop", "String");
            call.getConstantInput().put("my-prop", null);

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testFloatToDoubleWrapperAssignment() {
            action.getInput().put("p1", "Double");
            call.getConstantInput().put("p1", "2.05E-5f");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testFloatToDoublePrimitiveAssignment() {
            action.getInput().put("p1", "double");
            call.getConstantInput().put("p1", "2.05E-5f");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testDoubleToFloatPrimitiveAssignment() {
            action.getInput().put("p1", "double");
            call.getConstantInput().put("p1", " -2.05E5 ");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testNullToPrimitiveAssignment() {
            action.getInput().put("p1", "byte");
            call.getConstantInput().put("p1", null);

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("null");
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("primitive");
        }

        @Test
        void testNullToWrapperAssignment() {
            action.getInput().put("p1", "java.lang.Byte");
            call.getConstantInput().put("p1", null);

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testEmptyIntegerAssignment() {
            action.getInput().put("p1", "Integer");
            call.getConstantInput().put("p1", "");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testInvalidIntegerAssignment() {
            action.getInput().put("p1", "Integer");
            call.getConstantInput().put("p1", "14notanumber5");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testCorrectCharacterAssignment() {
            action.getInput().put("p1", "char");
            call.getConstantInput().put("p1", "X");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testEmptyCharacterAssignment() {
            action.getInput().put("p1", "char");
            call.getConstantInput().put("p1", "");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testTooLongCharacterAssignment() {
            action.getInput().put("p1", "char");
            call.getConstantInput().put("p1", "AB");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testUnknownTypeNonNullAssignment() {
            action.getInput().put("p1", "my.package.DomainObject");
            call.getConstantInput().put("p1", "blabla");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testUnknownTypeNullAssignment() {
            action.getInput().put("p1", "my.package.DomainObject");
            call.getConstantInput().put("p1", null);

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testNonParsableAssignment() {
            action.setImports(Arrays.asList("java.util"));
            action.getInput().put("p1", "Map");
            call.getConstantInput().put("p1", "I'm a map");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testDateAssignment() {
            action.setImports(Arrays.asList("java.util"));
            action.getInput().put("p1", "Date");
            call.getConstantInput().put("p1", "22/05/1950 10:00:42");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testDurationAssignment() {
            action.setImports(Arrays.asList("java.time"));
            action.getInput().put("p1", "Duration");
            call.getConstantInput().put("p1", "15s");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testMapToListAssignment() {
            action.setImports(Arrays.asList("java.time"));
            HashMap<String, String> nested = new HashMap<>();
            nested.put("0", "firstEntry");
            nested.put("1", "secondEntry");
            action.getInput().put("p1", "java.util.List");
            call.getConstantInput().put("p1", nested);

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testMapToMapAssignment() {
            action.setImports(Arrays.asList("java.time"));
            HashMap<String, String> nested = new HashMap<>();
            nested.put("firstKey", "firstEntry");
            nested.put("secondKey", "secondEntry");
            action.getInput().put("p1", "java.util.Map");
            call.getConstantInput().put("p1", nested);

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }
    }
}
