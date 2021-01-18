package rocks.inspectit.oce.eum.server.beacon.processor;

import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.Collections;

/**
 * Processor to expand comma separated key values pairs. The expanded values will be available at a new attribute
 * key in the beacon.
 * <p>
 * Example: The attribute <code>t_other=t_domloaded|437</code> will result in <code>t_other.t_domloaded: 437</code>
 */
@Component
public class CsvKeyValueExpanderBeaconProcessor implements BeaconProcessor {

    /**
     * The target key of the attribute to expand.
     */
    private static final String ATTRIBUTE_KEY = "t_other";

    /**
     * The regex (character) used to separate individual value groups.
     */
    private static final String GROUP_SEPARATOR = ",";

    /**
     * The regex (character) used to separate key and value.
     */
    private static final String KEY_VALUE_SEPARATOR = "\\|";

    @Override
    public Beacon process(Beacon beacon) {
        String targetAttribute = beacon.get(ATTRIBUTE_KEY);

        if (targetAttribute != null) {
            String[] attributes = targetAttribute.split(GROUP_SEPARATOR);

            for (String attribute : attributes) {
                String[] splitAttributes = attribute.split(KEY_VALUE_SEPARATOR);

                if (splitAttributes.length == 2) {
                    String resultKey = ATTRIBUTE_KEY + "." + splitAttributes[0];
                    String resultValue = splitAttributes[1];

                    beacon = beacon.merge(Collections.singletonMap(resultKey, resultValue));
                }
            }
        }

        return beacon;
    }
}
