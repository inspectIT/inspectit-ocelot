package rocks.inspectit.ocelot.core.tags;

import ch.qos.logback.classic.Level;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;

public class TagUtilsTest extends SpringTestBase {

    @Test
    public void createTagValue() {
        assertThat(TagUtils.createTagValue("my-tag-key", "my-tag-value")).isEqualTo(TagValue.create("my-tag-value"));
    }

    @Test
    public void createTagValue_tooLong() {
        assertThat(TagUtils.createTagValue("my-tag-key", "this-value-is-over-255-characters-long ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------"))
                .isEqualTo(TagValue.create("<invalid>"));
    }

    @Test
    public void createTagValue_nonPrintableCharacter() {
        assertThat(TagUtils.createTagValue("my-tag-key", "non-printable-character-\u007f")).isEqualTo(TagValue.create("<invalid>"));
    }

    @Test
    public void multipleCreateTagValue_nonPrintableCharacter() {
        for (int i = 0; i < 11; i++) {
            TagUtils.createTagValue("my-tag-key", "non-printable-character-\u007f");
        }
        assertLogsOfLevelOrGreater(Level.WARN);
        assertLogCount("Error creating value for tag", 10);
    }

}
