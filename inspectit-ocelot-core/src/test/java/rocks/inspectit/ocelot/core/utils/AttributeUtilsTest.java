package rocks.inspectit.ocelot.core.utils;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeUtilsTest extends SpringTestBase {

    @Test
    void resolveValue() {
        assertThat(AttributeUtils.resolveValue("my-attr-key", "my-attr-value")).isEqualTo("my-attr-value");
    }

    @Test
    void createAttributeValue_tooLong() {
        assertThat(AttributeUtils.resolveValue("my-attr-key", "this-value-is-over-255-characters-long ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------"))
                .isEqualTo("<invalid>");
    }

    @Test
    void createAttributeValue_nonPrintableCharacter() {
        assertThat(AttributeUtils.resolveValue("my-attr-key", "non-printable-character-\u007f")).isEqualTo("<invalid>");
    }

    @Test
    void multipleCreateAttributeValue_nonPrintableCharacter() {
        AttributeUtils.printedWarningCounter = 0;

        for (int i = 0; i < 11; i++) {
            AttributeUtils.resolveValue("my-attr-key", "non-printable-character-\u007f");
        }
        assertLogsOfLevelOrGreater(Level.WARN);
        assertLogCount("Error creating value for attribute", 10);
    }

    @Test
    void multipleCreateAttributeValue_moreThan10Minutes() {
        AttributeUtils.printedWarningCounter = 0;

        for (int i = 0; i < 11; i++) {
            AttributeUtils.resolveValue("my-attr-key", "non-printable-character-\u007f");
        }

        AttributeUtils.lastWarningTime = AttributeUtils.lastWarningTime - 610000;
        AttributeUtils.resolveValue("my-attr-key", "non-printable-character-\u007f");

        assertLogCount("Error creating value for attribute", 11);
    }
}
