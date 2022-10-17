package rocks.inspectit.ocelot.core.instrumentation;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import rocks.inspectit.ocelot.core.AgentImpl;

/**
 * Container for {@link TypeDescription}s and {@link  ClassLoader} pairs.
 * The purpose of this container is to have common type for already loaded classes and for classes loading for the first time
 * when no class object is available.
 */
@EqualsAndHashCode
public class TypeDescriptionWithClassLoader {

    @Getter
    private final TypeDescription type;

    @Getter
    private final ClassLoader loader;

    private TypeDescriptionWithClassLoader(TypeDescription type, ClassLoader loader) {
        this.type = type;
        this.loader = loader;
    }

    public String getName() {
        return type.getName();
    }

    /**
     * Creates a new {@link TypeDescriptionWithClassLoader} from a loaded class.
     *
     * @param clazz The class object
     *
     * @return new {@link TypeDescriptionWithClassLoader}
     */
    public static TypeDescriptionWithClassLoader of(Class<?> clazz) {
        return new TypeDescriptionWithClassLoader(TypeDescription.ForLoadedType.of(clazz), clazz.getClassLoader());
    }

    /**
     * Creates a new {@link TypeDescriptionWithClassLoader} for a not yet loaded class by its class name and the corresponding ClassLoader.
     *
     * @param className Full qualified name of the class
     * @param loader    The corresponding {@link ClassLoader}
     *
     * @return new {@link TypeDescriptionWithClassLoader}
     */
    public static TypeDescriptionWithClassLoader of(String className, ClassLoader loader) {
        if (loader == AgentImpl.AGENT_CLASS_LOADER) {
            // use the already loaded TypePool if loader is our InspectitClassLoader
            return new TypeDescriptionWithClassLoader(AgentImpl.AGENT_CLASS_LOADER_TYPE_POOL.describe(className)
                    .resolve(), loader);
        }
        return new TypeDescriptionWithClassLoader(TypePool.Default.of(loader).describe(className).resolve(), loader);
    }
}
