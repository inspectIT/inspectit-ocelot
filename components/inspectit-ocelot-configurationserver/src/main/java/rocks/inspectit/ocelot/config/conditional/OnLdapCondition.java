package rocks.inspectit.ocelot.config.conditional;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition which is `true` if the LDAP user authentication has been enabled.
 */
public class OnLdapCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String boolString = context.getEnvironment().getProperty("inspectit-config-server.security.ldap-authentication", "false");
        return Boolean.parseBoolean(boolString);
    }
}
