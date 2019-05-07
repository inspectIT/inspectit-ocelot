package rocks.inspectit.ocelot.core.config.model.instrumentation.dataproviders;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.InternalSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.SpecialSensorSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.DataProviderCallSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.ocelot.core.config.model.validation.Violation;
import rocks.inspectit.ocelot.core.config.model.validation.ViolationBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class DataProviderCallSettingsTest {

    InstrumentationSettings instr;

    DataProviderCallSettings call;

    GenericDataProviderSettings provider;

    ViolationBuilder vios;
    List<Violation> violations;

    @BeforeEach
    void setupDefaultSettings() {
        violations = new ArrayList<>();
        vios = new ViolationBuilder(violations);

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
        rule.setEntry(Maps.newHashMap("my_data", call));
        instr.setRules(Maps.newHashMap("my-rule", rule));
    }


    @Nested
    class CheckDataProviderExistsDecoded {
        @Test
        void testNonExistingProvider() {
            call.setProvider("blabla");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getParameters().values()).contains("blabla");
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

            call.performValidation(instr, vios);

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

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getParameters().values()).contains("other-prop");
        }

        @Test
        void testSpecialNotCountedAsAssignment() {
            provider.getInput().put("_returnValue", "String");

            call.performValidation(instr, vios);

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

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getParameters().values()).contains("P1");
        }

        @Test
        void testSpecialNotCountedAsAssignment() {
            provider.getInput().put("_returnValue", "String");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }
    }

    @Nested
    class CheckNoSpecialVariablesAssigned {

        @Test
        void testNonExistingProvider() {
            provider.getInput().put("my-prop", "String");
            provider.getInput().put("_thrown", "Throwable");
            provider.getInput().put("_arg3", "String");
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
            provider.getInput().put("my-prop", "String");
            call.getConstantInput().put("my-prop", "myValue");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testNullStringAssignment() {
            provider.getInput().put("my-prop", "String");
            call.getConstantInput().put("my-prop", null);

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }


        @Test
        void testFloatToDoubleWrapperAssignment() {
            provider.getInput().put("p1", "Double");
            call.getConstantInput().put("p1", "2.05E-5f");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }


        @Test
        void testFloatToDoublePrimitiveAssignment() {
            provider.getInput().put("p1", "double");
            call.getConstantInput().put("p1", "2.05E-5f");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }


        @Test
        void testDoubleToFloatPrimitiveAssignment() {
            provider.getInput().put("p1", "double");
            call.getConstantInput().put("p1", " -2.05E5 ");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testNullToPrimitiveAssignment() {
            provider.getInput().put("p1", "byte");
            call.getConstantInput().put("p1", null);

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("null");
            assertThat(violations.get(0).getMessage()).containsIgnoringCase("primitive");
        }

        @Test
        void testNullToWrapperAssignment() {
            provider.getInput().put("p1", "java.lang.Byte");
            call.getConstantInput().put("p1", null);

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testEmptyIntegerAssignment() {
            provider.getInput().put("p1", "Integer");
            call.getConstantInput().put("p1", "");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testInvalidIntegerAssignment() {
            provider.getInput().put("p1", "Integer");
            call.getConstantInput().put("p1", "14notanumber5");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }


        @Test
        void testCorrectCharacterAssignment() {
            provider.getInput().put("p1", "char");
            call.getConstantInput().put("p1", "X");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testEmptyCharacterAssignment() {
            provider.getInput().put("p1", "char");
            call.getConstantInput().put("p1", "");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testTooLongCharacterAssignment() {
            provider.getInput().put("p1", "char");
            call.getConstantInput().put("p1", "AB");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testUnknownTypeNonNullAssignment() {
            provider.getInput().put("p1", "my.package.DomainObject");
            call.getConstantInput().put("p1", "blabla");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testUnknownTypeNullAssignment() {
            provider.getInput().put("p1", "my.package.DomainObject");
            call.getConstantInput().put("p1", null);

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testNonParsableAssignment() {
            provider.setImports(Arrays.asList("java.util"));
            provider.getInput().put("p1", "Map");
            call.getConstantInput().put("p1", "I'm a map");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(1);
        }

        @Test
        void testDateAssignment() {
            provider.setImports(Arrays.asList("java.util"));
            provider.getInput().put("p1", "Date");
            call.getConstantInput().put("p1", "22/05/1950 10:00:42");

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }

        @Test
        void testDurationAssignment() {
            provider.setImports(Arrays.asList("java.time"));
            provider.getInput().put("p1", "Duration");
            call.getConstantInput().put("p1", "15s");

            call.performValidation(instr, vios);

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

            call.performValidation(instr, vios);

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

            call.performValidation(instr, vios);

            assertThat(violations).hasSize(0);
        }
    }
}
