package rocks.inspectit.ocelot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import rocks.inspectit.ocelot.authentication.JwtTokenFilter;
import rocks.inspectit.ocelot.authentication.JwtTokenManager;
import rocks.inspectit.ocelot.authentication.NoPopupBasicAuthenticationEntryPoint;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.user.UserDetailsServiceManager;
import rocks.inspectit.ocelot.user.ldap.LdapUtils;

import java.util.Arrays;

/**
 * Spring security configuration enabling authentication on all except excluded endpoints.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    public static final String DEFAUL_ACCESS_USER_ROLE = "OCELOT_ADMIN";

    @Autowired
    JwtTokenManager tokenManager;

    @Autowired
    UserDetailsServiceManager userDetailsServiceManager;

    @Autowired
    InspectitServerSettings serverSettings;

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(
                "/v2/api-docs",
                "/configuration/**",
                "/csrf",
                "/",
                "/ui/**",
                "/swagger*/**",
                "/webjars/**",
                "/api/v1/agent/configuration");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                .and()
                .cors()

                .and()
                .authorizeRequests()
                .anyRequest().hasRole(getAccessRole())

                .and()
                .httpBasic().authenticationEntryPoint(new NoPopupBasicAuthenticationEntryPoint())

                .and()
                //TODO: The "correct" way of selectively enabling token based would be to have multiple spring security configs.
                //However, previous attempts of doing so were unsuccessful, therefore we simply exclude them manually in the filter
                .addFilterBefore(new JwtTokenFilter(tokenManager, Arrays.asList(
                        "/api/v1/account/password"
                )), BasicAuthenticationFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (serverSettings.getSecurity().isLdapAuthentication()) {
            LdapContextSource contextSource = LdapUtils.createLdapContextSource(serverSettings);
            LdapSettings ldapSettings = serverSettings.getSecurity().getLdap();

            auth
                    .ldapAuthentication()
                    .userSearchFilter(ldapSettings.getUserSearchFilter())
                    .userSearchBase(ldapSettings.getUserSearchBase())
                    .groupSearchFilter(ldapSettings.getGroupSearchFilter())
                    .groupSearchBase(ldapSettings.getGroupSearchBase())
                    .contextSource(contextSource);
        } else {
            auth
                    .userDetailsService(userDetailsServiceManager.getUserDetailsService())
                    .passwordEncoder(userDetailsServiceManager.getPasswordEncoder());
        }
    }

    private String getAccessRole() {
        if (serverSettings.getSecurity().isLdapAuthentication()) {
            return serverSettings.getSecurity().getLdap().getAdminGroup();
        } else {
            return DEFAUL_ACCESS_USER_ROLE;
        }
    }
}
