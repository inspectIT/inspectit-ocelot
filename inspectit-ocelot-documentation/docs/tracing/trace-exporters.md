---
id: trace-exporters
title: Trace Exporters
---

Metrics exporters are responsible for passing the recorded tracing data to a corresponding storage.

inspectIT Ocelot currently supports the following OpenCensus trace exporters:

* [Logging](#logging-exporter) [[Homepage](https://github.com/open-telemetry/opentelemetry-java/blob/main/exporters/logging/src/main/java/io/opentelemetry/exporter/logging/LoggingSpanExporter.java)]
* [~~Zipkin~~](#zipkin-exporter) [[Homepage](https://zipkin.io/)]
* [~~Jaeger~~](#jaeger-exporter) [[Homepage](https://www.jaegertracing.io/)]

>**Important note**: Starting with version `1.15.0`, inspectIT Ocelot moved from OpenCensus to OpenTelemetry. As a result, the `OpenCensus Agent Exporter` is no longer supported.
Currently, Zipkin and Jaeger are **not** functional and will be re-implemented in the next version.

## Logging Exporter

The Logging exporter exports traces to the system log. By default, the Logging exporter is enabled.

## Zipkin Exporter
>**Important**: the Zipkin exporter is currently not working

The Zipkin exporter exports Traces in Zipkin v2 format to a Zipkin server or other compatible servers.
It can be enabled and disabled via the `inspectit.exporters.tracing.zipkin.enabled` property. By default, the Zipkin exporter is enabled. It however does not have an URL configured. The exporter will start up as soon as you define the `inspectit.exporters.tracing.zipkin.url` property.

For example, when adding the following property to your `-javaagent` options, traces will be sent to a zipkin server running on your localhost with the default port:

```
-Dinspectit.exporters.tracing.zipkin.url=http://127.0.0.1:9411/api/v2/spans
```

When sending spans, Zipkin expects you to give a name of the service where the spans have been recorded. This name can be set using the `inspectit.exporters.tracing.zipkin.service-name` property. This property defaults to `inspectit.service-name`.


## Jaeger Exporter
>**Important**: the Jaeger exporter is currently not working

The Jaeger exports works exactly the same way as the [Zipkin Exporter](#zipkin-exporter).
The corresponding properties are the following:

* `inspectit.exporters.tracing.jaeger.enabled`: enables / disables the Jaeger exporter
* `inspectit.exporters.tracing.jaeger.url`: defines the URL where the spans will be pushed
* `inspectit.exporters.tracing.jaeger.service-name`: defines the service name under which the spans will be published

By default, the Jaeger exporter is enabled but has no URL configured.
The service name defaults to `inspectit.service-name`.

To make inspectIT Ocelot push the spans to a Jaeger server running on the same machine as the agent, the following JVM property can be used:

```
-Dinspectit.exporters.tracing.jaeger.url=http://127.0.0.1:14268/api/traces
```
