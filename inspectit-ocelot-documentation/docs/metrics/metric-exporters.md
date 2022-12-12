---
id: metric-exporters
title: Metrics Exporters
---

Metrics exporters are responsible for passing the recorded metrics to a metric storage.
They can implement a push approach where metrics are sent to a collector or a pull approach where metrics are scraped by an external system.

If an exporter supports run-time updates it means that it can be enabled/disabled during the run-time or that any property related to the exporter can be changed.
This way you can, for example, change the endpoint where exporter pushes the metrics without a need to restart the application.
In order to use run-time updates, you must enable one of the [externalized configuration methods](configuration/external-configuration-sources) that support dynamic updates.

inspectIT Ocelot currently supports the following metrics exporters:

|Exporter |Supports run-time updates| Push / Pull |Enabled by default|
|---|---|---|---|
|[Logging Exporter (Metrics)](#logging-exporter-metrics) [[Homepage](https://github.com/open-telemetry/opentelemetry-java/blob/main/exporters/logging/src/main/java/io/opentelemetry/exporter/logging/LoggingMetricExporter.java)]|Yes|Push|No|
|[Prometheus Exporter](#prometheus-exporter)|Yes|Pull|No|
|[InfluxDB Exporter](#influxdb-exporter)|Yes|Push|No|
|[OTLP Exporter (Metrics)](#otlp-exporter-metrics) [[Homepage](https://github.com/open-telemetry/opentelemetry-java/tree/main/exporters/otlp/metrics)]|Yes|Push|No|

>**Important note**: Starting with version `2.0.0`, inspectIT Ocelot moved from OpenCensus to OpenTelemetry. As a result, the `OpenCensus Agent Exporter` is no longer supported.

## Logging Exporter (Metrics)

The Logging exporter exports the metrics to the system log. By default, the exporter is disabled. 
The following properties are nested properties below the `inspectit.exporters.metrics.logging`:

|Property |Default| Description
|---|---|---|
|`.enabled`| `DISABLED` |If `ENABLED` or `IF_CONFIGURED`, the inspectIT Ocelot agent will try to start the Logging metrics exporter.
|`.export-interval`|refers to `inspectit.metrics.frequency`|The export interval of the metrics.

## Prometheus Exporter

Prometheus exporter exposes the metrics in Prometheus format and is the default metrics exporter set up by inspectIT Ocelot.
When enabled, inspectIT starts a Prometheus HTTP server in parallel with your application.
The server is by default started on the port `8888` and metrics can then be accessed by visiting http://localhost:8888/metrics.

The following properties are nested properties below the `inspectit.exporters.metrics.prometheus` property:

|Property | Default    | Description
|---|------------|---|
|`.enabled`| `DISABLED` |If `ENABLED` or `IF_CONFIGURED`, the inspectIT Ocelot agent will try to start the Prometheus metrics exporter and Prometheus HTTP server.
|`.host`| `0.0.0.0`  |The hostname or network address to which the Prometheus HTTP server should bind.
|`.port`| `8888`     |The port the Prometheus HTTP server should use.


> Don't forget to check [the official OpenTelemetry Prometheus exporter documentation](https://github.com/open-telemetry/opentelemetry-java/tree/main/exporters/prometheus).

## InfluxDB Exporter
If enabled, metrics are pushed at a specified interval directly to a given InfluxDB v1.x instance.
To enable the InfluxDB Exporters, it is only required to specify the `url`.

The InfluxDB exporter provides a special handling for counter and sum metrics which is enabled by default and can be disabled using the `counters-as-differences` option.
Usually, the absolute value of such counters is irrelevant when querying the data, instead you want to have the increase of it over a certain period of time.
With the `counters-as-differences` option enabled, counters are preprocessed before being exported.

Instead of writing the absolute value of each counter into the InfluxDB, only the increase since the last export will be written.
In addition no value will be exported, if the counter has not changed since the last export.
This can greatly reduce the amount of data written into the InfluxDB, especially if the metrics are quite constant and won't change much.

The following properties are nested properties below the `inspectit.exporters.metrics.influx` property:

|Property | Default                                 | Description|
|---|-----------------------------------------|---|
|`.enabled`| `IF_CONFIGURED`                         |If `ENABLED` or `IF_CONFIGURED`, the agent will try to start the Influx exporter. If the url is not set, it will log a warning if set to `ENABLED` but fail silently if set to `IF_CONFIGURED`.|
|`.endpoint`| `null`                                  |The HTTP endpoint of the InfluxDB, e.g. `http://localhost:8086`.|
|`.user`| `null`                                  | The user to use for connecting to the InfluxDB, can not be empty.|
|`.password`| `null`                                  |The password to use for connecting to the InfluxDB, can be not be empty.|
|`.database`| `inspectit`                             | The InfluxDB database to which the metrics are pushed.|
|`.retention-policy`| `autogen`                               | The retention policy of the database to use for writing metrics.|
|`.create-database`| `true`                                  | If enabled, the database defined by the `database` property is automatically created on startup with an `autogen` retention policy if it does not exist yet.|
|`.export-interval`| refers to `inspectit.metrics.frequency` |Defines how often metrics are pushed to the InfluxDB.|
|<nobr>`.counters-as-differences`</nobr>| `true`                                  |Defines whether counters are exported using their absolute value or as the increase between exports|
|`buffer-size`| `40`                                    | In case the InfluxDB is not reachable, failed writes will be buffered and written on the next export. This value defines the maximum number of batches to buffer.|

## OTLP Exporter (Metrics)

The OpenTelemetry Protocol (OTLP) exporters export the metrics to the desired endpoint at a specified interval. 
To enable the OTLP exporters, it is only required to specify the `url`.

The following properties are nested properties below the `inspectit.exporters.metrics.otlp-grpc` property:

| Property                | Default         | Description                                                                                                                                                                                                                  |
|-------------------------|-----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `.enabled`              | `IF_CONFIGURED` | If `ENABLED` or `IF_CONFIGURED`, the inspectIT Ocelot agent will try to start the OTLP gRPC metrics exporter.                                                                                                                |
| `.endpoint`             | `null`          | Target to which the exporter is going to send metrics, e.g. `http://localhost:4317`                                                                                                                                          |
| `.protocol`             | `null`          | The transport protocol, see [OTEL documentation](https://opentelemetry.io/docs/reference/specification/protocol/exporter/). Supported protocols are `grpc` and `http/protobuf`.                                              |
| `.preferredTemporality` | `CUMULATIVE`    | The preferred output aggregation temporality, see [OTEL documentation](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md). Supported values are `CUMULATIVE` and `DELTA`.| 
| `.headers`              | `null`          | Key-value pairs to be used as headers associated with gRPC or HTTP requests, see [OTEL documentation](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md).|
| `.compression` | `NONE`          | The compression method, see [OTEL documentation](https://opentelemetry.io/docs/reference/specification/protocol/exporter/). Supported compression methods are `gzip` and `none`.                                   |
| `.timeout`     | `10s`           | Maximum time the OTLP exporter will wait for each batch export, see [OTEL documentation](https://opentelemetry.io/docs/reference/specification/protocol/exporter/).                           |

To make inspectIT Ocelot push the metris via OTLP to, e.g. an OpenTelemetry Collector running on the same machine as the agent, the following JVM property can be used:

```
-Dinspectit.exporters.metrics.otlp.endpoint=http://127.0.0.1:4317
-Dinspectit.exporters.metrics.otlp.protocol=grpc
```