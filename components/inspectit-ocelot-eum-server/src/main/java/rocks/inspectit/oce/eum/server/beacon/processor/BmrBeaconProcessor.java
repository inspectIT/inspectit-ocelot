package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.Collections;
import java.util.HashMap;

/**
 * Processor to expand comma separated values. The expanded values will be available at a new attribute
 * indexed key in the beacon.
 * <p>
 * Example: The attribute <code>rt.bmr=37,5</code> will result in <code>rt.bmr.startTime: 37</code>, <code>rt.bmr.responseEnd: 5</code>
 */
@Slf4j
@Component
public class BmrBeaconProcessor implements BeaconProcessor {

    /**
     * The target key of the attribute to expand.
     */
    @VisibleForTesting
    static final String ATTRIBUTE_KEY = "rt.bmr";

    /**
     * The regex (character) used to separate individual value groups.
     */
    private static final String GROUP_SEPARATOR = ",";

    /**
     * List of names for all possible values.
     */
    @VisibleForTesting
    static final String[] VALUE_NAMES = {"startTime", "responseEnd", "responseStart", "requestStart", "connectEnd", "secureConnectionStart", "connectStart", "domainLookupEnd", "domainLookupStart", "redirectEnd", "redirectStart"};

    @Override
    public Beacon process(Beacon beacon) {
        String targetAttribute = beacon.get(ATTRIBUTE_KEY);
        HashMap<String, String> bmrAttributes = new HashMap<>();

        if (StringUtils.isNoneBlank(targetAttribute)) {
            String[] attributes = targetAttribute.split(GROUP_SEPARATOR);

            for (int i = 0; i < VALUE_NAMES.length; i++) {
                String resultKey = ATTRIBUTE_KEY + "." + VALUE_NAMES[i];

                String value;
                if (i >= attributes.length || StringUtils.isBlank(attributes[i])) {
                    value = "0";
                } else {
                    value = attributes[i];
                }

                if (NumberUtils.isCreatable(value)) {
                    bmrAttributes.put(resultKey, value);
                } else {
                    log.trace("Error parsing the value <'{}'>: invalid number.", attributes[i]);
                }
            }
            beacon = beacon.merge(bmrAttributes);
        }
        return beacon;
    }
}
