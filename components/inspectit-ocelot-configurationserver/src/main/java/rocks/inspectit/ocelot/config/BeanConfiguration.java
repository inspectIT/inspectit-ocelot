package rocks.inspectit.ocelot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class BeanConfiguration {

    /**
     * Executor service to use for asynchronous tasks.
     *
     * @param config the applications configuration, gets autowired
     * @return the executor service
     */
    @Bean
    public ScheduledExecutorService fixedThreadPool(InspectitServerSettings config) {
        return Executors.newScheduledThreadPool(config.getThreadPoolSize());
    }
}
