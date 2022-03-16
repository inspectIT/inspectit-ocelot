---
id: trace-exporters
title: Trace Exporters
---

Tracing exporters are responsible for passing the recorded tracing data to a corresponding storage.

inspectIT Ocelot currently supports the following trace exporters:

* [Logging](#logging-exporter) [[Homepage](https://github.com/open-telemetry/opentelemetry-java/blob/main/exporters/logging/src/main/java/io/opentelemetry/exporter/logging/LoggingSpanExporter.java)]
* [Zipkin](#zipkin-exporter) [[Homepage](https://zipkin.io/)]
* [Jaeger](#jaeger-exporter) [[Homepage](https://www.jaegertracing.io/)]

>**Important note**: Starting with version `1.15.0`, inspectIT Ocelot moved from OpenCensus to OpenTelemetry. As a result, the `OpenCensus Agent Exporter` is no longer supported and has been removed.  
> Additionally, with OpenTelemetry, inspectIT Ocelot does not support the `service-name` property for individual exporter services anymore. Thus, we removed the `service-name` property from the Jaeger and Zipkin exporter. Please use the global `inspectit.service-name` property instead.

## Logging Exporter

The Logging exporter exports traces to the system log. By default, the Logging exporter is disabled.
The Logging trace exporter has the following properties:
- `inspectit.exporters.tracing.logging.enabled`: enables/disables the Logging trace exporter.

## Zipkin Exporter

The Zipkin exporter exports Traces in Zipkin v2 format to a Zipkin server or other compatible servers.

By default, the Zipkin exporter is enabled but the URL needed for the exporter to actually start is set to `null`.

The following properties are nested properties below the `inspectit.exporters.tracing.zipkin` property:

|Property |Default| Description
|---|---|---|
|`.enabled`|`IF_CONFIGURED`|If `ENABLED` or `IF_CONFIGURED`, the agent will try to start the Zipkin exporter. If the url is not set, it will log a warning if set to `ENABLED` but fail silently if set to `IF_CONFIGURED`.
|`.url`|`null`|v2 URL under which the ZipKin server can be accessed (e.g. http://127.0.0.1:9411/api/v2/spans).
|`.service-name`|refers to `inspectit.service-name`|The service-name which will be used to publish the spans.

To make inspectIT Ocelot push the spans to a Zipkin server running on the same machine as the agent, the following JVM property can be used:

```
-Dinspectit.exporters.tracing.zipkin.url=http://127.0.0.1:9411/api/v2/spans
```

## Jaeger Exporter

The Jaeger exports works exactly the same way as the [Zipkin Exporter](#zipkin-exporter).

By default, the Jaeger exporter is enabled but the URL needed for the exporter to actually start is set to `null`.

The following properties are nested properties below the `inspectit.exporters.tracing.jaeger` property:

|Property |Default| Description
|---|---|---|
|`.enabled`|`IF_CONFIGURED`|If `ENABLED` or `IF_CONFIGURED`, the agent will try to start the Jaeger exporter. If the url is not set, it will log a warning if set to `ENABLED` but fail silently if set to `IF_CONFIGURED`.
|`.url`|`null`|URL under which the Jaeger Thrift server can be accessed (e.g. http://127.0.0.1:14268/api/traces).
|`.service-name`|refers to `inspectit.service-name`|The service-name which will be used to publish the spans.

To make inspectIT Ocelot push the spans to a Jaeger server running on the same machine as the agent, the following JVM property can be used:

```
-Dinspectit.exporters.tracing.jaeger.url=http://127.0.0.1:14268/api/traces
```
