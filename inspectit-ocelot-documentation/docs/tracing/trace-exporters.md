---
id: trace-exporters
title: Trace Exporters
---

Metrics exporters are responsible for passing the recorded tracing data to a corresponding storage.

inspectIT Ocelot currently supports the following OpenCensus trace exporters:

* [Zipkin](#zipkin-exporter) [[Homepage](https://zipkin.io/)]
* [Jaeger](#jaeger-exporter) [[Homepage](https://www.jaegertracing.io/)]
* [OpenCensus Agent](#opencensus-agent-trace-exporter) [[Homepage](https://opencensus.io/exporters/supported-exporters/java/ocagent/)]

## Zipkin Exporter

The Zipkin exporter exports Traces in Zipkin v2 format to a Zipkin server or other compatible servers.

By default, the Zipkin exporter is enabled but the URL needed for the exporter to actually start is set to `null`.

The following properties are nested properties below the `inspectit.exporters.tracing.zipkin` property:

|Property |Default| Description
|---|---|---|
|`.enabled`|`IF_CONFIGURED`|If ENABLED or IF_CONFIGURED, the agent will try to start the Zipkin exporter. If the url is not set, it will log an error with ENABLED but fail silently with IF_CONFIGURED.
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
|`.enabled`|`IF_CONFIGURED`|If ENABLED or IF_CONFIGURED, the agent will try to start the Jaeger exporter. If the url is not set, it will log an error with ENABLED but fail silently with IF_CONFIGURED.
|`.url`|`null`|URL under which the Jaeger Thrift server can be accessed (e.g. http://127.0.0.1:14268/api/traces).
|`.service-name`|refers to `inspectit.service-name`|The service-name which will be used to publish the spans.

To make inspectIT Ocelot push the spans to a Jaeger server running on the same machine as the agent, the following JVM property can be used:

```
-Dinspectit.exporters.tracing.jaeger.url=http://127.0.0.1:14268/api/traces
```

## OpenCensus Agent Trace Exporter

Spans can be additionally exported to the [OpenCensus Agent](https://opencensus.io/service/components/agent/).
When enabled, all Spans are sent via gRCP to the OpenCensus Agent. By default, the exporter is enabled, but the agent address that is needed for the exporter to actually start is set to `null`.

The following properties are nested properties below the `inspectit.exporters.tracing.open-census-agent` property:

|Property |Default| Description
|---|---|---|
|`.enabled`|`IF_CONFIGURED`|If ENABLED or IF_CONFIGURED, the agent will try to start the OpenCensus Agent Trace exporter. If the address is not set, it will log an error with ENABLED but fail silently with IF_CONFIGURED.
|`.address`|`null`|Address of the open-census agent (e.g. localhost:1234).
|`.use-insecure`|`false`|If true, SSL is disabled.
|`.service-name`|refers to `inspectit.service-name`|The service-name which will be used to publish the spans.
|`.reconnection-period`|`5`|The time at which the exporter tries to reconnect to the OpenCensus agent.

> Don't forget to check [the official OpenCensus Agent exporter documentation](https://opencensus.io/exporters/supported-exporters/java/ocagent/).