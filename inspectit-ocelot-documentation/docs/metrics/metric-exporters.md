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

|Property |Default| Description
|---|---|---|
|`inspectit.exporters.metrics.prometheus.enabled`|`true`|If true, the inspectIT Ocelot agent will try to start the Prometheus metrics exporter and Prometheus HTTP server.
|`inspectit.exporters.metrics.prometheus.host`|`0.0.0.0`|The hostname or network address to which the Prometheus HTTP server should bind.
|`inspectit.exporters.metrics.prometheus.port`|`8888`|The port the Prometheus HTTP server should use.


> Don't forget to check [the official OpenCensus Prometheus exporter documentation](https://opencensus.io/exporters/supported-exporters/java/prometheus/).

## OpenCensus Agent Metrics Exporter

Metrics can be additionally exported to the [OpenCensus Agent](https://opencensus.io/service/components/agent/).
When enabled, all metrics are sent via gRCP to the OpenCensus Agent. By default, the exporter is enabled, but the agent address is set to `null`.

|Property |Default| Description
|---|---|---|
|`inspectit.exporters.metrics.open-census-agent.enabled`|`true`|If true, the agent will try to start the OpenCensus Agent Metrics exporter.
|`inspectit.exporters.metrics.open-census-agent.address`|`null`|Address of the open-census agent (e.g. localhost:1234).
|`inspectit.exporters.metrics.open-census-agent.use-insecure`|`false`|If true, SSL is disabled.
|`inspectit.exporters.metrics.open-census-agent.service-name`|refers to `inspectit.service-name`|The service-name which will be used to publish the metrics.
|`inspectit.exporters.metrics.open-census-agent.reconnection-period`|`5`|The time at which the exporter tries to reconnect to the OpenCensus agent.
|`inspectit.exporters.metrics.open-census-agent.export-interval`|refers to `inspectit.metrics.frequency`|The export interval of the metrics.

> Don't forget to check [the official OpenCensus Agent exporter documentation](https://opencensus.io/exporters/supported-exporters/java/ocagent/).

## InfluxDB Exporter

If enabled, metrics are pushed at a specified interval directly to a given influxDB v1.x instance.
To enable the InfluxDB Exporters, it is only required to specify the `url`.

|Property |Default| Description
|---|---|---|
|`inspectit.exporters.metrics.influx.enabled`|`true`|If true, the agent will try to start the Influx exporter, if also the `url` is not empty.
|`inspectit.exporters.metrics.influx.url`|`null`|The HTTP url of the influxDB, e.g. `http://localhost:8086`.
|`inspectit.exporters.metrics.influx.user`|`null`| The user to use for connecting to the influxDB, can be empty if the influxDB is configured for unauthorized access.
|`inspectit.exporters.metrics.influx.password`|`null`|The password to use for connecting to the influxDB, can be empty if the influxDB is configured for unauthorized access.
|`inspectit.exporters.metrics.influx.database`|`inspectit`| The influxDB database to which the metrics are pushed.
|`inspectit.exporters.metrics.influx.retention-policy`|`autogen`| The retention policy of the database to use for writing metrics.
|`inspectit.exporters.metrics.influx.create-database`|`true`| If enabled, the database defined by the `database` property is automatically created on startup with an `autogen` retention policy if it does not exist yet.
|`inspectit.exporters.metrics.influx.export-interval`|refers to `inspectit.metrics.frequency`|Defines how often metrics are pushed to the influxDB.