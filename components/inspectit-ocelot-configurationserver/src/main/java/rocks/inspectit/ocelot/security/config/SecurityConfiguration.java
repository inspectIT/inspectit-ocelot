package rocks.inspectit.ocelot.security.config;

import com.google.common.annotations.VisibleForTesting;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.filters.AccessLogFilter;
import rocks.inspectit.ocelot.security.jwt.JwtTokenFilter;
import rocks.inspectit.ocelot.security.jwt.JwtTokenManager;
import rocks.inspectit.ocelot.security.userdetails.CustomLdapUserDetailsMapper;
import rocks.inspectit.ocelot.security.userdetails.LocalUserDetailsService;

import java.util.Collections;

/**
 * Spring security configuration enabling authentication on all except excluded endpoints.
 */
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@Configuration
public class SecurityConfiguration {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private JwtTokenManager tokenManager;

    @Autowired
    private AccessLogFilter accessLogFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LocalUserDetailsService localUserDetailsService;

    @Autowired(required = false)
    private LdapContextSource contextSource;

    @Autowired
    @VisibleForTesting
    InspectitServerSettings serverSettings;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sc -> sc.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(
                authz -> authz
                        .requestMatchers(
                                "/v2/**",
                                "/configuration/**",
                                "/csrf",
                                "/",
                                "/ui/**",
                                "/actuator/**",
                                // the following two patterns allow unauthenticated access to Swagger-UI
                                "/swagger*/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/api/v1/agent/configuration",
                                "/api/v1/agent/command",
                                "/api/v1/hook/**"
                        ).permitAll()
                        .anyRequest().hasRole(UserRoleConfiguration.READ_ACCESS)
        );

        http.httpBasic(hb -> hb
                .authenticationEntryPoint((req, resp, authException) -> resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException
                        .getMessage()))
        );

        //TODO: The "correct" way of selectively enabling token based would be to have multiple spring security configs.
        //However, previous attempts of doing so were unsuccessful, therefore we simply exclude them manually in the filter
        http.addFilterBefore(new JwtTokenFilter(tokenManager, eventPublisher, Collections.singletonList("/api/v1/account/password")), BasicAuthenticationFilter.class);
        http.addFilterBefore(accessLogFilter.getFilter(), JwtTokenFilter.class);

        return http.build();
    }

    @Autowired
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (serverSettings.getSecurity().isLdapAuthentication()) {
            configureLdapAuthentication(auth);
        }
        configureLocalAuthentication(auth);
    }

    /**
     * Configures the user authentication to use LDAP user management and authentication
     */
    private void configureLdapAuthentication(AuthenticationManagerBuilder auth) throws Exception {
        LdapSettings ldapSettings = serverSettings.getSecurity().getLdap();

        auth.ldapAuthentication()
                .userDetailsContextMapper(new CustomLdapUserDetailsMapper(ldapSettings))
                .userSearchFilter(ldapSettings.getUserSearchFilter())
                .userSearchBase(ldapSettings.getUserSearchBase())
                .groupSearchFilter(ldapSettings.getGroupSearchFilter())
                .groupSearchBase(ldapSettings.getGroupSearchBase())
                .contextSource(contextSource);
    }

    /**
     * Configures the user authentication to use the local and embedded database for user management and authentication.
     */
    private void configureLocalAuthentication(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(localUserDetailsService).passwordEncoder(passwordEncoder);
    }
}
