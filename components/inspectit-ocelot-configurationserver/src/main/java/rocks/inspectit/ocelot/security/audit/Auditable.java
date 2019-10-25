package rocks.inspectit.ocelot.security.audit;

/**
 * Entities implementing this will be qualified for getting audit logged.
 */
public interface Auditable {

    /**
     * This provides details for auditing.
     *
     * @return audit information for entity to be audited.
     */
    AuditDetail getAuditDetail();
}
