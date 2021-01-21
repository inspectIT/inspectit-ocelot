package rocks.inspectit.oce.eum.server.utils;

import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TagUtilsTest {

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

}
