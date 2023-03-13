package rocks.inspectit.ocelot.core.tags;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.internal.StringUtils;
import io.opencensus.tags.TagValue;
import io.opentelemetry.api.common.AttributeKey;
import lombok.extern.slf4j.Slf4j;

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
        if (isTagValueValid(value)) {
            return TagValue.create(value);
        }
        printWarning(tagKey, value);
        return TagValue.create("<invalid>");
    }

    private static boolean isTagValueValid(String value) {
        return value.length() <= TagValue.MAX_LENGTH && StringUtils.isPrintableString(value);
    }

    /**
     * Constructs an attribute value (String)  from the given string.
     * If the string is not valid an <code>&lt;invalid&gt;</code> value is created.
     *
     * @param attributeKey the attribute key
     * @param value        the value
     *
     * @return the created TagValue with 'v' or '&lt;invalid&gt;'
     */
    public static String createAttributeValue(String attributeKey, String value) {
        if (isAttributeValueValid(value)) {
            return value;
        }
        printWarning(attributeKey, value);
        return "<invalid>";
    }

    /**
     * Constructs an attribute value (String)  from the given string.
     * If the string is not valid an <code>&lt;invalid&gt;</code> value is created.
     *
     * @param attributeKey the attribute key
     * @param value        the value
     *
     * @return the created TagValue with 'v' or '&lt;invalid&gt;'
     */
    public static String createAttributeValue(AttributeKey attributeKey, String value) {
        return createAttributeValue(attributeKey.getKey(), value);
    }

    private final static int ATTRIBUTE_MAX_LENGTH = 255;

    private static boolean isAttributeValueValid(String value) {
        // TODO: get value from the current SdkTracerProvider. This does not work currently as the OpenTelemetryController registers the OpenTelemetry after the CommonTagsManager initializes
        int maxLength = ATTRIBUTE_MAX_LENGTH; //openTelemetryController.getSpanLimits().getMaxAttributeValueLength();
        return value.length() <= maxLength && isPrintableString(value);
    }

    /**
     * This method was copied from {@link io.opencensus.internal}.
     * <p>
     * Determines whether the {@code String} contains only printable characters.
     *
     * @param str the {@code String} to be validated.
     *
     * @return whether the {@code String} contains only printable characters.
     */
    public static boolean isPrintableString(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!isPrintableChar(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * This class was copied from {@link io.opencensus.internal}.
     */
    private static boolean isPrintableChar(char ch) {
        return ch >= ' ' && ch <= '~';
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