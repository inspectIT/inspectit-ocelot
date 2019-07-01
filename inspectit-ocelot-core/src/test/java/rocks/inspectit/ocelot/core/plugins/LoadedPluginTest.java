package rocks.inspectit.ocelot.core.plugins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.sdk.ConfigurablePlugin;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoadedPluginTest {

    @Mock
    ConfigurablePlugin<Object> plugin;

    @Mock
    InspectitEnvironment env;

    InspectitConfig confA = new InspectitConfig();
    InspectitConfig confB = new InspectitConfig();

    @BeforeEach
    void setupMocks() {
        when(env.getCurrentConfig()).thenReturn(confA);
        confA.setServiceName("A");
        confB.setServiceName("B");
    }

    @Nested
    class UpdateConfiguration {

        @Test
        void testNullConfigUpdateCalled() {
            when(plugin.getConfigurationClass()).thenReturn(null);
            LoadedPlugin loaded = new LoadedPlugin(plugin, "test");

            loaded.updateConfiguration(env);
            loaded.updateConfiguration(env); //call twice to make sure it is called once

            when(env.getCurrentConfig()).thenReturn(confB);

            loaded.updateConfiguration(env);
            loaded.updateConfiguration(env);

            verify(plugin).update(same(confA), isNull());
            verify(plugin).update(same(confB), isNull());
            verifyNoMoreInteractions(plugin);
        }


        @SuppressWarnings("unchecked")
        @Test
        void testConfigUpdateOnInspectitConfigChange() {
            when(plugin.getConfigurationClass()).thenReturn((Class) String.class);
            when(env.loadAndValidateFromProperties(LoadedPlugin.PLUGIN_CONFIG_PREFIX + "test", String.class))
                    .thenReturn(Optional.of("foo"));
            LoadedPlugin loaded = new LoadedPlugin(plugin, "test");

            loaded.updateConfiguration(env);
            loaded.updateConfiguration(env); //call twice to make sure it is called once

            when(env.getCurrentConfig()).thenReturn(confB);

            loaded.updateConfiguration(env);
            loaded.updateConfiguration(env);

            verify(plugin).update(same(confA), eq("foo"));
            verify(plugin).update(same(confB), eq("foo"));
            verifyNoMoreInteractions(plugin);
        }


        @SuppressWarnings("unchecked")
        @Test
        void testConfigUpdateOnPluginConfigChange() {
            when(plugin.getConfigurationClass()).thenReturn((Class) String.class);
            when(env.loadAndValidateFromProperties(LoadedPlugin.PLUGIN_CONFIG_PREFIX + "test", String.class))
                    .thenReturn(Optional.of("foo"));
            LoadedPlugin loaded = new LoadedPlugin(plugin, "test");

            loaded.updateConfiguration(env);
            loaded.updateConfiguration(env); //call twice to make sure it is called once

            when(env.loadAndValidateFromProperties(LoadedPlugin.PLUGIN_CONFIG_PREFIX + "test", String.class))
                    .thenReturn(Optional.of("bar"));

            loaded.updateConfiguration(env);
            loaded.updateConfiguration(env);

            verify(plugin).update(same(confA), eq("foo"));
            verify(plugin).update(same(confA), eq("bar"));
            verifyNoMoreInteractions(plugin);
        }


        @SuppressWarnings("unchecked")
        @Test
        void testNoConfigUpdateOnInvalidConfigChange() {
            when(plugin.getConfigurationClass()).thenReturn((Class) String.class);
            when(env.loadAndValidateFromProperties(LoadedPlugin.PLUGIN_CONFIG_PREFIX + "test", String.class))
                    .thenReturn(Optional.of("foo"));
            LoadedPlugin loaded = new LoadedPlugin(plugin, "test");

            loaded.updateConfiguration(env);
            loaded.updateConfiguration(env); //call twice to make sure it is called once

            when(env.loadAndValidateFromProperties(LoadedPlugin.PLUGIN_CONFIG_PREFIX + "test", String.class))
                    .thenReturn(Optional.empty());

            loaded.updateConfiguration(env);
            loaded.updateConfiguration(env);

            verify(plugin).update(same(confA), eq("foo"));
            verifyNoMoreInteractions(plugin);
        }

    }
}
