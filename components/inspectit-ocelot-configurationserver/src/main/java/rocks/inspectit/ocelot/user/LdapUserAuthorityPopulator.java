package rocks.inspectit.ocelot.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.security.userdetails.CustomLdapUserDetailsService;

import java.util.Collection;

/**
 * This LdapAuthoritiesPopulator is used to populate user object with roles upon basic authentication when they are
 * authenticated by a ldap server.
 */
@Service
public class LdapUserAuthorityPopulator implements LdapAuthoritiesPopulator {

    @Autowired
    private CustomLdapUserDetailsService customLdapUserDetailsService;

    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
        return customLdapUserDetailsService.loadUserByUsername(username).getAuthorities();
    }
}