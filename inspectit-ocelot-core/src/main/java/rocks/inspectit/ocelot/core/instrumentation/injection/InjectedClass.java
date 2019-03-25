package rocks.inspectit.ocelot.core.instrumentation.injection;

import java.lang.ref.WeakReference;
import java.util.Optional;

public class InjectedClass<T> {

    private final WeakReference<Class<? extends T>> injectedClass;

    public InjectedClass(Class<? extends T> injectedClass) {
        this.injectedClass = new WeakReference<>(injectedClass);
    }

    public Optional<Class<? extends T>> getInjectedClassObject() {
        return Optional.ofNullable(injectedClass.get());
    }
}
