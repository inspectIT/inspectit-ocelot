package rocks.inspectit.ocelot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the files for the web frontend on the /ui/ endpoint.
 */
@Configuration
public class FrontendConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/ui/**")
                .addResourceLocations("classpath:/static/ui/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/ui", "/ui/")
                .setKeepQueryParams(true)
                .setStatusCode(HttpStatus.PERMANENT_REDIRECT);
        registry.addRedirectViewController("/", "/ui/")
                .setKeepQueryParams(true)
                .setStatusCode(HttpStatus.PERMANENT_REDIRECT);
    }
}
