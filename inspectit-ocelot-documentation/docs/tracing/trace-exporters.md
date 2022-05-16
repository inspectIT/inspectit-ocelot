---
id: trace-exporters
title: Trace Exporters
---

Tracing exporters are responsible for passing the recorded tracing data to a corresponding storage.

inspectIT Ocelot currently supports the following trace exporters:

* [Logging (Traces)](#logging-exporter-traces) [[Homepage](https://github.com/open-telemetry/opentelemetry-java/blob/main/exporters/logging/src/main/java/io/opentelemetry/exporter/logging/LoggingSpanExporter.java)]
* [Zipkin](#zipkin-exporter) [[Homepage](https://zipkin.io/)]
* [Jaeger](#jaeger-exporter) [[Homepage](https://www.jaegertracing.io/)]
* [OTLP (Traces)](#otlp-exporter-traces) [[Homepage](https://github.com/open-telemetry/opentelemetry-java/tree/main/exporters/otlp/trace)]

>**Important note**: Starting with version <mark>`2.X.0`</mark>, inspectIT Ocelot moved from OpenCensus to OpenTelemetry. As a result, the `OpenCensus Agent Exporter` is no longer supported and has been removed.  
> Additionally, with OpenTelemetry, inspectIT Ocelot does not support the `service-name` property for individual exporter services anymore. Thus, we removed the `service-name` property from the Jaeger and Zipkin exporter. Please use the global `inspectit.service-name` property instead.

## Logging Exporter (Traces)

The Logging exporter exports traces to the system log. By default, the Logging exporter is disabled.
The following properties are nested properties below the `inspectit.exporters.tracing.logging` property:

| Property   | Default    | Description                                                  |
| ---------- | ---------- | ------------------------------------------------------------ |
| `.enabled` | `DISABLED` | If `ENABLED` or `IF_CONFIGURED`, the agent will try to start the Logging trace exporter. |

To make inspectIT Ocelot write the spans to the system log, the following JVM property can be used:

`-Dinspectit.exporters.tracing.logging.enabled=ENABLED`

## Zipkin Exporter

The Zipkin exporter exports Traces in Zipkin v2 format to a Zipkin server or other compatible servers.

By default, the Zipkin exporter is enabled but the URL needed for the exporter to actually start is set to `null`.

The following properties are nested properties below the `inspectit.exporters.tracing.zipkin` property:

|Property |Default| Description|
|---|---|---|
|`.enabled`|`IF_CONFIGURED`|If `ENABLED` or `IF_CONFIGURED`, the agent will try to start the Zipkin exporter. If the url is not set, it will log a warning if set to `ENABLED` but fail silently if set to `IF_CONFIGURED`.|
|`.endpoint`|`null`|v2 URL under which the ZipKin server can be accessed (e.g. http://127.0.0.1:9411/api/v2/spans).|

To make inspectIT Ocelot push the spans to a Zipkin server running on the same machine as the agent, the following JVM property can be used:

```
-Dinspectit.exporters.tracing.zipkin.url=http://127.0.0.1:9411/api/v2/spans
```

## Jaeger Exporter

The Jaeger exports works exactly the same way as the [Zipkin Exporter](#zipkin-exporter). InspectIT Ocelot supports thrift and gRPC Jaeger exporter.

By default, the Jaeger exporters are enabled but the URL/gRPC endpoint needed for the exporter to actually start is set to `null`.

### Jaeger Thrift Exporter 

The following properties are nested properties below the `inspectit.exporters.tracing.jaeger` property:

|Property | Default         | Description|
|---|-----------------|---|
|`.enabled`| `IF_CONFIGURED` |If `ENABLED` or `IF_CONFIGURED`, the agent will try to start the Jaeger exporter. If the url is not set, it will log a warning if set to `ENABLED` but fail silently if set to `IF_CONFIGURED`.|
|`.endpoint`| `null`          |URL endpoint under which the Jaeger server can be accessed (e.g. http://127.0.0.1:14268/api/traces).|
|`.protocol`| `null`          |The transport protocol. Supported protocols are `grpc` and `http/thrift`.|

To make inspectIT Ocelot push the spans to a Jaeger server running on the same machine as the agent, the following JVM property can be used:

```
-Dinspectit.exporters.tracing.jaeger.endpoint=http://127.0.0.1:14268/api/traces
```

## OTLP Exporter (Traces)

The OpenTelemetry Protocol (OTLP) exporters export the Traces in OTLP to the desired endpoint at a specified interval. 
By default, the OTLP exporters are enabled but the URL endpoint needed for the exporter to actually start is set to `null`.

The following properties are nested properties below the `inspectit.exporters.traces.otlp` property:

| Property    | Default    | Description                                                                                                                                                                     |
| ----------- |------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `.enabled`  | `IF_CONFIGURED` | If `ENABLED` or `IF_CONFIGURED`, the inspectIT Ocelot agent will try to start the OTLP gRPC trace exporter.                                                                     |
| `.endpoint` | `null`     | Target to which the exporter is going to send traces, e.g. `http://localhost:4317`                                                                                              |
| `.protocol` | `null`     | The transport protocol, see [OTEL documentation](https://opentelemetry.io/docs/reference/specification/protocol/exporter/). Supported protocols are `grpc` and `http/protobuf`. |

To make inspectIT Ocelot push the spans via OTLP to, e.g. an OpenTelemetry Collector running on the same machine as the agent, the following JVM property can be used:

```
-Dinspectit.exporters.tracing.otlp.endpoint=http://127.0.0.1:4317
-Dinspectit.exporters.tracing.otlp.protocol=grpc
```
