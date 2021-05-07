package rocks.inspectit.ocelot.core.tags;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.internal.StringUtils;
import io.opencensus.tags.TagValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TagUtils {

    @VisibleForTesting
    static int printedWarningCounter = 0;

    private final static int maxWarningPrints = 10;

    @VisibleForTesting
    static long lastWarningTime = 0;

    private final static int waitingTimeInMilliSeconds = 600000;

    private TagUtils() {
        // empty private default constructor for util class
    }

    /**
     * Constructs a {@code io.opencensus.tags.TagValue} from the given string.
     * If String is not valid an <code>&lt;invalid&gt;</code> TagName is created.
     *
     * @param tagKey the tag key
     * @param value  the tag value
     *
     * @return the created TagValue with 'v' or '&lt;invalid&gt;'
     */
    public static TagValue createTagValue(String tagKey, String value) {
        if (isTagValueValid(value)) {
            return TagValue.create(value);
        }
        printWarning(tagKey, value);
        return TagValue.create("<invalid>");
    }

    private static boolean isTagValueValid(String value) {
        return value.length() <= TagValue.MAX_LENGTH && StringUtils.isPrintableString(value);
    }

    private static void printWarning(String tagKey, String value) {
        if (printedWarningCounter < maxWarningPrints) {
            log.warn("Error creating value for tag <{}>: illegal tag value <{}> converted to <invalid>", tagKey, value);

            printedWarningCounter++;
            if (printedWarningCounter == maxWarningPrints) {
                lastWarningTime = System.currentTimeMillis();
            }
            
        } else if ((System.currentTimeMillis() - lastWarningTime) > waitingTimeInMilliSeconds) {
            printedWarningCounter = 0;
            printWarning(tagKey, value);
        }
        return;
    }
}
