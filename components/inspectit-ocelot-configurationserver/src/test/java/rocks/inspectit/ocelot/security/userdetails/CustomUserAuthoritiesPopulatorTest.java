package rocks.inspectit.ocelot.security.userdetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapRoleResolveSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.config.model.SecuritySettings;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomUserAuthoritiesPopulatorTest {

    CustomUserAuthoritiesMapper customUserAuthoritiesMapper;

    @BeforeEach
    public void setUp() {
        customUserAuthoritiesMapper = new CustomUserAuthoritiesMapper();
    }

    public InspectitServerSettings setupRoleSettings(
            List<String> write,
            List<String> commit,
            List<String> admin
    ) {
        InspectitServerSettings mockSettings = mock(InspectitServerSettings.class);
        SecuritySettings mockSecuritySettings = mock(SecuritySettings.class);
        LdapRoleResolveSettings mockLdapRoleResolveSettings = mock(LdapRoleResolveSettings.class);
        LdapSettings mockLdapSettings = mock(LdapSettings.class);

        when(mockSettings.getSecurity()).thenReturn(mockSecuritySettings);
        when(mockSecuritySettings.getLdap()).thenReturn(mockLdapSettings);
        when(mockLdapSettings.getRoles()).thenReturn(mockLdapRoleResolveSettings);

        lenient().when(mockLdapRoleResolveSettings.getWrite()).thenReturn(write);
        lenient().when(mockLdapRoleResolveSettings.getCommit()).thenReturn(commit);
        lenient().when(mockLdapRoleResolveSettings.getAdmin()).thenReturn(admin);


        return mockSettings;

    }

    @Nested
    public class ResolveAccessRole {

        @Test
        public void hasRead() {
            List<SimpleGrantedAuthority> test_permission_set = Collections.singletonList(new SimpleGrantedAuthority("ROLE_DefaultAccessRole"));
            customUserAuthoritiesMapper.settings = setupRoleSettings(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );

            Collection<? extends GrantedAuthority> output = customUserAuthoritiesMapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(1);
            assertThat(output).isEqualTo(UserRoleConfiguration.READ_ROLE_PERMISSION_SET);
        }

        @Test
        public void hasWrite() {
            List<SimpleGrantedAuthority> test_permission_set = Collections.singletonList(new SimpleGrantedAuthority("ROLE_write"));
            customUserAuthoritiesMapper.settings = setupRoleSettings(
                    Collections.singletonList("write"),
                    Collections.emptyList(),
                    Collections.emptyList()
            );

            Collection<? extends GrantedAuthority> output = customUserAuthoritiesMapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(2);
            assertThat(output).isEqualTo(UserRoleConfiguration.WRITE_ROLE_PERMISSION_SET);
        }

        @Test
        public void hasCommit() {
            List<SimpleGrantedAuthority> test_permission_set = Collections.singletonList(new SimpleGrantedAuthority("ROLE_commit"));
            customUserAuthoritiesMapper.settings = setupRoleSettings(
                    Collections.emptyList(),
                    Collections.singletonList("commit"),
                    Collections.emptyList()
            );

            Collection<? extends GrantedAuthority> output = customUserAuthoritiesMapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(3);
            assertThat(output).isEqualTo(UserRoleConfiguration.COMMIT_ROLE_PERMISSION_SET);
        }

        @Test
        public void hasAdmin() {
            List<SimpleGrantedAuthority> test_permission_set = Collections.singletonList(new SimpleGrantedAuthority("ROLE_admin"));
            customUserAuthoritiesMapper.settings = setupRoleSettings(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.singletonList("admin")
            );

            Collection<? extends GrantedAuthority> output = customUserAuthoritiesMapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(4);
            assertThat(output).isEqualTo(UserRoleConfiguration.ADMIN_ROLE_PERMISSION_SET);
        }

        @Test
        public void hasMultiple() {
            List<SimpleGrantedAuthority> test_permission_set = Collections.singletonList(new SimpleGrantedAuthority("ROLE_admin"));
            customUserAuthoritiesMapper.settings = setupRoleSettings(
                    Collections.singletonList("write"),
                    Collections.singletonList("commit"),
                    Collections.singletonList("admin")
            );
            Collection<? extends GrantedAuthority> output = customUserAuthoritiesMapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(4);
            assertThat(output).isEqualTo(UserRoleConfiguration.ADMIN_ROLE_PERMISSION_SET);
        }
    }
}
