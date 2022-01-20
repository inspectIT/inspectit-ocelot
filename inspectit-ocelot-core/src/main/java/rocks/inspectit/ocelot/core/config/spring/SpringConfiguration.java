package rocks.inspectit.ocelot.core.config.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import rocks.inspectit.ocelot.core.AgentImpl;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URLClassLoader;
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

    /**
     * @return application listener for closing the inspectIT class loader when the context is getting destroyed.
     */
    @Bean
    public ApplicationListener<ContextClosedEvent> getContextClosedListener() {
        return event -> {
            try {
                log.info("Closing inspectIT class loader.");
                ((URLClassLoader) AgentImpl.INSPECTIT_CLASS_LOADER).close();
            } catch (IOException e) {
                log.error("Failed closing inspectIT class loader.", e);
            }
        };
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
