package rocks.inspectit.oce.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ComponentScan("rocks.inspectit")
public class SpringConfiguration {

    @Bean
    public ScheduledExecutorService getScheduledExecutorService(@Value("${inspectit.thread-pool-size}") int poolSize) {
        return Executors.newScheduledThreadPool(poolSize, (runnable) -> {
            Thread t = Executors.defaultThreadFactory().newThread(runnable);
            t.setDaemon(true);
            return t;
        });
    }

}
