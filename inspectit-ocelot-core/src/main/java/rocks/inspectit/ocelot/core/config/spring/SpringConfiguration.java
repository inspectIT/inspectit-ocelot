package rocks.inspectit.ocelot.core.config.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@ComponentScan("rocks.inspectit")
@Slf4j
public class SpringConfiguration {

    private ScheduledExecutorService activeExecutor = null;

    @Bean
    public ScheduledExecutorService getScheduledExecutorService(@Value("#{#inspectit.threadPoolSize}") int poolSize) {
        AtomicInteger threadCount = new AtomicInteger();
        activeExecutor = Executors.newScheduledThreadPool(poolSize, (runnable) -> {
            Thread t = Executors.defaultThreadFactory().newThread(runnable);
            t.setDaemon(true);
            t.setName("inspectit-thread-" + threadCount.getAndIncrement());
            return t;
        });
        return activeExecutor;
    }

    @PreDestroy
    void destroy() {
        if (activeExecutor != null) {
            activeExecutor.shutdownNow();
            try {
                activeExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error("Error waiting for executor shutdown", e);
            }
        }
    }

}
