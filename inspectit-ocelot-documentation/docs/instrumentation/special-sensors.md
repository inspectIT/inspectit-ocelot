---
id: special-sensors
title: Special Sensors
---

A _Special Sensor_ is a sensor that does not collect data, but which is needed to enable certain functionality.
Each of them can be deactivated, but this is not recommended because certain features require them as a prerequisite.
For example, tracing across multiple threads is made possible by specific special sensors.
Should these sensors be deactivated, this feature would no longer be automatically available.
By default, all special sensors are enabled.

>The special sensors can be disabled using the following configuration:
>```yaml
>inspectit:
>  instrumentation:
>    special:
>      # See the default configuration for all existing special sensor keys
>      SPECIAL_SENSOR_KEY: false
>```

## JVM Local Context Propagation Sensors

The context propagation sensors are necessary to enable tracing and [data propagation](instrumentation/rules.md#data-propagation) between multiple threads by passing the current context.
By doing this, tags and scopes can be correlated to another in case of switching threads.

The following context propagation sensors are provided out-of-the-box:

* **Thread-Start Context Propagation Sensor:**
   Can be enabled or disabled via `inspectit.instrumentation.special.thread-start-context-propagation`.
   This sensor enables passing the current context via simple `java.lang.Thread` instances.
   Note that the context is only passed when calling the thread's `start` method and not when calling the `run` method due to the fact that we do not switch threads in this case.

* **Executor Context Propagation Sensor:**
   Can be enabled or disabled via `inspectit.instrumentation.special.executor-context-propagation`.
   This sensor enables passing the current context via implementations of the `java.util.concurrent.Executor` interface.
   The context is attached to the `java.lang.Runnable` used to invoke the Executor's `execute` method.

* **Scheduled Executor Context Propagation Sensor:**
   Can be enabled or disabled via `inspectit.instrumentation.special.scheduled-executor-context-propagation`.
   This sensor enables passing the current context via implementations of the `java.util.concurrent.ScheduledExecutorService` interface.
   The context is attached to the `java.lang.Runnable` or `java.util.concurrent.Callable` used to invoke the Executor's `schedule`, `scheduleAtFixedRate` and `scheduleWithFixedDelay` method.

## Cross-JVM Context Propagation Sensors

In addition to [JVM Local Context Propagation Sensors](instrumentation/special-sensors.md#jvm-local-context-propagation-sensors), inspectIT Ocelot also supports the propagation of data and the correlation of traces across JVM borders. In this case, the correlation data is attached as a header or an equivalent of the used protocol. Currently, the following sensors are available to enable propagation via HTTP:

* **HTTPUrlConnection Context Propagation Sensor:**
   Can be enabled or disabled via `inspectit.instrumentation.special.http-url-connection-context-propagation`.
   This sensor enables the context propagation across HTTP clients which use [HttpUrlConnection](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html).

* **Apache HTTP Client Context Propagation Sensor:**
   Can be enabled or disabled via `inspectit.instrumentation.special.apache-http-client-context-propagation`.
   This sensor enables the context across HTTP clients which use the [Apache CloseableHttpClient](https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/client/CloseableHttpClient.html).

* **Servlet API Context Propagation Sensor:**
   Can be enabled or disabled via `inspectit.instrumentation.special.servlet-api-client-context-propagation`.
   This sensor enables the context across HTTP servers which use the [Servlet API](https://javaee.github.io/javaee-spec/javadocs/javax/servlet/http/package-summary.html).

## Class Loader Delegation

For performing the instrumentation, inspectIT Ocelot requires that some classes it provides are accessible from the instrumented class. To ensure this, inspectIT pushes these classes to the bootstrap classloader.

However commonly module systems, such as OSGi limit the access to bootstrap classes. For this reason, inspectIT Ocelot instruments all application classloaders to allow access to the relevant inspectIT classes.

Using this sensor, applications based on module systems, such as OSGi are supported out of the box without requiring any configuration changes of the agent or the application!

The class loader delegation can be disabled using the `inspectit.instrumentation.special.class-loader-delegation` property.