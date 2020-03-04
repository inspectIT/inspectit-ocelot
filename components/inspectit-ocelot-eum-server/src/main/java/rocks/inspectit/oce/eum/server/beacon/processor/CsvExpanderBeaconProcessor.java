package rocks.inspectit.oce.eum.server.beacon.processor;

import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processor to expand comma separated values. The expanded values will be available at a new attribute
 * key in the beacon.
 * <p>
 * Example: The attribute <code>t_other=t_domloaded|437</code> will result in <code>t_other.t_domloaded: 437</code>
 */
@Component
public class CsvExpanderBeaconProcessor implements BeaconProcessor {

    /**
     * The target key of the attribute to expand.
     */
    private static final String ATTRIBUTE_KEY = "t_other";

    /**
     * The pattern used to extract the key and value of the separated value.
     */
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(.*)\\|(\\d+)");

    @Override
    public Beacon process(Beacon beacon) {
        String targetAttribute = beacon.get(ATTRIBUTE_KEY);

        if (targetAttribute != null) {
            String[] attributes = targetAttribute.split(",");

            for (String attribute : attributes) {
                Matcher matcher = KEY_VALUE_PATTERN.matcher(attribute);

                if (matcher.find()) {
                    String attributeKey = matcher.group(1);
                    String attributeValue = matcher.group(2);

                    String resultKey = ATTRIBUTE_KEY + "." + attributeKey;

                    beacon = beacon.merge(Collections.singletonMap(resultKey, attributeValue));
                }
            }
        }

        return beacon;
    }
}
