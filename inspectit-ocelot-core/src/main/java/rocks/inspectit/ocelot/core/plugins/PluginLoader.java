package rocks.inspectit.ocelot.core.plugins;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.config.PropertySourcesChangedEvent;
import rocks.inspectit.ocelot.core.config.util.PropertyUtils;
import rocks.inspectit.ocelot.sdk.ConfigurablePlugin;
import rocks.inspectit.ocelot.sdk.OcelotPlugin;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * This component scans the path specified by inspectit.plugins.path for Jar-files.
 * These files are in turn scanned for classes having the {@link OcelotPlugin} annotation, which are then loaded.
 * This class also keeps
 * <p>
 * This component only performs the scanning at startup time, meaning
 * that dynamically adding or removing plugins is currently not supported.
 */
@Component
@Slf4j
public class PluginLoader {

    /**
     * We add the combination of all default settings of loaded plugins as an additional property source to {@link InspectitEnvironment}.
     * This is the name of this property source.
     */
    private static final String PROPERTY_SOURCE_NAME = "Plugin Default Configurations";


    @Autowired
    private InspectitEnvironment env;

    /**
     * The list of loaded plugins.
     */
    @VisibleForTesting
    List<LoadedPlugin> plugins = new ArrayList<>();

    @PostConstruct
    private void scanAndLoadPlugins() {
        String directory = env.getCurrentConfig().getPlugins().getPath();
        if (!StringUtils.isEmpty(directory)) {
            log.info("Scanning '{}' for plugins", directory);

            Properties defaultConfigurations = new Properties();

            try {
                Collection<File> pluginsJars = FileUtils.listFiles(new File(directory), new String[]{"jar"}, true);
                for (File jar : pluginsJars) {
                    try {
                        loadPluginJar(jar, defaultConfigurations);
                    } catch (Throwable t) {
                        log.error("Error loading plugins from jar '{}'", jar.toString(), t);
                    }
                }
            } catch (Exception e) {
                log.error("Error scanning for plugins", e);
            }
            env.updatePropertySources(ps ->
                    ps.addLast(new PropertiesPropertySource(PROPERTY_SOURCE_NAME, defaultConfigurations))
            );
            updatePlugins();
        }
    }

    @PreDestroy
    void destroy() {
        for (LoadedPlugin plugin : plugins) {
            try {
                plugin.destroy();
            } catch (Throwable t) {
                log.error("Destroying plugin {} threw Exception!", plugin.getName(), t);
            }
        }
    }

    @EventListener(PropertySourcesChangedEvent.class)
    @VisibleForTesting
    void updatePlugins() {
        for (LoadedPlugin plugin : plugins) {
            try {
                plugin.updateConfiguration(env);
            } catch (Throwable t) {
                log.error("updating plugin {} threw Exception!", plugin.getName(), t);
            }
        }
    }


    private void loadPluginJar(File jar, Properties defaultConfigurations) throws MalformedURLException {
        ClassLoader pluginLoader = new PluginClassLoader(jar.toURI().toURL());
        //TODO: replace Reflections library with a faster solution which simply scans the given Jar for the annotation
        //this can be done by enuemrating all .class files using JarFile and then loading the classes using Class.forName()
        //alternatively we can use ASM to avoid loading the class at all
        Set<Class<?>> list = new Reflections(pluginLoader)
                .getTypesAnnotatedWith(OcelotPlugin.class);
        list.stream()
                .filter(cl -> cl.getClassLoader() == pluginLoader)
                .forEach(cl -> initializePlugin(cl, defaultConfigurations));
    }

    /**
     * Instantiates a plugin from the given class.
     *
     * @param pluginClass           the plugins class
     * @param defaultConfigurations the output properties object to which default configurations of the given plugin are loaded.
     */
    @VisibleForTesting
    void initializePlugin(Class<?> pluginClass, Properties defaultConfigurations) {
        try {
            if (ConfigurablePlugin.class.isAssignableFrom(pluginClass)) {

                OcelotPlugin pluginInfo = pluginClass.getAnnotation(OcelotPlugin.class);

                try {
                    ConfigurablePlugin<?> plugin = (ConfigurablePlugin<?>) pluginClass.getConstructor().newInstance();
                    plugins.add(new LoadedPlugin(plugin, pluginInfo.value()));

                    String defaultConfigYml = pluginInfo.defaultConfig();
                    if (!StringUtils.isEmpty(defaultConfigYml)) {
                        try {
                            Properties result = PropertyUtils.readYamlFiles(new ClassPathResource(defaultConfigYml, pluginClass));
                            defaultConfigurations.putAll(result);
                        } catch (Exception e) {
                            log.error("Error loading default config file {} of plugin {}", defaultConfigYml, pluginClass.getName(), e);
                        }
                    }
                } catch (NoSuchMethodException e) {
                    log.error("The plugin {} does not have a public default constructor!", pluginClass.getName());
                }

            } else {
                log.error("The plugin {} does not implement {}!", pluginClass.getName(), ConfigurablePlugin.class.getName());
            }
        } catch (Throwable t) {
            log.error("Error initializing plugin {]", pluginClass.getName());
        }
    }


    /**
     * Simple classloader which just is marked with the {@link DoNotInstrumentMarker}.
     */
    private static class PluginClassLoader extends URLClassLoader implements DoNotInstrumentMarker {

        public PluginClassLoader(URL jarFile) {
            super(new URL[]{jarFile}, PluginLoader.class.getClassLoader());
        }
    }
}
