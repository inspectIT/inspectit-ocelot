package rocks.inspectit.ocelot.config;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebMvc
public class YamlConfiguration implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(createYamlConverter());
    }

    private AbstractJackson2HttpMessageConverter createYamlConverter() {
        return new AbstractJackson2HttpMessageConverter(new YAMLMapper(), MediaType.parseMediaType("application/x-yaml")) {
        };
    }
}
