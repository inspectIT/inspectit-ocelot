---
id: log-correlation
title: Log Correlation
---

InspectIT Ocelot allows you to add trace information to your log statements in order to correlate them.
This way, it is possible to identify the trace in which a log entry was created or to select all log statements given a specific trace id.

The Ocelot agent does this by adding the current trace-id to the "Mapped Diagnostic Context" (MDC) of the used logging libraries.
The MDC acts like a storage of environment variables which can be accessed by the logger and inject them into the log statements, according to the specified log pattern.

Currently, the following logging APIs and Facades are supported:
* Log4J Version 1
* Log4J Version 2
* Logback
* SLF4J

Log correlation is disabled by default. You can enable it using the following configuration snippet:
```yaml
inspectit:
  tracing:
    log-correlation:
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

```
09:39:53.014 [main] INFO  test.TestMain TRACE[612772355048a0b21662ed3bc07a6326] - Hello logback!
```

> Note that the trace-id is only available when the log statement is within a trace which is sampled, otherwise the trace-id is empty.

You can change the key under which the trace-id is placed in the MDC using the property `inspectit.tracing.log-correlation.key`.