package rocks.inspectit.oce.eum.server.metrics;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

@Slf4j
/**
 * Resolves the geolocation of given IP, by using the GeoLite2 database
 * (https://dev.maxmind.com/geoip/geoip2/geolite2/)
 */
@Component
public class GeolocationResolver {
    /**
     * Location of the geoip database.
     */
    private static final String dbLocation = GeolocationResolver.class.getClassLoader().getResource("geoip-db/GeoLite2-Country.mmdb").getFile();

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
            log.info("The requester address {} could not be resolved", ip);
            return "";
        }
    }

    @PostConstruct
    private void initialize() {
        try {
            File database = new File(dbLocation);
            databaseReader = new DatabaseReader.Builder(database).build();
        } catch (IOException e) {
            log.warn("The geoip database could not be found!");
        }
    }
}
