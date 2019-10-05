package rocks.inspectit.ocelot.security.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Listener for events to be audit logged.
 */
@Slf4j
public class AuditEventListener {

    private static final String SYSTEM_USER = "System";

    @PrePersist
    public void beforePersist(Auditable auditable) {
        logEvent(auditable.getAuditDetail(), "has been created");
    }

    @PreUpdate
    public void beforeUpdate(Auditable auditable) {
        logEvent(auditable.getAuditDetail(), "has been updated");
    }

    @PreRemove
    public void beforeRemove(Auditable auditable) {
        logEvent(auditable.getAuditDetail(), "has been deleted");
    }

    private void logEvent(AuditDetail audit, String eventDescription) {
        String principal = getPrincipalUsername();
        log.info("{}({}) {} by {} on {}", audit.entityType.getValue(), audit.getAuditIdentifier(), eventDescription, principal, LocalDateTime
                .now());
    }

    private String getPrincipalUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.nonNull(authentication)) {
            if (authentication.getPrincipal() instanceof String) {
                return (String) authentication.getPrincipal();
            }
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return SYSTEM_USER;
    }
}
