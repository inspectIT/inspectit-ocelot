---
id: log-correlation
title: Log Correlation
---

InspectIT Ocelot allows you to add trace information to your log statements in order to correlate them.
This way, it is possible to identify the trace in which a log entry was created or to select all log statements given a specific trace id.

## Trace ID Injection Using MDC

The Ocelot agent does this by adding the current trace-id to the "Mapped Diagnostic Context" (MDC) of the used logging libraries.
The MDC acts like a storage of environment variables which can be accessed by the logger and inject them into the log statements, according to the specified log pattern.

Currently, the following logging APIs and Facades are supported:
* `Log4J Version 1`
* `Log4J Version 2`
* `Logback`
* `SLF4J`

Log correlation is disabled by default. You can enable it using the following configuration snippet:
```yaml
inspectit:
  tracing:
    log-correlation:
      trace-id-mdc-injection:
        enabled: true
```

When enabled, the trace-id is automatically added to all MDCs.
In order to make use of this information the log patterns have to be adapted.
In all of the logging libraries mentioned above you can use `%X{traceid}` to access the trace-id from the MDC.

For example for logback, the following pattern can be used:
```xml
<Console name="Console" target="SYSTEM_OUT">
    <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} TRACE[%X{traceid}] - %msg%n"/>
</Console>
```

As a result, log messages, generated within an exported trace, will be prefixed with the corresponding trace-id:

```text
09:39:53.014 [main] INFO  test.TestMain TRACE[612772355048a0b21662ed3bc07a6326] - Hello logback!
```

> Note that the trace ID is only available when the log statement is within a trace which is sampled, otherwise the trace-id is empty.

You can change the key under which the trace-id is placed in the MDC using the property `inspectit.tracing.log-correlation.trace-id-mdc-injection.key`.

By default, the trace-id will be inserted into all MDCs. If required, you can selectively exclude the supported libraries using the following flags:
```yaml
inspectit:
  tracing:
    log-correlation:
      trace-id-mdc-injection:
        slf4j-enabled: true  # Set to "false" to disable slf4J-Support
        log4j1-enabled: true # Set to "false" to disable Log4J Version 1 Support
        log4j2-enabled: true # Set to "false" to disable Log4J Version 2 Support
        jboss-logmanager-enabled: true # Set to "false" to disable JBoss Logmanager support
```
    
        
        
## Automatical Trace ID Injection

:::warning Experimental Feature
Please note that this is an experimental feature.
It is recommended to insert the trace IDs into the log messages via the MDC, which is described in section [Trace ID Injection Using MDC](#trace-id-injection-using-mdc).
:::

The inspectIT Ocelot Java agent provides the ability to automatically inject trace IDs into log statements in order to correlate traces and logs. When this approach is used, there are the same limitations as when [injecting trace IDs using MDCs](#trace-id-injection-using-mdc), which means that a trace ID can only be injected into a log statement when it is within a sampled trace.

Currently, the following logging APIs and Facades are supported:
* `Log4J Version 1`
* `Log4J Version 2`

The automatic trace ID injection is disabled by default. You can enable it using the following configuration snippet:
```yaml
inspectit:
  tracing:
    log-correlation:
      trace-id-auto-injection:
        enabled: true
```

In case a trace ID exists which is automatically injected, the agent will add the trace ID to the beginning of the log statement. Furthermore, the trace ID is wrapped into a prefix and suffix which results in the following format: `[PREFIX][TRACE_ID][SUFFIX][ORIGINAL_MESSAGE]`

The used prefix and suffix can be configured using the following configuration snippet - the shown values are the default ones: 

```yaml
inspectit:
  tracing:
    log-correlation:
      trace-id-auto-injection:
        prefix: '[TraceID: '
        suffix: ']'
```

The previous configuration would lead to the following log statement:

```text
[TraceID: 612772355048a0b21662ed3bc07a6326]This is my log statement.
```

:::note
Please note that the trace ID will be injected at **the beginning of the log message and not at the beginning of the log pattern**.
The following output shows an example when the message is logged using a certain log pattern:

```text
18:19:43.474 [main] INFO  org.Example - [TraceID: e32ce6197f774d229460b2fd14448cf6]This is a test.
```
:::