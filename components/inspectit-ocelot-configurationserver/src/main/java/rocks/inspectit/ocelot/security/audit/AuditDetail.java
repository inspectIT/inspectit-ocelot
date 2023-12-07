package rocks.inspectit.ocelot.security.audit;

/**
 * This provides fields for auditing useful entity information. Fields can be added, as and when required.
 *
 * @param entityType       The type of entity to be audited.
 * @param entityIdentifier The identifier to be logged.
 */
public record AuditDetail(String entityType, String entityIdentifier) {

}
