package rocks.inspectit.oce.eum.server.beacon.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link BeaconProcessor} to remove query string parameter from Boomerangs <b>u</b> and <b>pgu</b> Fields.
 * New beacon properties <b>u_no_query</b> and <b>pgu_no_query</b> are added.
 */
@Slf4j
@Component
public class URLNormalizeBeaconProcessor implements BeaconProcessor {

    public static String TAG_U_NO_QUERY = "U_NO_QUERY";
    public static String TAG_PGU_NO_QUERY = "PGU_NO_QUERY";


    @Override
    public Beacon process(Beacon beacon) {
        Map<String, String> uris = new HashMap<>();
        uris.put(TAG_U_NO_QUERY, resolveUrlWithoutParameter("u", beacon));
        uris.put(TAG_PGU_NO_QUERY, resolveUrlWithoutParameter("pgu", beacon));
        return beacon.merge(uris);
    }


    private String resolveUrlWithoutParameter(String sourceProperty, Beacon beacon) {
        if (!beacon.contains(sourceProperty)) {
            return "";
        }
        String url = beacon.get(sourceProperty);
        // Check if parameter exist
        int paramStartIndex = url.indexOf("?");
        return paramStartIndex > -1 ? url.substring(0, paramStartIndex) : url;
    }
}
