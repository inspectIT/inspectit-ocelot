package rocks.inspectit.ocelot.core.plugins;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.sdk.ConfigurablePlugin;
import rocks.inspectit.ocelot.sdk.OcelotPlugin;

import java.util.Arrays;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PluginLoaderTest {

    @Mock
    InspectitEnvironment env;

    @InjectMocks
    private PluginLoader loader = new PluginLoader();

    @Nested
    class UpdatePlugins {

        @Test
        void ensureExceptionsCatched() {
            LoadedPlugin myPlugin = Mockito.mock(LoadedPlugin.class);
            doThrow(RuntimeException.class).when(myPlugin).updateConfiguration(any());
            loader.plugins = Arrays.asList(myPlugin);

            loader.updatePlugins();

            verify(myPlugin).updateConfiguration(same(env));
        }
    }

    @Nested
    class Destroy {

        @Test
        void ensureExceptionsCaught() {
            LoadedPlugin myPlugin = Mockito.mock(LoadedPlugin.class);
            doThrow(RuntimeException.class).when(myPlugin).destroy();
            loader.plugins = Arrays.asList(myPlugin);

            loader.destroy();

            verify(myPlugin).destroy();
        }
    }


    @Nested
    class InitializePlugin {

        @Test
        void noExceptionOnPluginWithoutDefaultConstructor() {
            Properties props = new Properties();

            loader.initializePlugin(PluginWithoutPublicDefaultConstructor.class, props);

            assertThat(loader.plugins).isEmpty();
        }


        @Test
        void noExceptionOnPluginWithoutInterfaceImplementation() {
            Properties props = new Properties();

            loader.initializePlugin(PluginWithoutInterfaceImplementation.class, props);

            assertThat(loader.plugins).isEmpty();
        }


        @Test
        void verifyDefaultConfigLoaded() {
            Properties props = new Properties();

            loader.initializePlugin(PluginWithDefaultConfig.class, props);

            assertThat(loader.plugins).isNotEmpty();
            assertThat(props).hasSize(1);
            assertThat(props).containsEntry("inspectit.plugins.my-plugin.foo", "bar");
        }


        @Test
        void verifyThrownExceptionsCaught() {
            Properties props = new Properties();

            loader.initializePlugin(PluginThrowingException.class, props);

            assertThat(loader.plugins).isEmpty();
        }
    }


    @OcelotPlugin("pla")
    public static class PluginWithoutPublicDefaultConstructor implements ConfigurablePlugin {

        PluginWithoutPublicDefaultConstructor() {
        }

        @Override
        public void update(InspectitConfig inspectitConfig, Object pluginConfig) {

        }

        @Override
        public Class getConfigurationClass() {
            return null;
        }
    }


    @OcelotPlugin("plb")
    public static class PluginWithoutInterfaceImplementation {

    }


    @OcelotPlugin(value = "pla", defaultConfig = "my-default.yml")
    public static class PluginWithDefaultConfig implements ConfigurablePlugin {


        @Override
        public void update(InspectitConfig inspectitConfig, Object pluginConfig) {

        }

        @Override
        public Class getConfigurationClass() {
            return null;
        }
    }


    @OcelotPlugin(value = "pla", defaultConfig = "non-existing.yml")
    public static class PluginThrowingException implements ConfigurablePlugin {

        public PluginThrowingException() {
            throw new RuntimeException();
        }

        @Override
        public void update(InspectitConfig inspectitConfig, Object pluginConfig) {

        }

        @Override
        public Class getConfigurationClass() {
            return null;
        }
    }

}
