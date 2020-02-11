package rocks.inspectit.oce.eum.server.utils;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

/**
 * Resolves the geolocation of given IP, by using the GeoLite2 database
 * (https://dev.maxmind.com/geoip/geoip2/geolite2/)
 */
@Component
@Slf4j
public class GeolocationResolver {

    /**
     * Location of the geoip database.
     */
    private static final InputStream databaseStream = GeolocationResolver.class.getClassLoader().getResourceAsStream("geoip-db/GeoLite2-Country.mmdb");

    private DatabaseReader databaseReader;

    /**
     * Returns country code of current requester.
     *
     * @return country code, empty string, if country code is not resolvable
     */
    public String getCountryCode(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CountryResponse response = databaseReader.country(ipAddress);

            return response.getCountry().getIsoCode();
        } catch (GeoIp2Exception | IOException e) {
            log.debug("The requester address {} could not be resolved", ip);
            return "";
        }
    }

    @PostConstruct
    private void initialize() {
        try {
            databaseReader = new DatabaseReader.Builder(databaseStream).build();
        } catch (IOException e) {
            log.warn("The geoip database could not be found!");
        }
    }
}
