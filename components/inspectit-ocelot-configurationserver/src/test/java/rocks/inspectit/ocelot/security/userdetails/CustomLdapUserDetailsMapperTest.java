package rocks.inspectit.ocelot.security.userdetails;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import rocks.inspectit.ocelot.config.model.LdapRoleResolveSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomLdapUserDetailsMapperTest {

    private static CustomLdapUserDetailsMapper mapper;

    @BeforeAll
    public static void setup() {
        LdapSettings settings = LdapSettings.builder()
                .url("url")
                .userSearchBase("")
                .userSearchFilter("")
                .groupSearchBase("")
                .groupSearchFilter("")
                .roles(LdapRoleResolveSettings.builder()
                        .read(Collections.singletonList("read"))
                        .write(Collections.singletonList("write"))
                        .promote(Collections.singletonList("promote"))
                        .admin(Collections.singletonList("admin"))
                        .build())
                .build();

        mapper = new CustomLdapUserDetailsMapper(settings);
    }

    @Nested
    public class MapAuthorities {

        @Test
        public void hasNone() {
            List<SimpleGrantedAuthority> test_permission_set = Collections.singletonList(new SimpleGrantedAuthority("ROLE_notMatching"));

            Collection<? extends GrantedAuthority> output = mapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(0);
        }

        @Test
        public void hasRead() {
            List<SimpleGrantedAuthority> test_permission_set = Collections.singletonList(new SimpleGrantedAuthority("ROLE_read"));

            Collection<? extends GrantedAuthority> output = mapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(1);
            assertThat(output).isEqualTo(UserRoleConfiguration.READ_ROLE_PERMISSION_SET);
        }

        @Test
        public void hasWrite() {
            List<SimpleGrantedAuthority> test_permission_set = Collections.singletonList(new SimpleGrantedAuthority("ROLE_write"));

            Collection<? extends GrantedAuthority> output = mapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(2);
            assertThat(output).isEqualTo(UserRoleConfiguration.WRITE_ROLE_PERMISSION_SET);
        }

        @Test
        public void hasCommit() {
            List<SimpleGrantedAuthority> test_permission_set = Collections.singletonList(new SimpleGrantedAuthority("ROLE_promote"));

            Collection<? extends GrantedAuthority> output = mapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(3);
            assertThat(output).isEqualTo(UserRoleConfiguration.PROMOTE_ROLE_PERMISSION_SET);
        }

        @Test
        public void hasAdmin() {
            List<SimpleGrantedAuthority> test_permission_set = Collections.singletonList(new SimpleGrantedAuthority("ROLE_admin"));

            Collection<? extends GrantedAuthority> output = mapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(4);
            assertThat(output).isEqualTo(UserRoleConfiguration.ADMIN_ROLE_PERMISSION_SET);
        }

        @Test
        public void hasMultiple() {
            List<SimpleGrantedAuthority> test_permission_set = Arrays.asList(new SimpleGrantedAuthority("ROLE_admin"), new SimpleGrantedAuthority("ROLE_read"));

            Collection<? extends GrantedAuthority> output = mapper.mapAuthorities(test_permission_set);

            assertThat(output).hasSize(4);
            assertThat(output).isEqualTo(UserRoleConfiguration.ADMIN_ROLE_PERMISSION_SET);
        }
    }

}