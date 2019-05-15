package rocks.inspectit.ocelot.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rocks.inspectit.ocelot.rest.agent.AgentBearerFilter;

/**
 * The spring configuration.
 */
@Configuration
public class SpringBootConfiguration {

    /**
     * The servlet filter used for authorization of requests against the /agent/* REST interfaces.
     */
    @Bean
    public FilterRegistrationBean agentBearerFilter() {

        FilterRegistrationBean<AgentBearerFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AgentBearerFilter());
        registration.addUrlPatterns("/agent/*");
        registration.setName("agent-bearer-filter");

        return registration;
    }

}
