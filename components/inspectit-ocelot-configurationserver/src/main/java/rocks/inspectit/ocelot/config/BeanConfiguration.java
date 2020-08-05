package rocks.inspectit.ocelot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableScheduling
public class BeanConfiguration {

    /**
     * Executor service to use for asynchronous tasks.
     *
     * @param config the applications configuration, gets autowired
     *
     * @return the executor service
     */
    @Bean
    public ScheduledExecutorService fixedThreadPool(InspectitServerSettings config) {
        return Executors.newScheduledThreadPool(config.getThreadPoolSize());
    }
}
