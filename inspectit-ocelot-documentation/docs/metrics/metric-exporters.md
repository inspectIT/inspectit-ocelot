---
id: metric-exporters
title: Metrics Exporters
---

Metrics exporters are responsible for passing the recorded metrics to a metric storage.
They can implement a push approach where metrics are sent to a collector or a pull approach where metrics are scraped by an external system.

If an exporter supports run-time updates it means that it can be enabled/disabled during the run-time or that any property related to the exporter can be changed.
This way you can, for example, change the endpoint where exporter pushes the metrics without a need to restart the application.
In order to use run-time updates, you must enable one of the [externalized configuration methods](configuration/external-configuration-sources) that support dynamic updates.

inspectIT Ocelot currently supports the following OpenCensus metrics exporters:

|Exporter |Supports run-time updates| Push / Pull |Enabled by default
|---|---|---|---|
|[Prometheus Exporter](#prometheus-exporter)|Yes|Pull|Yes
|[OpenCensus Agent Exporter](#opencensus-agent-metrics-exporter)|No|Push|Yes
|[InfluxDB Exporter](#influxdb-exporter)|Yes|Push|Yes

## Prometheus Exporter

Prometheus exporter exposes the metrics in Prometheus format and is the default metrics exporter set up by inspectIT Ocelot.
When enabled, inspectIT starts a Prometheus HTTP server in parallel with your application.
The server is by default started on the port `8888` and metrics can then be accessed by visiting http://localhost:8888/metrics.

The following properties are nested properties below the `inspectit.exporters.metrics.prometheus` property:

|Property |Default| Description
|---|---|---|
|`.enabled`|`true`|If true, the inspectIT Ocelot agent will try to start the Prometheus metrics exporter and Prometheus HTTP server.
|`.host`|`0.0.0.0`|The hostname or network address to which the Prometheus HTTP server should bind.
|`.port`|`8888`|The port the Prometheus HTTP server should use.


> Don't forget to check [the official OpenCensus Prometheus exporter documentation](https://opencensus.io/exporters/supported-exporters/java/prometheus/).

## OpenCensus Agent Metrics Exporter

Metrics can be additionally exported to the [OpenCensus Agent](https://opencensus.io/service/components/agent/).
When enabled, all metrics are sent via gRCP to the OpenCensus Agent. By default, the exporter is enabled, but the agent address is set to `null`.

The following properties are nested properties below the `inspectit.exporters.metrics.open-census-agent` property:

|Property |Default| Description
|---|---|---|
|`.enabled`|`true`|If true, the agent will try to start the OpenCensus Agent Metrics exporter.
|`.address`|`null`|Address of the open-census agent (e.g. localhost:1234).
|`.use-insecure`|`false`|If true, SSL is disabled.
|`.service-name`|refers to `inspectit.service-name`|The service-name which will be used to publish the metrics.
|<nobr>`.reconnection-period`</nobr>|`5`|The time at which the exporter tries to reconnect to the OpenCensus agent.
|`.export-interval`|refers to `inspectit.metrics.frequency`|The export interval of the metrics.

> Don't forget to check [the official OpenCensus Agent exporter documentation](https://opencensus.io/exporters/supported-exporters/java/ocagent/).

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

|Property |Default| Description
|---|---|---|
|`.enabled`|`true`|If true, the agent will try to start the Influx exporter, if also the `url` is not empty.
|`.url`|`null`|The HTTP url of the InfluxDB, e.g. `http://localhost:8086`.
|`.user`|`null`| The user to use for connecting to the InfluxDB, can be empty if the InfluxDB is configured for unauthorized access.
|`.password`|`null`|The password to use for connecting to the InfluxDB, can be empty if the InfluxDB is configured for unauthorized access.
|`.database`|`inspectit`| The InfluxDB database to which the metrics are pushed.
|`.retention-policy`|`autogen`| The retention policy of the database to use for writing metrics.
|`.create-database`|`true`| If enabled, the database defined by the `database` property is automatically created on startup with an `autogen` retention policy if it does not exist yet.
|`.export-interval`|refers to `inspectit.metrics.frequency`|Defines how often metrics are pushed to the InfluxDB.
|<nobr>`.counters-as-differences`</nobr>|`true`|Defines whether counters are exported using their absolute value or as the increase between exports
|`buffer-size`| `40` | In case the InfluxDB is not reachable, failed writes will be buffered and written on the next export. This value defines the maximum number of batches to buffer.