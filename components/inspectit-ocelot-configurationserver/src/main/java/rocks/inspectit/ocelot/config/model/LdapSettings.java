package rocks.inspectit.ocelot.config.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LdapSettings {

    private String adminGroup;

    private String url;

    private String baseDn;

    private String managerDn;

    private String managerPassword;

    private String userSearchBase;

    private String userSearchFilter;

    private String groupSearchBase;

    private String groupSearchFilter;

}
