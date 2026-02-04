---
id: enduser-monitoring-server
title: End User Monitoring with inspectIT Ocelot
sidebar_label: Overview
---

The inspectIT Ocelot EUM Server can be used to collect end user (real user) data via  [OpenTelemetry](https://opentelemetry.io/docs/languages/java/).
The EUM server is completely stateless and can be used as a network separation component between 
the EUM agents and the monitoring backend.

![EUM Server Architecture](assets/eum-architecture.png)

# Collecting Metrics

The server can be used to collect metrics, produced by the [Akamai Boomerang](https://techdocs.akamai.com/mpulse-boomerang/docs/welcome-to-mpulse-boomerang) 
EUM JavaScript Agent and calculate metrics based on this data and store them in a metric backend like [Prometheus](https://prometheus.io/) 
or [InfluxDB](https://www.influxdata.com/products/influxdb-overview/).

# Collecting Traces

The server also support collection of EUM traces, as it implements the [OpenTelemetry Tracing API](https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/trace/api.md).
The collected traces are propagated to any registered trace exporter.

Currently, the EUM server supports only [OLTP](https://opentelemetry.io/docs/specs/otlp/) as the trace exporter.

:::tip
If you already use Boomerang to collect the EUM metrics, you can automatically collect traces by using our self-made [Boomerang OpenTelemetry plugin](https://github.com/inspectIT/boomerang-opentelemetry-plugin).
:::
