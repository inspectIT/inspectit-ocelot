package rocks.inspectit.oce.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
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
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.spring.SpringConfiguration;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Optional;
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

    @Autowired
    protected Instrumentation mockInstrumentation;

    /**
     * Allows to customize properties while the Context is open.
     * This method is based on {@link InspectitEnvironment#updatePropertySources(Consumer)},
     * which therefore also triggers {@link InspectitConfigChangedEvent}s.
     * <p>
     * Any test using this method should also hav the {@link org.springframework.test.annotation.DirtiesContext} annotation.
     *
     * @param propsCustomizer the lambda for customizing the properties.
     */
    public void updateProperties(Consumer<MockPropertySource> propsCustomizer) {
        env.updatePropertySources((propsList) -> propsCustomizer.accept(env.mockProperties));
        env.addMockAppender();
    }


    @BeforeEach
    public void clearTrackedLogs() {
        Mockito.reset(env.mockAppender);
    }

    static class TestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        /**
         * This "hack" of a nested class has to be used because there is no other way of passing the soures to customizePropertySources
         * prior to the call of the superconstructor.
         */
        List<PropertySource> testPropertySources;

        class TestInspectitEnvironment extends InspectitEnvironment {

            MockPropertySource mockProperties;

            Appender<ILoggingEvent> mockAppender = Mockito.mock(Appender.class);


            public TestInspectitEnvironment(ConfigurableApplicationContext ctx) {
                super(ctx, Optional.empty());
                when(mockAppender.getName()).thenReturn("MOCK");
                addMockAppender();
            }

            @Override
            protected void configurePropertySources(Optional<String> cmdArgs) {
                MutablePropertySources propsList = getPropertySources();
                mockProperties = new MockPropertySource();
                propsList.addFirst(mockProperties);
                testPropertySources.forEach(propsList::addLast);
                super.configurePropertySources(cmdArgs);
            }

            void addMockAppender() {
                Logger root = (Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
                if (root.getAppender("MOCK") == null) {
                    root.addAppender(mockAppender);
                }
            }
        }

        private Instrumentation initInstrumentationMock() {
            Instrumentation instr = Mockito.mock(Instrumentation.class);
            when(instr.isRetransformClassesSupported()).thenReturn(true);
            when(instr.getAllLoadedClasses()).thenReturn(new Class<?>[]{});
            return instr;
        }

        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            ConfigurableEnvironment defaultEnv = ctx.getEnvironment();

            testPropertySources = defaultEnv.getPropertySources().stream()
                    .filter(ps -> ps.getName() != StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)
                    .filter(ps -> ps.getName() != StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
                    .collect(Collectors.toList());

            new TestInspectitEnvironment(ctx);
            ctx.addBeanFactoryPostProcessor(fac -> fac.registerSingleton("instrumentation", initInstrumentationMock()));

        }
    }

    /**
     * Asserts that no log output greater or equal to the given level are produced by the given test.
     *
     * @param level the level to compare against.
     */
    public void assertNoLogsOfLevelOrGreater(Level level) {
        verify(env.mockAppender, times(0)).doAppend(argThat(
                (le) -> le.getLevel().isGreaterOrEqual(level)));
    }

    /**
     * Asserts that log output greater or equal to the given level is produced by the given test.
     *
     * @param level the level to compare against.
     */
    public void assertLogsOfLevelOrGreater(Level level) {
        verify(env.mockAppender, atLeastOnce()).doAppend(argThat(
                (le) -> le.getLevel().isGreaterOrEqual(level)));
    }


}
