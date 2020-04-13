package rocks.inspectit.ocelot.security.config;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import static rocks.inspectit.ocelot.security.userdetails.LocalUserDetailsService.DEFAULT_ACCESS_USER_ROLE;

/**
 * Shared spring security configuration enabling authentication on all except excluded endpoints.
 * <p>
 * Sub-classes can extend the configuration.
 */
public class SharedSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    @VisibleForTesting
    InspectitServerSettings serverSettings;

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(
                "/v2/api-docs",
                "/configuration/**",
                "/csrf",
                "/",
                "/ui/**",
                "/actuator/**",
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
                .anyRequest().hasRole(getAccessRole());
    }

    /**
     * Returns the role name which is required by users to get access to the secured API endpoints.
     * In case LDAP is not used, a constant role name is used, otherwise the configured role name of the LDAP settings is used.
     *
     * @return the role name to use
     */
    private String getAccessRole() {
        if (serverSettings.getSecurity().isLdapAuthentication()) {
            return serverSettings.getSecurity().getLdap().getAdminGroup();
        } else {
            return DEFAULT_ACCESS_USER_ROLE;
        }
    }

}
