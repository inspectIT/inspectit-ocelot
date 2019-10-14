---
id: log-correlation
title: Log Correlation
---

InspectIT Ocelot allows you to add trace correlation information to you logs.
This enabled you to find the trace in which a log file was generated or to select all logs given a trace.

Ocelot does this by adding the current trace-id to the "Mapped Diagnostic Context" (MDC) of all of your used logging libraries.
The MDC acts like a storage of environment variables which can be accessed by your log patterns.

Currently, the following logging APIs and Facades are supported:
* Log4J Version 1
* Log4J Version 2
* Logback
* SLF4J

Log correlation is by default disabled. You can enable it using the following configuration snippet:
```yaml
inspectit:
  tracing:
    log-correlation:
      enabled: true
```

When enabled, the trace-id is automatically added to all MDCs.
To make use of this information you also need to adjust your log patterns:
In all of the logging libraries mentioned above you can use `%X{traceid}` to access the trace-id from the MDC.

For example for logback, the following pattern can be used:
```xml
<Console name="Console" target="SYSTEM_OUT">
    <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} TRACE[%X{traceid}] - %msg%n"/>
</Console>
```

As result, log messages generated within an exported trace are now prefixed with the trace-id:

```
09:39:53.014 [main] INFO  test.TestMain TRACE[612772355048a0b21662ed3bc07a6326] - Hello logback!
```

Note that the trace-id will only be available if the log is generated within a trace and the trace is also sampled.
Otherwise the trace-id is empty.

You can change the key under which the trace-id is placed in the MDC using the property `inspectit.tracing.log-correlation.key`.