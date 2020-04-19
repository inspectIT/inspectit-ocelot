package rocks.inspectit.ocelot.rest.file;

import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.config.loaders.ConfigFileLoader;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.security.userdetails.CustomLdapUserDetailsService;

import java.io.IOException;
import java.util.Map;

@RestController
public class DefaultConfigController extends AbstractBaseController {

    @Secured(
            {
                    CustomLdapUserDetailsService.READ_ACCESS_ROLE,
                    CustomLdapUserDetailsService.WRITE_ACCESS_ROLE,
                    CustomLdapUserDetailsService.COMMIT_ACCESS_ROLE,
                    CustomLdapUserDetailsService.ADMIN_ACCESS_ROLE
            }
    )
    @GetMapping(value = "defaultconfig")
    public Map<String, String> getDefaultConfigContent() throws IOException {
        return ConfigFileLoader.getDefaultConfigFiles();
    }

    @Secured(
            {
                    CustomLdapUserDetailsService.READ_ACCESS_ROLE,
                    CustomLdapUserDetailsService.WRITE_ACCESS_ROLE,
                    CustomLdapUserDetailsService.COMMIT_ACCESS_ROLE,
                    CustomLdapUserDetailsService.ADMIN_ACCESS_ROLE
            }
    )
    @GetMapping(value = "fallbackconfig")
    public Map<String, String> getFallBackConfigContent() throws IOException {
        return ConfigFileLoader.getFallbackConfigFiles();
    }

}
