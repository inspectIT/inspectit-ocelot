package rocks.inspectit.oce.eum.server.utils;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * Util to extract the origin IP from the given request.
 */
@Component
public class IPUtils {

    /**
     * IP header candidates.
     */
    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"};

    /**
     * Returns the origin ip of the requester.
     *
     * @return the ip
     */
    public String getClientIpAddress(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && ipList.length() != 0 && !"unknown".equalsIgnoreCase(ipList)) {
                String ip = ipList.split(",")[0];
                return ip;
            }
        }
        return request.getRemoteAddr();
    }
}
