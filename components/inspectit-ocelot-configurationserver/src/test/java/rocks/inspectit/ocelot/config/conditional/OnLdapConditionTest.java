package rocks.inspectit.ocelot.config.conditional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnLdapConditionTest {

    @InjectMocks
    OnLdapCondition condition;

    @Mock
    ConditionContext context;

    @Mock
    AnnotatedTypeMetadata metadata;

    @Mock
    Environment environment;

    @Nested
    public class Matches {

        @Test
        public void ldapEnabled() {
            when(context.getEnvironment()).thenReturn(environment);
            when(environment.getProperty("inspectit-config-server.security.ldap-authentication", "false")).thenReturn("true");

            boolean result = condition.matches(context, metadata);

            assertThat(result).isTrue();
        }

        @Test
        public void ldapDisabled() {
            when(context.getEnvironment()).thenReturn(environment);
            when(environment.getProperty("inspectit-config-server.security.ldap-authentication", "false")).thenReturn("false");

            boolean result = condition.matches(context, metadata);

            assertThat(result).isFalse();
        }

        @Test
        public void ldapPropertyMissing() {
            when(context.getEnvironment()).thenReturn(environment);
            when(environment.getProperty("inspectit-config-server.security.ldap-authentication", "false")).thenReturn(null);

            boolean result = condition.matches(context, metadata);

            assertThat(result).isFalse();
        }
    }

}