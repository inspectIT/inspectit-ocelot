package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters.MdcTest;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class LogTest {

    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException {
        Method targetMethod = MdcTest.class.getMethod("getTest", String.class, Object.class);

        System.out.println(targetMethod.getReturnType());
        System.out.println(Arrays.deepToString(targetMethod.getParameterTypes()));

        MethodHandles.Lookup lookup = MethodHandles.lookup();
//        MethodType type = MethodType.methodType(void.class, String.class, String.class);
        MethodType type = MethodType.methodType(targetMethod.getReturnType(), targetMethod.getParameterTypes());

        MethodHandle lookupStatic ;// = lookup.findStatic(MdcTest.class, "getTest", MethodType.methodType(Object.class, String.class));
        lookupStatic = lookup.unreflect(targetMethod);

        try {
            BiConsumer function = (BiConsumer) LambdaMetafactory.metafactory(lookup, "accept",
                    MethodType.methodType(BiConsumer.class), type.erase(), lookupStatic, type)
                    .getTarget()
                    .invoke();

//            System.setSecurityManager(new SecurityManager() {
//                @Override
//                public void checkPackageAccess(String pkg){
//                    // don't allow the use of the reflection package
//                    if(pkg.equals("java.lang.reflect")){
//                        throw new SecurityException("Reflection is not allowed!");
//                    }
//                }
//            });

            function.accept("Hans", "Jo");
//            System.out.println("Fnct: "+ result);

            Object test = targetMethod.invoke(null, "test");
            System.out.println("Refelction: "+test);
        } catch (Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }



}
