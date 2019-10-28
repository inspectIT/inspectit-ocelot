package rocks.inspectit.ocelot.security.audit;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * This provides fields for auditing useful entity information. Fields can be added, as and when required.
 */
@AllArgsConstructor
@Value
public class AuditDetail {

    /**
     * The type of entity to be audited.
     */
    String entityType;

    /**
     * The identifier to be logged.
     */
    String entityIdentifier;
}
