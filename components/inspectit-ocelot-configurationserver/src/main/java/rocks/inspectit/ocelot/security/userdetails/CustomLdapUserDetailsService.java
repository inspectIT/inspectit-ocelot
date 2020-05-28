package rocks.inspectit.ocelot.security.userdetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.conditional.ConditionalOnLdap;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.util.Collection;

/**
 * The user details service used for authentication against the configured LDAP system.
 */
@Component
@ConditionalOnLdap
public class CustomLdapUserDetailsService extends LdapUserDetailsService {

    private static class MappingLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {

        private CustomUserAuthoritiesMapper customUserAuthoritiesMapper;

        private DefaultLdapAuthoritiesPopulator delegate;

        private MappingLdapAuthoritiesPopulator(DefaultLdapAuthoritiesPopulator populator, InspectitServerSettings serverSettings) {
            customUserAuthoritiesMapper = new CustomUserAuthoritiesMapper(serverSettings);
            delegate = populator;
        }

        @Override
        public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
            return customUserAuthoritiesMapper.mapAuthorities(delegate.getGrantedAuthorities(userData, username));
        }
    }

    /**
     * Constructor.
     *
     * @param search         created via {@link rocks.inspectit.ocelot.security.config.SecurityBeanConfiguration}
     * @param populator      created via {@link rocks.inspectit.ocelot.security.config.SecurityBeanConfiguration}
     * @param serverSettings the settings used for deriving roles
     */
    @Autowired
    public CustomLdapUserDetailsService(LdapUserSearch search, DefaultLdapAuthoritiesPopulator populator, InspectitServerSettings serverSettings) {
        super(search, new MappingLdapAuthoritiesPopulator(populator, serverSettings));
    }

}
