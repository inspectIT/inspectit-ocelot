package rocks.inspectit.ocelot.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class User {

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
    private String password;
}
