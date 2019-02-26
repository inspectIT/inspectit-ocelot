package rocks.inspectit.oce.core.instrumentation.config.dummy;

public class LambdaTestProvider {

    public static Class<?> getLambdaWithDefaultMethod() {
        FunctionalInterfaceWithDefaultMethod a = () -> "str";
        return a.getClass();
    }

    public static Class<?> getLambdaWithInheritedDefaultMethod() {
        SubInterfaceWithDefaultMethod a = () -> "str";
        return a.getClass();
    }

    public static Class<?> getAnonymousClassWithDefaultMethod() {
        FunctionalInterfaceWithDefaultMethod a = new FunctionalInterfaceWithDefaultMethod() {
            @Override
            public String b() {
                return null;
            }
        };
        return a.getClass();
    }

    public static Class<?> getAnonymousClassWithInheritedDefaultMethod() {
        SubInterfaceWithDefaultMethod a = new SubInterfaceWithDefaultMethod() {
            @Override
            public String b() {
                return null;
            }
        };
        return a.getClass();
    }

}
