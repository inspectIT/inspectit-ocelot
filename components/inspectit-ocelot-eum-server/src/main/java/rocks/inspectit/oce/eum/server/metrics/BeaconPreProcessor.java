package rocks.inspectit.oce.eum.server.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.utils.DefaultTags;
import rocks.inspectit.oce.eum.server.utils.IPUtils;

import java.util.List;
import java.util.Map;

/**
 * Preprocesses beacon and adds default tags
 */
@Component
public class BeaconPreProcessor {

    @Autowired
    private IPUtils ipUtils;

    @Autowired
    private GeolocationResolver geolocationResolver;

    @Autowired
    private EumServerConfiguration configuration;

    public Beacon preProcessBeacon(Map<String, String> beaconMap) {
        addCountryCode(beaconMap);
        return Beacon.of(beaconMap);
    }

    /**
     * Adds country code of requester, if resolvable
     * As first priority, the custom defined mappings are resolved.
     * As a second priority, the IP is being resolved by using the GeoIP database
     *
     * @param beacon the beacon
     */
    private void addCountryCode(Map<String, String> beacon) {
        String ip = ipUtils.getClientIpAddress(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        String countryCode = resolveCustomIPMapping(ip);
        if (countryCode == null){
            countryCode = geolocationResolver.getCountryCode(ip);
        }
        beacon.put(DefaultTags.COUNTRY_CODE.name(), countryCode);
    }

    /**
     * Resolves custom ip mapping, if defined.
     *
     * @param ip the IP of the beacon
     * @return the CountryCode
     */
    private String resolveCustomIPMapping(String ip) {
        Map<String, List<String>> customIpMapping = configuration.getTags().getCustomIPMapping();
        if (customIpMapping != null) {
            for (Map.Entry<String, List<String>> customCountryCodeDefinition : customIpMapping.entrySet()) {
                if (customCountryCodeDefinition.getValue().stream()
                        .anyMatch(address -> address.contains("/") ? ipUtils.containsIp(address, ip) : address.equals(ip))) {
                    return customCountryCodeDefinition.getKey();
                }
            }
        }
        return null;
    }
}
