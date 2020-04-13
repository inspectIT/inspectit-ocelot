package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings for the configuration of the to let a HTTP reverse proxy handling authentication.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthProxySettings {

    /**
     * If auth proxy is enabled.
     */
    private boolean enabled;

    /**
     * The request header where the username is stored.
     */
    private String principalRequestHeader;

}
