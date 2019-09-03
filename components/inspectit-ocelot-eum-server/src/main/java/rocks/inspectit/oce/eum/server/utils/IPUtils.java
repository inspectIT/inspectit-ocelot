package rocks.inspectit.oce.eum.server.utils;

import org.apache.commons.net.util.SubnetUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
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

    /**
     * Checks whether the given CIDRs are overlapping
     * @param net1
     * @param net2
     * @return true, if nets are overlapping
     */
    public boolean overlap(final String net1, final String net2) {
        SubnetUtils.SubnetInfo subnet1 = new SubnetUtils(net1).getInfo();
        SubnetUtils.SubnetInfo subnet2 = new SubnetUtils(net2).getInfo();

        int mask1 = subnet1.asInteger(subnet1.getNetmask());
        int mask2 = subnet2.asInteger(subnet2.getNetmask());

        int maskToUse = mask1 < mask2 ? mask1 : mask2;
        int addr1 = subnet1.asInteger(subnet1.getAddress()) & maskToUse;
        int addr2 = subnet2.asInteger(subnet2.getAddress()) & maskToUse;

        return addr1 == addr2;
    }

    /**
     * Returns true, if given cidr contains given ip
     * @param cidr
     * @param ip
     * @return
     */
    public boolean containsIp(String cidr, String ip){
        IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(cidr);
        return ipAddressMatcher.matches(ip);
    }
}
