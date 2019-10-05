package rocks.inspectit.ocelot.security.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This provides fields for auditing useful entity information. Fields can be added, as and when required.
 */
@Getter
@AllArgsConstructor
public class AuditDetail {

    /**
     * The type of entity to be audited.
     */
    ENTITY_TYPE entityType;

    /**
     * The identifier to be logged.
     */
    String auditIdentifier;

    /**
     * Specifies entities which should be audited.
     */
    @Getter
    public enum ENTITY_TYPE {
        USER("A User"), AGENT_MAPPINGS("An Agent-Mapping");

        private final String value;

        ENTITY_TYPE(String value) {
            this.value = value;
        }
    }
}
