package rocks.inspectit.ocelot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import rocks.inspectit.ocelot.authentication.JwtTokenFilter;
import rocks.inspectit.ocelot.authentication.JwtTokenManager;
import rocks.inspectit.ocelot.authentication.NoPopupBasicAuthenticationEntryPoint;
import rocks.inspectit.ocelot.user.LocalUserDetailsService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * Spring security configuration enabling authentication on all except excluded endpoints.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    JwtTokenManager tokenManager;

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
                .anyRequest().hasRole("SHIP_CREW")

                .and()
                .httpBasic().authenticationEntryPoint(new NoPopupBasicAuthenticationEntryPoint())

                .and()
                //TODO: The "correct" way of selectively enabling token based would be to have multiple spring security configs.
                //However, previous attempts of doing so were unsuccessful, therefore we simply exclude them manually in the filter
                .addFilterBefore(new JwtTokenFilter(tokenManager, Arrays.asList(
                        "/api/v1/account/password"
                )), BasicAuthenticationFilter.class);
    }

    @Autowired
    public void configureAuth(AuthenticationManagerBuilder auth, LocalUserDetailsService detailsService) throws Exception {
//        auth
//                .userDetailsService(detailsService)
//                .passwordEncoder(detailsService.getPasswordEncoder());

        auth
                .ldapAuthentication()

                .userSearchFilter("(uid={0})")
                .userSearchBase("ou=people,dc=planetexpress,dc=com")
                .groupSearchFilter("(member={0})")
                .groupSearchBase("ou=people,dc=planetexpress,dc=com")

                .contextSource()
                .url("ldap://localhost:389/")
                .port(389)
                .managerDn("cn=admin,dc=planetexpress,dc=com")
                .managerPassword("GoodNewsEveryone");
    }
}
