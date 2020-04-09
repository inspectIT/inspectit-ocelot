package rocks.inspectit.ocelot.core.tags;

import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonTagsManagerTest {

    @Test
    public void createTagValue() {
        assertThat(CommonTagsManager.createTagValue("my-tag-value")).isEqualTo(TagValue.create("my-tag-value"));
    }

    @Test
    public void createTagValue_tooLong() {
        assertThat(CommonTagsManager.createTagValue("this-value-is-over-255-characters-long ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------"))
                .isEqualTo(TagValue.create("<invalid>"));
    }

    @Test
    public void createTagValue_nonPrintableCharacter() {
        assertThat(CommonTagsManager.createTagValue("non-printable-character-\u007f"))
                .isEqualTo(TagValue.create("<invalid>"));
    }

}
