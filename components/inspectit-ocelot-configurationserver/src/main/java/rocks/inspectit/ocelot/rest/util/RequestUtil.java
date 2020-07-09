package rocks.inspectit.ocelot.rest.util;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

public class RequestUtil {

    /**
     * If the handling method for this request is mapped to a path like some/path/**,
     * this method returns the part of the url after the /**.
     *
     * @param request the request
     *
     * @return the part of the request path after /**
     */
    public static String getRequestSubPath(HttpServletRequest request) {
        String path = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        String bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        return new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);
    }
}
