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

import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

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

    @Nested
    class GetAnnotatedClasses {

        @Test
        void hasAnnotations() throws Exception {
            JarFile mockJar = mock(JarFile.class);
            Enumeration mockEnumeration = mock(Enumeration.class);
            when(mockJar.entries()).thenReturn(mockEnumeration);
            when(mockEnumeration.hasMoreElements()).thenReturn(true, false);
            JarEntry entry = mock(JarEntry.class);
            when(entry.isDirectory()).thenReturn(false);
            when(entry.getName()).thenReturn("HasAnnotation.class");
            when(mockEnumeration.nextElement()).thenReturn(entry);
            when(mockJar.getInputStream(any())).thenReturn(loadTestClass("HasAnnotation.class"));

            List<String> output = loader.getAnnotatedClasses(mockJar);

            assertThat(output).hasSize(1);
            assertThat(output).contains("HasAnnotation");
        }

        @Test
        void noAnnotations() throws Exception {
            JarFile mockJar = mock(JarFile.class);
            Enumeration mockEnumeration = mock(Enumeration.class);
            when(mockJar.entries()).thenReturn(mockEnumeration);
            when(mockEnumeration.hasMoreElements()).thenReturn(true, false);
            JarEntry entry = mock(JarEntry.class);
            when(entry.isDirectory()).thenReturn(false);
            when(entry.getName()).thenReturn("NoAnnotation.class");
            when(mockEnumeration.nextElement()).thenReturn(entry);
            when(mockJar.getInputStream(any())).thenReturn(loadTestClass("NoAnnotation.class"));

            List<String> output = loader.getAnnotatedClasses(mockJar);

            assertThat(output).hasSize(0);
        }

        @Test
        void annotationOnMethod() throws Exception {
            JarFile mockJar = mock(JarFile.class);
            Enumeration mockEnumeration = mock(Enumeration.class);
            when(mockJar.entries()).thenReturn(mockEnumeration);
            when(mockEnumeration.hasMoreElements()).thenReturn(true, false);
            JarEntry entry = mock(JarEntry.class);
            when(entry.isDirectory()).thenReturn(false);
            when(entry.getName()).thenReturn("AnnotationOnMethod.class");
            when(mockEnumeration.nextElement()).thenReturn(entry);
            when(mockJar.getInputStream(any())).thenReturn(loadTestClass("AnnotationOnMethod.class"));

            List<String> output = loader.getAnnotatedClasses(mockJar);

            assertThat(output).hasSize(0);
        }
    }

    private InputStream loadTestClass(String testClass) {
        StringBuilder pathBuilder = new StringBuilder("/rocks/inspectit/ocelot/core/plugins/PluginLoaderTest$");
        pathBuilder.append(testClass);
        return getClass().getResourceAsStream(pathBuilder.toString());
    }

    @OcelotPlugin("")
    public class HasAnnotation {

    }

    public class NoAnnotation {

    }

    public class AnnotationOnMethod {

        @OcelotPlugin("")
        public String test() {
            return "test!";
        }
    }

    @OcelotPlugin("pla")
    public static class PluginWithoutPublicDefaultConstructor implements ConfigurablePlugin {

        PluginWithoutPublicDefaultConstructor() {
        }

        @Override
        public void start(InspectitConfig inspectitConfig, Object pluginConfig) {

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
        public void start(InspectitConfig inspectitConfig, Object pluginConfig) {

        }

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
        public void start(InspectitConfig inspectitConfig, Object pluginConfig) {

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
