package rocks.inspectit.ocelot.security.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

/**
 * Listener for events to be audit logged.
 */
@Slf4j
public class AuditEventListener {

    @Autowired
    private EntityAuditLogger auditLogger;

    @PostPersist
    public void afterPersist(Auditable entity) {
        auditLogger.logEntityCreation(entity);
    }

    @PostUpdate
    public void afterUpdate(Auditable entity) {
        auditLogger.logEntityUpdate(entity);
    }

    @PostRemove
    public void afterRemove(Auditable entity) {
        auditLogger.logEntityDeletion(entity);
    }
}
