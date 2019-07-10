package rocks.inspectit.ocelot.user;

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
    Long id;

    /**
     * Name of the user, should be always lowercase.
     */
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * The hashed password.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Column(nullable = false)
    private String password;
}
