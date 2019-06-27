package rocks.inspectit.ocelot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import rocks.inspectit.ocelot.users.InspectitUserDetailsService;
import rocks.inspectit.ocelot.users.JwtTokenFilter;
import rocks.inspectit.ocelot.users.JwtTokenManager;

@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    JwtTokenManager tokenManager;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                .authorizeRequests()
                .antMatchers("/v2/api-docs",
                        "/configuration/**",
                        "/csrf",
                        "/",
                        "/swagger*/**",
                        "/webjars/**")
                .permitAll()

                .and()
                .authorizeRequests().anyRequest().authenticated()
                .and()
                .httpBasic()
                .and()
                .addFilterBefore(new JwtTokenFilter(tokenManager), BasicAuthenticationFilter.class);
    }

    @Autowired
    public void configureAuth(AuthenticationManagerBuilder auth, InspectitUserDetailsService detailsService) throws Exception {
        auth
                .userDetailsService(detailsService)
                .passwordEncoder(detailsService.getPasswordEncoder());
    }
}
