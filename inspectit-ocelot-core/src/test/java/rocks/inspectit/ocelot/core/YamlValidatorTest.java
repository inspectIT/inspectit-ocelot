package rocks.inspectit.ocelot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class YamlValidatorTest {

    @Autowired
    YamlValidator validator;

    @BeforeEach
    void buildValidator() {
        validator = new YamlValidator();
    }

    @Nested
    public class Parse {

        @Test
        void kebabCaseTest() {
            LinkedList<String> output = new LinkedList<>(Arrays.asList("ich-kann-kebab", "case", "sogar-mit-klammern\\wow", "sachen-gibts"));
            assertEquals(output, validator.parse("inspectit.ich-kann-kebab.case[sogar-mit-klammern\\wow].sachen-gibts"));
        }

        @Test
        void emptyString() {
            LinkedList<String> output = new LinkedList<>();
            assertThat(validator.parse("")).isEqualTo(output);
        }

        @Test
        void nullString() {
            LinkedList<String> output = new LinkedList<>();
            assertThat(validator.parse(null)).isEqualTo(output);
        }

    }

    @Nested
    public class FindUnmappedStrings {

        @Test
        void workingTest() {
            String[] strings = {"irgendwasAnderes"/*Alles was nicht mit inspectit. beginnt wird ignoriert*/, "inspectit.config.das-geht-nie-imLeben", "inspectit.metrics.processor.enabled",
                    "inspectit.metrics.processor.enbaled"};
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit.config.das-geht-nie-imLeben", "inspectit.metrics.processor.enbaled"));
            assertEquals(output, validator.findInvalidPropertyNames(strings));
        }

        @Test
        void emptyString() {
            String[] strings = {"inspectit.config.das-geht-nie-imLeben", "", "inspectit.metrics.processor.enabled"};
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit.config.das-geht-nie-imLeben"));
            assertEquals(output, validator.findInvalidPropertyNames(strings));
        }

        @Test
        void nullString() {
            String[] strings = {"inspectit.config.das-geht-nie-imLeben", null, "inspectit.metrics.processor.enabled"};
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit.config.das-geht-nie-imLeben"));
            assertEquals(output, validator.findInvalidPropertyNames(strings));
        }

        @Test
        void existingMap() {
            String[] strings = {"inspectit.metrics.definitions[jvm/gc/concurrent/phase/time].description"};
            ArrayList<String> output = new ArrayList<>();
            assertEquals(output, validator.findInvalidPropertyNames(strings));
        }

        @Test
        void existingList() {
            String[] strings = {"inspectit.instrumentation.scopes.jdbc_statement_execute.interfaces[0].matcher-mode"};
            ArrayList<String> output = new ArrayList<>();
            assertEquals(output, validator.findInvalidPropertyNames(strings));
        }
    }

}
