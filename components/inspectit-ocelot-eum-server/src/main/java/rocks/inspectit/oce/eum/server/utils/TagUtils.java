package rocks.inspectit.oce.eum.server.utils;

import io.opencensus.internal.StringUtils;
import io.opencensus.tags.TagValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TagUtils {

    private static boolean isWarningPrinted = false;

    private TagUtils() {
        // empty private default constructor for util class
    }

    /**
     * Constructs a {@code io.opencensus.tags.TagValue} from the given string.
     * If String is not valid an <code>&lt;invalid&gt;</code> TagName is created.
     *
     * @param tagKey the tag name
     * @param value  the tag value
     *
     * @return the created TagValue with 'value' or '&lt;invalid&gt;'
     */
    public static TagValue createTagValue(String tagKey, String value) {
        if (isTagValueValid(value)) {
            return TagValue.create(value);
        }
        printWarningOnce(tagKey, value);
        return TagValue.create("<invalid>");
    }

    private static boolean isTagValueValid(String value) {
        return value.length() <= TagValue.MAX_LENGTH && StringUtils.isPrintableString(value);
    }

    private static void printWarningOnce(String tagKey, String value) {
        if (!isWarningPrinted) {
            log.warn("Error creating value for tag <{}>: illegal tag value <{}> converted to <invalid>", tagKey, value);
            isWarningPrinted = true;
        }
    }

}
