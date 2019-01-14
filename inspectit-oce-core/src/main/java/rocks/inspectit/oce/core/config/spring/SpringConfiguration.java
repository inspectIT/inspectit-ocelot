package rocks.inspectit.oce.core.config.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@ComponentScan("rocks.inspectit")
public class SpringConfiguration {

    @Bean
    public ScheduledExecutorService getScheduledExecutorService(@Value("#{#inspectit.threadPoolSize}") int poolSize) {
        AtomicInteger threadCount = new AtomicInteger();
        return Executors.newScheduledThreadPool(poolSize, (runnable) -> {
            Thread t = Executors.defaultThreadFactory().newThread(runnable);
            t.setDaemon(true);
            t.setName("inspectit-thread-" + threadCount.getAndIncrement());
            return t;
        });
    }

}
