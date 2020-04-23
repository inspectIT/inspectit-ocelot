package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.utils.GeolocationResolver;
import rocks.inspectit.oce.eum.server.utils.IPUtils;

import java.util.List;
import java.util.Map;

/**
 * BeaconProcessor to attach country code to Beacon.
 */
@Component
public class CountryCodeBeaconProcessor implements BeaconProcessor {

    public static String TAG_COUNTRY_CODE = "COUNTRY_CODE";

    @Autowired
    private IPUtils ipUtils;

    @Autowired
    private GeolocationResolver geolocationResolver;

    @Autowired
    private EumServerConfiguration configuration;


    @Override
    public Beacon process(Beacon beacon) {
        String countryCode = resolveCountryCode();
        return beacon.merge(ImmutableMap.of(TAG_COUNTRY_CODE, countryCode));
    }

    /**
     * Adds country code of requester, if resolvable
     * As first priority, the custom defined mappings are resolved.
     * As a second priority, the IP is being resolved by using the GeoIP database
     */
    private String resolveCountryCode() {
        String countryCode = "";
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            String ip = ipUtils.getClientIpAddress(requestAttributes.getRequest());
            countryCode = resolveCustomIPMapping(ip);
            if (countryCode == null) {
                countryCode = geolocationResolver.getCountryCode(ip);
            }
        }

        return countryCode;
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
