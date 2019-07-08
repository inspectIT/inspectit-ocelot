package rocks.inspectit.ocelot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Fixes the breakign swagger ui due to our {@link YamlConfiguration}.
 * See https://stackoverflow.com/questions/38020044/spring-boot-with-custom-converters-breaks-swagger-how-to-move-it
 */
@Configuration
@EnableWebMvc
public class YamlConfigurationSupport extends WebMvcConfigurationSupport {

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
