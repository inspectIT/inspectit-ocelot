package rocks.inspectit.ocelot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.SecuritySettings;
import rocks.inspectit.ocelot.filters.WebhookAccessFilter;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
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

    /**
     * Creates the {@link WebhookAccessFilter} which handles the token authentication against the webhook endpoints.
     *
     * @param settings The currently used server settings.
     *
     * @return the {@link WebhookAccessFilter}
     */
    @Bean
    @ConditionalOnExpression("'${inspectit-config-server.security.webhook-tokens}' != null")
    public FilterRegistrationBean<WebhookAccessFilter> webhookAccessFilter(InspectitServerSettings settings) {
        List<String> validTokens;
        if (settings.getSecurity() != null) {
            validTokens = settings.getSecurity().getWebhookTokens();
        } else {
            validTokens = Collections.emptyList();
        }

        if (CollectionUtils.isEmpty(validTokens)) {
            log.warn("Requests against webhook endpoints will be rejected because no access-tokens have been specified. See the documentation on how to specify access-tokens.");
        }

        WebhookAccessFilter accessFilter = new WebhookAccessFilter(validTokens);

        FilterRegistrationBean<WebhookAccessFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(accessFilter);
        registrationBean.addUrlPatterns("/api/v1/hook/*");

        return registrationBean;
    }
}
