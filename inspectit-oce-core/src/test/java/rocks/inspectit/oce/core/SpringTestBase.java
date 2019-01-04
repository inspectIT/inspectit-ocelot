package rocks.inspectit.oce.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.SpringConfiguration;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

/**
 * Base class for all tests.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringConfiguration.class, initializers = SpringTestBase.TestContextInitializer.class)
@TestPropertySource(properties = {
        "inspectit.config.file-based.watch=false"
})
public class SpringTestBase {

    @Autowired
    private TestContextInitializer.TestInspectitEnvironment env;

    private Appender<ILoggingEvent> mockAppender;

    /**
     * Allows to customize properties while the Context is open.
     * This method is based on {@link InspectitEnvironment#updatePropertySources(Consumer)},
     * which therefore also triggers {@link rocks.inspectit.oce.core.config.InspectitConfigChangedEvent}s.
     * <p>
     * Any test using this method should also hav the {@link org.springframework.test.annotation.DirtiesContext} annotation.
     *
     * @param propsCustomizer the lambda for customizing the properties.
     */
    public void updateProperties(Consumer<MockPropertySource> propsCustomizer) {
        env.updatePropertySources((propsList) -> propsCustomizer.accept(env.mockProperties));
    }

    static class TestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        /**
         * This "hack" of a nested class has to be used because there is no other way of passing the soures to customizePropertySources
         * prior to the call of the superconstructor.
         */
        List<PropertySource> testPropertySources;

        class TestInspectitEnvironment extends InspectitEnvironment {

            MockPropertySource mockProperties;


            public TestInspectitEnvironment(ConfigurableApplicationContext ctx) {
                super(ctx);
            }

            @Override
            protected void customizePropertySources(MutablePropertySources propsList) {
                mockProperties = new MockPropertySource();
                propsList.addFirst(mockProperties);
                testPropertySources.forEach(propsList::addLast);
                super.customizePropertySources(propsList);
            }
        }

        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            ConfigurableEnvironment defaultEnv = ctx.getEnvironment();

            testPropertySources = defaultEnv.getPropertySources().stream()
                    .filter(ps -> ps.getName() != StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)
                    .filter(ps -> ps.getName() != StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
                    .collect(Collectors.toList());

            new TestInspectitEnvironment(ctx);
        }
    }

    @BeforeEach
    void addMockAppender() {
        mockAppender = Mockito.mock(Appender.class);
        Logger root = (Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        reset(mockAppender);
    }

    @AfterEach
    void removeMockAppender() {
        Logger root = (Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.detachAppender(mockAppender);
    }

    /**
     * Asserts that no log output greater or equal to the given level are produced by the given test.
     *
     * @param level the level to compare against.
     */
    public void assertNoLogsOfLevelOrGreater(Level level) {
        verify(mockAppender, times(0)).doAppend(argThat(
                (le) -> le.getLevel().isGreaterOrEqual(level)));
    }


}
