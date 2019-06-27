package rocks.inspectit.ocelot.users;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * This filter performs token-based authentication.
 * Tokens are created via the {@link rocks.inspectit.ocelot.rest.users.AccountController}.
 * This Filter is registered by the {@link rocks.inspectit.ocelot.config.SecurityConfig}.
 */
@AllArgsConstructor
public class JwtTokenFilter extends GenericFilterBean {

    private JwtTokenManager tokenManager;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        String token = extractBearerToken((HttpServletRequest) req);

        if (token != null) {
            try {
                Authentication authentication = tokenManager.authenticateWithToken(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                logger.debug("Token-based authentication failed: {}", e);
            }
        }
        chain.doFilter(req, res);
    }

    /**
     * Extracts the bearer token from the 'Authorization' HTTP header
     *
     * @param req the request to extract the token from
     * @return the token, if a Bearer Authorization header was found, otherwise null
     */
    private String extractBearerToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (!StringUtils.isEmpty(header) && header.toLowerCase().startsWith("bearer ")) {
            return header.substring("bearer ".length());
        }
        return null;
    }

}
