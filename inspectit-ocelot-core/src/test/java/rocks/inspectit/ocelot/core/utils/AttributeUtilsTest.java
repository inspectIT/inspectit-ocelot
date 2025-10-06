package rocks.inspectit.ocelot.core.utils;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;

public class AttributeUtilsTest extends SpringTestBase {

    @Test
    public void resolveValue() {
        assertThat(AttributeUtils.resolveValue("my-tag-key", "my-tag-value")).isEqualTo("my-tag-value");
    }

    @Test
    public void createTagValue_tooLong() {
        assertThat(AttributeUtils.resolveValue("my-tag-key", "this-value-is-over-255-characters-long ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------"))
                .isEqualTo("<invalid>");
    }

    @Test
    public void createTagValue_nonPrintableCharacter() {
        assertThat(AttributeUtils.resolveValue("my-tag-key", "non-printable-character-\u007f")).isEqualTo("<invalid>");
    }

    @Test
    public void multipleCreateTagValue_nonPrintableCharacter() {
        AttributeUtils.printedWarningCounter = 0;

        for (int i = 0; i < 11; i++) {
            AttributeUtils.resolveValue("my-tag-key", "non-printable-character-\u007f");
        }
        assertLogsOfLevelOrGreater(Level.WARN);
        assertLogCount("Error creating value for attribute", 10);
    }

    @Test
    public void multipleCreateTagValue_moreThan10Minutes() {
        AttributeUtils.printedWarningCounter = 0;

        for (int i = 0; i < 11; i++) {
            AttributeUtils.resolveValue("my-tag-key", "non-printable-character-\u007f");
        }

        AttributeUtils.lastWarningTime = AttributeUtils.lastWarningTime - 610000;
        AttributeUtils.resolveValue("my-tag-key", "non-printable-character-\u007f");

        assertLogCount("Error creating value for attribute", 11);
    }
}
