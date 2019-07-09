package rocks.inspectit.ocelot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class BeanConfiguration {

    /**
     * Executor service to sue for asynchronous tasks.
     *
     * @param config the applications configuration, gets autowired
     * @return the executor service
     */
    @Bean
    public ScheduledExecutorService fixedThreadPool(InspectitServerSettings config) {
        return Executors.newScheduledThreadPool(config.getThreadPoolSize());
    }
}
