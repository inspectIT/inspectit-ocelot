package rocks.inspectit.ocelot.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import rocks.inspectit.ocelot.filters.AccessLogFilter;

import java.util.Collections;

/**
 * Spring security configuration when proxy authentication is enabled.
 */
@ConditionalOnProperty(value = "inspectit-config-server.security.auth-proxy.enabled", havingValue = "true")
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class AuthProxySecurityConfiguration extends SharedSecurityConfiguration {

    @Autowired
    private AccessLogFilter accessLogFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        //TODO I see no reason to enable any other auth in addition here
        // I also don't get why is there both basic and token auth by default now, it should be token with the auth and resource servers
        // so token is used to get resources and form auth (with ot without ldap) is used for login purposes
        // now everything is mixed imo

        http
                // specific stuff for auth proxy
                .exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                .addFilterAfter(preAuthenticationFilter(), RequestHeaderAuthenticationFilter.class)
                .addFilterBefore(accessLogFilter.getFilter(), RequestHeaderAuthenticationFilter.class);


    }

    @Bean
    public RequestHeaderAuthenticationFilter preAuthenticationFilter() {
        RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter = new RequestHeaderAuthenticationFilter();
        requestHeaderAuthenticationFilter.setPrincipalRequestHeader(serverSettings.getSecurity().getAuthProxy().getPrincipalRequestHeader());
        requestHeaderAuthenticationFilter.setAuthenticationManager(authenticationManager());
        requestHeaderAuthenticationFilter.setExceptionIfHeaderMissing(false);
        return requestHeaderAuthenticationFilter;
    }

    @Override
    protected AuthenticationManager authenticationManager() {
        return new ProviderManager(Collections.singletonList(authenticationProvider()));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(userDetailsServiceWrapper());
        authenticationProvider.setThrowExceptionWhenTokenRejected(false);

        return authenticationProvider;
    }

    @Bean
    public AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsServiceWrapper() {
        // note that if ldap is active the user details service to use here will the ldap one
        return new UserDetailsByNameServiceWrapper<>(userDetailsService);
    }

}
