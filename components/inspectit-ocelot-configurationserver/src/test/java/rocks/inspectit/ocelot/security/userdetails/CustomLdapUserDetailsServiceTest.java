package rocks.inspectit.ocelot.security.userdetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.config.model.RoleSettings;
import rocks.inspectit.ocelot.config.model.SecuritySettings;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomLdapUserDetailsServiceTest {

    CustomLdapUserDetailsService customLdapUserDetailsServiceTest;

    @BeforeEach
    public void setUp() {
        LdapUserSearch ldapUserSearch = mock(LdapUserSearch.class);
        DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator = mock(DefaultLdapAuthoritiesPopulator.class);
        customLdapUserDetailsServiceTest = new CustomLdapUserDetailsService(ldapUserSearch, defaultLdapAuthoritiesPopulator);
    }

    public InspectitServerSettings setupRoleSettings(
            List<String> write,
            List<String> commit,
            List<String> admin
    ) {
        InspectitServerSettings mockSettings = mock(InspectitServerSettings.class);
        SecuritySettings mockSecuritySettings = mock(SecuritySettings.class);
        RoleSettings mockRoleSettings = mock(RoleSettings.class);
        LdapSettings mockLdapSettings = mock(LdapSettings.class);

        when(mockSettings.getSecurity()).thenReturn(mockSecuritySettings);
        when(mockSecuritySettings.getLdap()).thenReturn(mockLdapSettings);
        when(mockLdapSettings.getRoles()).thenReturn(mockRoleSettings);

        lenient().when(mockRoleSettings.getWrite()).thenReturn(write);
        lenient().when(mockRoleSettings.getCommit()).thenReturn(commit);
        lenient().when(mockRoleSettings.getAdmin()).thenReturn(admin);


        return mockSettings;

    }

    @Nested
    public class ResolveAccessRole {

        @Test
        public void hasRead() {
            User mockUser = mock(User.class);
            SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority("DefaultAccessRole");
            when(mockUser.getAuthorities()).thenReturn(Collections.singletonList(simpleGrantedAuthority));
            customLdapUserDetailsServiceTest.settings = setupRoleSettings(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );

            String[] output = customLdapUserDetailsServiceTest.resolveAccessRoleSet(mockUser);

            assertThat(output).hasSize(1);
            assertThat(output).contains("OCELOT_READ");
        }

        @Test
        public void hasWrite() {
            User mockUser = mock(User.class);
            SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority("write");
            when(mockUser.getAuthorities()).thenReturn(Collections.singletonList(simpleGrantedAuthority));
            customLdapUserDetailsServiceTest.settings = setupRoleSettings(
                    Collections.singletonList("write"),
                    Collections.emptyList(),
                    Collections.emptyList()
            );

            String[] output = customLdapUserDetailsServiceTest.resolveAccessRoleSet(mockUser);

            assertThat(output).hasSize(2);
            assertThat(output).contains("OCELOT_READ");
            assertThat(output).contains("OCELOT_WRITE");
        }

        @Test
        public void hasCommit() {
            User mockUser = mock(User.class);
            SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority("commit");
            when(mockUser.getAuthorities()).thenReturn(Collections.singletonList(simpleGrantedAuthority));
            customLdapUserDetailsServiceTest.settings = setupRoleSettings(
                    Collections.emptyList(),
                    Collections.singletonList("commit"),
                    Collections.emptyList()
            );

            String[] output = customLdapUserDetailsServiceTest.resolveAccessRoleSet(mockUser);

            assertThat(output).hasSize(3);
            assertThat(output).contains("OCELOT_READ");
            assertThat(output).contains("OCELOT_WRITE");
            assertThat(output).contains("OCELOT_COMMIT");
        }

        @Test
        public void hasAdmin() {
            User mockUser = mock(User.class);
            SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority("admin");
            when(mockUser.getAuthorities()).thenReturn(Collections.singletonList(simpleGrantedAuthority));
            customLdapUserDetailsServiceTest.settings = setupRoleSettings(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.singletonList("admin")
            );

            String[] output = customLdapUserDetailsServiceTest.resolveAccessRoleSet(mockUser);

            assertThat(output).hasSize(4);
            assertThat(output).contains("OCELOT_READ");
            assertThat(output).contains("OCELOT_WRITE");
            assertThat(output).contains("OCELOT_COMMIT");
            assertThat(output).contains("OCELOT_ADMIN");
        }

        @Test
        public void hasMultiple() {
            User mockUser = mock(User.class);
            SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority("admin");
            when(mockUser.getAuthorities()).thenReturn(Collections.singletonList(simpleGrantedAuthority));
            customLdapUserDetailsServiceTest.settings = setupRoleSettings(
                    Collections.singletonList("write"),
                    Collections.singletonList("commit"),
                    Collections.singletonList("admin")
            );
            String[] output = customLdapUserDetailsServiceTest.resolveAccessRoleSet(mockUser);

            assertThat(output).hasSize(4);
            assertThat(output).contains("OCELOT_READ");
            assertThat(output).contains("OCELOT_WRITE");
            assertThat(output).contains("OCELOT_COMMIT");
            assertThat(output).contains("OCELOT_ADMIN");

        }
    }
}
