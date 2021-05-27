package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.utils.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Processor to enrich the beacon with client header information. Each header will be available at a new attribute
 * with the {@link #HEADER_PREFIX} in the beacon.
 * <p>
 * Example: A header <code>Accept-Encoding=gzip</code> will result in <code>client.header.Accept-Encoding: gzip</code>
 */
@Slf4j
@Component
public class ClientHeaderBeaconProcessor implements BeaconProcessor {

    private final String HEADER_PREFIX = "client.header.";

    /**
     * Supplier for accessing the current request.
     */
    @VisibleForTesting
    Supplier<HttpServletRequest> requestSupplier = RequestUtils::getCurrentRequest;

    @Override
    public Beacon process(Beacon beacon) {
        HttpServletRequest request = requestSupplier.get();
        if (request == null) {
            return beacon;
        }

        Map<String, String> requestHeaders = Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(header -> HEADER_PREFIX + header, header -> String.join(",", Collections.list(request
                        .getHeaders(header)))));

        return beacon.merge(requestHeaders);
    }
}
