package rocks.inspectit.ocelot.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for enabling and providing swagger.
 */
@Configuration
public class SwaggerConfiguration implements WebMvcConfigurer {

    @Bean
    public OpenAPI swaggerApi() {
        return  new OpenAPI()
                .info(new Info().title("Configuration Server API")
                        .description("inspectIT Ocelot Configuration Server")
                        // we explicitly do not give a version. We do not have a version REST-API at the moment
                        .license(new License().name("Apache 2.0").url("https://inspectit.rocks")))
                .externalDocs(new ExternalDocumentation()
                        .description("Configuration Server Documentation")
                        .url("https://inspectit.github.io/inspectit-ocelot/docs/config-server/overview"));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry
                .addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
