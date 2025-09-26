package rocks.inspectit.ocelot.core.utils;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.internal.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AttributeUtils {

    /** The maximum length of a single attribute value */
    private static final int MAX_VALUE_LENGTH = 255;

    /** The string to use, if a provided attribute value is invalid */
    private static final String INVALID_VALUE = "<invalid>";

    /** The counter for the number of warnings that have already been printed */
    @VisibleForTesting
    static int printedWarningCounter = 0;

    /** The time in ms when the last warning was printed */
    @VisibleForTesting
    static long lastWarningTime = 0;

    /** The number of maximum warnings that are to be printed */
    private final static int MAX_WARNING_PRINTS = 10;

    /** The duration in ms that needs to pass, if MAX_WARNING_PRINTS has been reached */
    private final static int WAITING_TIME_IN_MILLI_SECONDS = 600_000;

    /** The marker that indicates whether the user gets a message that further logs are suppressed */
    private static boolean PRINT_FURTHER_MESSAGE = true;

    private AttributeUtils() {}

    /**
     * Converts the provided baggage data to attributes.
     *
     * @param baggage the baggage
     *
     * @return the baggage data as attributes
     */
    public static Attributes toAttributes(Baggage baggage) {
        AttributesBuilder builder = Attributes.builder();
        baggage.asMap()
                .forEach((key, entry) -> builder.put(key, entry.getValue()));
        return builder.build();
    }

    /**
     * Resolves the attribute value object to a string
     *
     * @return the value as a string
     */
    public static String resolveValue(String key, Object value) {
        if (isAttributeValueValid(value)) {
            return value.toString();
        }

        printWarning(key, value);
        return INVALID_VALUE;
    }

    private static boolean isAttributeValueValid(Object value) {
        if (value != null) {
            String valueString = value.toString();
            return valueString.length() <= MAX_VALUE_LENGTH && StringUtils.isPrintableString(valueString);
        }
        return false;
    }

    private static void printWarning(String key, Object value) {
        if ((System.currentTimeMillis() - lastWarningTime) > WAITING_TIME_IN_MILLI_SECONDS) {
            printedWarningCounter = 0;
            PRINT_FURTHER_MESSAGE = true;
        }
        if (printedWarningCounter < MAX_WARNING_PRINTS) {
            log.warn("Error creating value for attribute <{}>: illegal value <{}> converted to {}", key, value, INVALID_VALUE);
            printedWarningCounter++;
            lastWarningTime = System.currentTimeMillis();
        } else if (PRINT_FURTHER_MESSAGE) {
            log.warn("Further log messages are suppressed");
            PRINT_FURTHER_MESSAGE = false;
        }
    }
}
