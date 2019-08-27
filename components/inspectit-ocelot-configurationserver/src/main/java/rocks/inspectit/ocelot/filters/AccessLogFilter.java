package rocks.inspectit.ocelot.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Filter for printing messages on authentication failure and success.
 * Does not directly inherit from {@link Filter} to ensure that spring does not automatically place it in the Filter chain.
 * Instead, the filter is manually registered in the right postion by the {@link rocks.inspectit.ocelot.security.config.SecurityConfiguration}.
 */
@Slf4j
@Component
public class AccessLogFilter {

    /**
     * The event listeners do not have access to the {@link HttpServletRequest}.
     * Therefore, we remember the request identifier in a thread local variable,
     * which can then be accessed by the event listeners.
     */
    private ThreadLocal<String> requestIdentifier = new ThreadLocal<>();

    @Autowired
    private InspectitServerSettings config;

    @EventListener
    private void authSuccess(AuthenticationSuccessEvent event) {
        if (config.getSecurity().isAccessLog()) {
            String user = event.getAuthentication().getName();
            log.info("'{}' accessed {}", user, requestIdentifier.get());
        }
    }

    @EventListener
    private void authFailure(AbstractAuthenticationFailureEvent event) {
        if (config.getSecurity().isAccessLog()) {
            Authentication authentication = event.getAuthentication();
            log.info("Authentication ({}) failed for {}", authentication.getName(), requestIdentifier.get(), event.getException());
        }
    }

    public Filter getFilter() {
        return (ServletRequest request, ServletResponse response, FilterChain chain) -> {
            try {
                if (request instanceof HttpServletRequest) {
                    HttpServletRequest httpReq = (HttpServletRequest) request;
                    requestIdentifier.set(httpReq.getMethod() + " " + httpReq.getServletPath());
                }
                chain.doFilter(request, response);
            } finally {
                requestIdentifier.remove();
            }
        };
    }
}
