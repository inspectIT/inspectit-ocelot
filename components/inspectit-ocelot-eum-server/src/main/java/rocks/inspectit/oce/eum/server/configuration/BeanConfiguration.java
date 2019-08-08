package rocks.inspectit.oce.eum.server.configuration;

import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The bean configuration for the EUM server.
 */
@Configuration
public class BeanConfiguration {

    /**
     * Instance of the OpenCensus {@link StatsRecorder} which should be used.
     */
    @Bean
    public StatsRecorder statsRecorder() {
        return Stats.getStatsRecorder();
    }

    /**
     * Instance of the OpenCensus {@link ViewManager} which should be used.
     */
    @Bean
    public ViewManager viewManager() {
        return Stats.getViewManager();
    }
}
