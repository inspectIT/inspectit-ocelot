package rocks.inspectit.ocelot.core;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;

import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class MethodTest {



    //// MDC
    public static void put(String key, String value) {
        System.out.println("Yo: " + key + " - " + value);
    }

    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, InstantiationException {
        System.out.println("Test");

//        Method put = MethodTest.class.getMethod("put", String.class, String.class);
//        put.invoke(null, "", ""); // verboten

        Class<? extends MdcProxy> mdcProxyClass =
                new ByteBuddy()
                .subclass(MdcProxy.class)
//                .method(named("myPut")).intercept(MethodDelegation.to(MethodTest.class))
                .method(named("myPut")).intercept(MethodCall.invoke(MethodTest.class.getMethod("put", String.class, String.class)).withAllArguments())

//                .defineMethod("doSout", void.class, Ownership.STATIC, Visibility.PUBLIC)
//                .withParameters(Exception.class)
//                .intercept(MethodCall.invoke(Crashalytics.getMethod("logException"))
//                        .withArgument(0));
//                .method(named("second")).intercept(MethodCall.invoke(MethodTest.class.getMethod("sout", String.class)).withArgument(0))

                .make()
                .load(ClassLoader.getSystemClassLoader())
                .getLoaded();

        MdcProxy mdcProxy = mdcProxyClass.newInstance();
        mdcProxy.myPut("hey", "Patrick");
    }

    public interface MdcProxy {
        void myPut(String key, String value);
    }
}
