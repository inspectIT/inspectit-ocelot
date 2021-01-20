package rocks.inspectit.ocelot.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.event.AuthorizationFailureEvent;
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
 * Instead, the filter is manually registered in the right position by the {@link rocks.inspectit.ocelot.security.config.SecurityConfiguration}.
 */
@Slf4j
@Component
public class AccessLogFilter {

    @Autowired
    private InspectitServerSettings config;

    /**
     * The event listeners do not have access to the {@link HttpServletRequest}.
     * Therefore, we remember the request identifier in a thread local variable,
     * which can then be accessed by the event listeners.
     */
    private ThreadLocal<String> requestIdentifier = new ThreadLocal<>();

    @EventListener
    private void authSuccess(AuthenticationSuccessEvent event) {
        if (config.getSecurity().isAccessLog()) {
            String user = event.getAuthentication().getName();
            log.info("'{}' accessed '{}'", user, requestIdentifier.get());
        }
    }

    /**
     * This event is triggered if an authentication fails due to bad credentials.
     *
     * @param event - the {@link AuthorizationFailureEvent}
     */
    @EventListener
    private void authFailure(AuthorizationFailureEvent event) {
        handleAuthFailure(event.getAuthentication(), event.getAccessDeniedException());
    }

    /**
     * This event is triggered if an authentication fails because no credentials have been provided.
     *
     * @param event the {@link AbstractAuthenticationFailureEvent}
     */
    @EventListener
    private void authFailure(AbstractAuthenticationFailureEvent event) {
        handleAuthFailure(event.getAuthentication(), event.getException());
    }

    /**
     * Handles and logs the failed authentications.
     *
     * @param authentication the {@link Authentication} used for requesting access
     * @param exception      the reason why an authentication has failed
     */
    private void handleAuthFailure(Authentication authentication, Exception exception) {
        String url = requestIdentifier.get();
        if (url != null) {
            requestIdentifier.remove();
            if (config.getSecurity().isAccessLog()) {
                log.warn("Authentication ({}) failed for '{}': {}", authentication.getName(), url, exception.getMessage());
            }
        }
    }

    public Filter getFilter() {
        return (ServletRequest request, ServletResponse response, FilterChain chain) -> {
            try {
                if (request instanceof HttpServletRequest) {
                    HttpServletRequest httpRequest = (HttpServletRequest) request;
                    requestIdentifier.set(httpRequest.getMethod() + " " + httpRequest.getServletPath());
                }
                chain.doFilter(request, response);
            } finally {
                requestIdentifier.remove();
            }
        };
    }
}
