package rocks.inspectit.oce.eum.server.beacon.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.Collections;

/**
 * Processor to expand comma separated values. The expanded values will be available at a new attribute
 * indexed key in the beacon.
 * <p>
 * Example: The attribute <code>rt.bmr=37,5</code> will result in <code>rt.bmr.0: 37</code>, <code>rt.bmr.1: 5</code>
 * and <code>rt.bmr.sum: 42</code>
 */
@Slf4j
@Component
public class CsvExpanderBeaconProcessor implements BeaconProcessor {

    /**
     * The target key of the attribute to expand.
     */
    private static final String ATTRIBUTE_KEY = "rt.bmr";

    /**
     * The regex (character) used to separate individual value groups.
     */
    private static final String GROUP_SEPARATOR = ",";

    @Override
    public Beacon process(Beacon beacon) {
        String targetAttribute = beacon.get(ATTRIBUTE_KEY);

        if (StringUtils.isNoneBlank(targetAttribute)) {
            int sum = 0;
            boolean isSum = false;
            String[] attributes = targetAttribute.split(GROUP_SEPARATOR);

            for (int i = 0; i < attributes.length; i++) {
                String resultKey = ATTRIBUTE_KEY + "." + i;

                try {
                    sum += Integer.parseInt(attributes[i]);
                    isSum = true;
                    beacon = beacon.merge(Collections.singletonMap(resultKey, attributes[i]));
                } catch (Exception e) {
                    log.error("Error parsing the value <'{}'>: invalid number.", attributes[i]);
                }
            }
            if (isSum) {
                beacon = beacon.merge(Collections.singletonMap(ATTRIBUTE_KEY + ".sum", String.valueOf(sum)));
            }
        }
        return beacon;
    }
}
