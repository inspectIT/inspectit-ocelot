package rocks.inspectit.oce.eum.server.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rocks.inspectit.oce.eum.server.utils.DefaultTags;
import rocks.inspectit.oce.eum.server.utils.IPUtils;

import java.util.Map;

/**
 * Preprocesses beacon and adds default tags
 */
@Component
public class BeaconPreProcessor {

    @Autowired
    IPUtils ipUtils;

    @Autowired
    GeolocationResolver geolocationResolver;

    public Map<String, String> preProcessBeacon(Map<String, String> beacon) {
        addCountryCode(beacon);
        return beacon;
    }

    /**
     * Adds country code of requester, if resolvable
     *
     * @param beacon the beacon
     */
    private void addCountryCode(Map<String, String> beacon) {
        String ip = ipUtils.getClientIpAddress(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        String countryCode = geolocationResolver.getCountryCode(ip);
        beacon.put(DefaultTags.COUNTRY_CODE.name(), countryCode);
    }
}
