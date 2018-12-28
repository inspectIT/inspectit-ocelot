package rocks.inspectit.oce.core.config;

import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenCensusConfiguration {

    @Bean
    public StatsRecorder getStatsRecorder() {
        return Stats.getStatsRecorder();
    }

    @Bean
    public ViewManager getViewManager() {
        return Stats.getViewManager();
    }
}
