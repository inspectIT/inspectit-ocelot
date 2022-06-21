package rocks.inspectit.ocelot.core.tags;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.internal.StringUtils;
import io.opencensus.tags.TagValue;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public final class TagUtils {

    /**
     * Counter for the number of warnings that have already been printed
     */
    @VisibleForTesting
    static int printedWarningCounter = 0;

    /**
     * The time in ms when the last warning was printed
     */
    @VisibleForTesting
    static long lastWarningTime = 0;

    /**
     * The number of maximum warnings that are to be printed
     */
    private final static int MAX_WARNING_PRINTS = 10;

    /**
     * The duration in ms that needs to pass, if MAX_WARNING_PRINTS has been reached
     */
    private final static int WAITING_TIME_IN_MILLI_SECONDS = 600_000;

    /**
     * Boolean that indicates whether the user gets a message that further logs are suppressed
     */
    private static boolean PRINT_FURTHER_MESSAGE = true;

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
        return resolveTageValue(tagKey, value, TagValue::create);
    }

    public static String createTagValueAsString(String tagKey, String value) {
        return resolveTageValue(tagKey, value, s -> s);
    }

    private static <T> T resolveTageValue(String tagKey, String value, Function<String, T> creator) {
        if (isTagValueValid(value)) {
            return creator.apply(value);
        }
        printWarning(tagKey, value);
        return creator.apply("<invalid>");
    }

    private static boolean isTagValueValid(String value) {
        return value.length() <= TagValue.MAX_LENGTH && StringUtils.isPrintableString(value);
    }

    private static void printWarning(String tagKey, String value) {
        if ((System.currentTimeMillis() - lastWarningTime) > WAITING_TIME_IN_MILLI_SECONDS) {
            printedWarningCounter = 0;
            PRINT_FURTHER_MESSAGE = true;
        }
        if (printedWarningCounter < MAX_WARNING_PRINTS) {
            log.warn("Error creating value for tag <{}>: illegal tag value <{}> converted to <invalid>", tagKey, value);
            printedWarningCounter++;
            lastWarningTime = System.currentTimeMillis();
        } else if (PRINT_FURTHER_MESSAGE) {
            log.warn("Further log messages are suppressed");
            PRINT_FURTHER_MESSAGE = false;
        }
    }
}