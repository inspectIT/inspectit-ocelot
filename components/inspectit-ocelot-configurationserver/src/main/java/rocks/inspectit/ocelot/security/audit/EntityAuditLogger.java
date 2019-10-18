package rocks.inspectit.ocelot.security.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class EntityAuditLogger {

    private static final String DEFAULT_USER = "anonymousUser";

    public void logEntityCreation(Auditable entity) {
        logEvent(entity.getAuditDetail(), "created");
    }

    public void logEntityDeletion(Auditable entity) {
        logEvent(entity.getAuditDetail(), "deleted");
    }

    public void logEntityUpdate(Auditable entity) {
        logEvent(entity.getAuditDetail(), "updated");
    }

    private void logEvent(AuditDetail detail, String eventDescription) {
        String principal = getPrincipalUsername();
        log.info("{}({}) {} by {}", detail.getEntityType(), detail.getEntityIdentifier(), eventDescription, principal);
    }

    private String getPrincipalUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.nonNull(authentication)) {
            if (authentication.getPrincipal() instanceof String) {
                return (String) authentication.getPrincipal();
            }
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return DEFAULT_USER;
    }
}
