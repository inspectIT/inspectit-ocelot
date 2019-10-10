package rocks.inspectit.ocelot.security.jwt;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import rocks.inspectit.ocelot.security.config.SecurityConfiguration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * This filter performs token-based authentication.
 * Tokens are created via the {@link rocks.inspectit.ocelot.rest.users.AccountController}.
 * This Filter is registered by the {@link SecurityConfiguration}.
 */
@AllArgsConstructor
public class JwtTokenFilter extends GenericFilterBean {

    private JwtTokenManager tokenManager;

    private ApplicationEventPublisher eventDrain;

    private List<String> excludePatterns;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (excludePatterns != null) {
            String servletPath = ((HttpServletRequest) req).getServletPath();
            for (String pattern : excludePatterns) {
                if (pathMatcher.match(pattern, servletPath)) {
                    chain.doFilter(req, res);
                    return;
                }
            }
        }

        String token = extractBearerToken((HttpServletRequest) req);

        boolean authenticationThroughFilter = false;

        if (token != null) {
            try {
                Authentication authentication = tokenManager.authenticateWithToken(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                eventDrain.publishEvent(new AuthenticationSuccessEvent(authentication));
                authenticationThroughFilter = true;
            } catch (Exception e) {
                Authentication dummyAuth = new UsernamePasswordAuthenticationToken("<unknown>", "", Collections.emptyList());
                eventDrain.publishEvent(new AuthenticationFailureBadCredentialsEvent(dummyAuth, new BadCredentialsException("Invalid JWT token")));
                logger.debug("Token-based authentication failed: {}", e);
            }
        }
        chain.doFilter(req, res);
        if (authenticationThroughFilter) {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
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
