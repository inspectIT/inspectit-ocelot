package rocks.inspectit.oce.core.config.model.instrumentation;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.DataProviderCallSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.GenericDataProviderSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.rules.InstrumentationRuleSettings;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class DataProviderCallSettingsTest {

    private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();


    InstrumentationSettings instr;

    DataProviderCallSettings call;

    GenericDataProviderSettings provider;

    @BeforeEach
    void setupDefaultSettings() {
        instr = new InstrumentationSettings();
        instr.setSpecial(new SpecialSensorSettings());
        instr.setInternal(new InternalSettings());

        provider = new GenericDataProviderSettings();
        provider.setValue("null");
        provider.setInput(new HashMap<>());
        instr.setDataProviders(Maps.newHashMap("my-provider", provider));

        call = new DataProviderCallSettings();
        call.setProvider("my-provider");
        call.setDataInput(new HashMap<>());
        call.setConstantInput(new HashMap<>());

        InstrumentationRuleSettings rule = new InstrumentationRuleSettings();
        rule.setEntryData(Maps.newHashMap("my_data", call));
        instr.setRules(Maps.newHashMap("my-rule", rule));
    }


    @Nested
    class CheckDataProviderExistsDecoded {
        @Test
        void testNonExistingProvider() {
            call.setProvider("blabla");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("blabla");
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("provider");
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("exist");
        }
    }

    @Nested
    class CheckNoDuplicateAssignments {

        @Test
        void testDuplicateAssignment() {
            provider.getInput().put("my-prop", "String");
            call.getConstantInput().put("my-prop", "value");
            call.getDataInput().put("my-prop", "my_data_key");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("same variable");
        }
    }

    @Nested
    class CheckNoMissingAssignments {

        @Test
        void testMissingAssignment() {
            provider.getInput().put("my-prop", "String");
            provider.getInput().put("other-prop", "String");
            call.getConstantInput().put("my-prop", "value");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("other-prop");
        }

        @Test
        void testSpecialNotCountedAsAssignment() {
            provider.getInput().put("returnValue", "String");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }
    }

    @Nested
    class CheckNoUnusedAssignments {

        @Test
        void testMissingAssignment() {
            provider.getInput().put("p1", "String");
            call.getConstantInput().put("p1", "value");
            call.getConstantInput().put("P1", "other-value");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("P1");
        }

        @Test
        void testSpecialNotCountedAsAssignment() {
            provider.getInput().put("returnValue", "String");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }
    }

    @Nested
    class CheckNoSpecialVariablesAssigned {

        @Test
        void testNonExistingProvider() {
            provider.getInput().put("my-prop", "String");
            provider.getInput().put("thrown", "Throwable");
            provider.getInput().put("arg3", "String");
            call.getConstantInput().put("my-prop", "value");
            call.getConstantInput().put("arg3", "value");
            call.getDataInput().put("thrown", "my_data_key");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(2);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("special");
            assertThat(violations.get(1).getMessage()).containsIgnoringCase("special");
        }
    }

    @Nested
    class CheckConstantInputsCanBeDecoded {

        @Test
        void testConstantStringAssignment() {
            provider.getInput().put("my-prop", "String");
            call.getConstantInput().put("my-prop", "myValue");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }

        @Test
        void testNullStringAssignment() {
            provider.getInput().put("my-prop", "String");
            call.getConstantInput().put("my-prop", null);

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }


        @Test
        void testFloatToDoubleWrapperAssignment() {
            provider.getInput().put("p1", "Double");
            call.getConstantInput().put("p1", "2.05E-5f");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }


        @Test
        void testFloatToDoublePrimitiveAssignment() {
            provider.getInput().put("p1", "double");
            call.getConstantInput().put("p1", "2.05E-5f");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }


        @Test
        void testDoubleToFloatPrimitiveAssignment() {
            provider.getInput().put("p1", "double");
            call.getConstantInput().put("p1", " -2.05E5 ");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }

        @Test
        void testNullToPrimitiveAssignment() {
            provider.getInput().put("p1", "byte");
            call.getConstantInput().put("p1", null);

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("null");
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("primitive");
        }

        @Test
        void testNullToWrapperAssignment() {
            provider.getInput().put("p1", "java.lang.Byte");
            call.getConstantInput().put("p1", null);

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }

        @Test
        void testEmptyIntegerAssignment() {
            provider.getInput().put("p1", "Integer");
            call.getConstantInput().put("p1", "");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
        }

        @Test
        void testInvalidIntegerAssignment() {
            provider.getInput().put("p1", "Integer");
            call.getConstantInput().put("p1", "14notanumber5");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
        }


        @Test
        void testCorrectCharacterAssignment() {
            provider.getInput().put("p1", "char");
            call.getConstantInput().put("p1", "X");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }

        @Test
        void testEmptyCharacterAssignment() {
            provider.getInput().put("p1", "char");
            call.getConstantInput().put("p1", "");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
        }

        @Test
        void testTooLongCharacterAssignment() {
            provider.getInput().put("p1", "char");
            call.getConstantInput().put("p1", "AB");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
        }

        @Test
        void testUnknownTypeNonNullAssignment() {
            provider.getInput().put("p1", "my.package.DomainObject");
            call.getConstantInput().put("p1", "blabla");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
        }

        @Test
        void testUnknownTypeNullAssignment() {
            provider.getInput().put("p1", "my.package.DomainObject");
            call.getConstantInput().put("p1", null);

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }

        @Test
        void testNonParsableAssignment() {
            provider.setImports(Arrays.asList("java.util"));
            provider.getInput().put("p1", "Map");
            call.getConstantInput().put("p1", "I'm a map");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(1);
        }

        @Test
        void testDateAssignment() {
            provider.setImports(Arrays.asList("java.util"));
            provider.getInput().put("p1", "Date");
            call.getConstantInput().put("p1", "22/05/1950 10:00:42");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }

        @Test
        void testDurationAssignment() {
            provider.setImports(Arrays.asList("java.time"));
            provider.getInput().put("p1", "Duration");
            call.getConstantInput().put("p1", "15s");

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }

        @Test
        void testMapToListAssignment() {
            provider.setImports(Arrays.asList("java.time"));
            HashMap<String, String> nested = new HashMap<>();
            nested.put("0", "firstEntry");
            nested.put("1", "secondEntry");
            provider.getInput().put("p1", "java.util.List");
            call.getConstantInput().put("p1", nested);

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }

        @Test
        void testMapToMapAssignment() {
            provider.setImports(Arrays.asList("java.time"));
            HashMap<String, String> nested = new HashMap<>();
            nested.put("firstKey", "firstEntry");
            nested.put("secondKey", "secondEntry");
            provider.getInput().put("p1", "java.util.Map");
            call.getConstantInput().put("p1", nested);

            List<ConstraintViolation<InstrumentationSettings>> violations = new ArrayList<>(validator.validate(instr));

            assertThat(violations).hasSize(0);
        }
    }
}
