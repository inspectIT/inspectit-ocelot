package rocks.inspectit.ocelot.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * Filter for handling token authentication of the webhook endpoints. In case no token or only blank tokens are defined,
 * access will always be denied.
 */
@Slf4j
@AllArgsConstructor
public class WebhookAccessFilter implements Filter {

    /**
     * The name of the query parameter containing the token.
     */
    private static final String TOKEN_PARAMETER = "token";

    /**
     * A list of valid tokens.
     */
    @NonNull
    private final List<String> validTokens;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String secret = request.getParameter(TOKEN_PARAMETER);

        if (StringUtils.isNotBlank(secret) && validTokens.contains(secret)) {
            chain.doFilter(request, response);
        } else {
            String requestUrl = ((HttpServletRequest) request).getRequestURL().toString();
            log.warn("Access has been denied for '{}' because of an invalid or missing token.", requestUrl);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
