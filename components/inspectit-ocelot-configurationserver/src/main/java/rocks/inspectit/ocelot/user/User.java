package rocks.inspectit.ocelot.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.security.audit.AuditDetail;
import rocks.inspectit.ocelot.security.audit.AuditEventListener;
import rocks.inspectit.ocelot.security.audit.Auditable;

import javax.persistence.*;

/**
 * Data model for a user account, stored in the embedded database.
 */
@Data
@Builder(toBuilder = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = @Index(columnList = "username", unique = true))
@EntityListeners(AuditEventListener.class)
public class User implements Auditable {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * Name of the user, should be always lowercase.
     */
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * The hashed password.
     */
    @JsonIgnore
    @Column(nullable = false)
    private String passwordHash;

    /**
     * The raw password, never persisted.
     */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * Specifies whether this user has been automatically added due to a LDAP authentication.
     */
    @Column(nullable = false)
    private boolean isLdapUser;

    @Override
    @JsonIgnore
    public AuditDetail getAuditDetail() {
        String identifier = "Username:" + getUsername();
        return new AuditDetail("User", identifier);
    }
}
