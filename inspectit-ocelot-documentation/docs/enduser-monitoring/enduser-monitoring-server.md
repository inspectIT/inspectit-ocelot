---
id: enduser-monitoring-server
title: End User Monitoring with Ocelot
sidebar_label: Overview
---

The inspectIT Ocelot EUM Server can be used to collect EUM data.
The EUM server is completely stateless and can be used as a network separation component between the EUM agents and the monitoring backend.

![EUM Server Architecture](assets/eum-architecture.png)

# Collecting metrics

The server can be used to collect metrics, produced by the [Akamai Boomerang](https://developer.akamai.com/tools/boomerang) EUM Javascript Agent, and calculate metrics based on this data and store them in a metric backend like [Prometheus](https://prometheus.io/) or [InfluxDB](https://www.influxdata.com/products/influxdb-overview/).

For this purpose, [OpenCensus](https://github.com/census-instrumentation/opencensus-java) is used, which enables the server to use all existing metric exporter implementations of OpenCensus.

# Collecting traces

The server also support collection of EUM traces, as it implements the [OpenTelemetry Trace V1 API](https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/trace/api.md).
The collected traces are propagated to the any registered trace exporter.

Currently, the EUM server supports only [Jaeger](https://www.jaegertracing.io/) as the trace exporter (using the [Protobuf via gRPC](https://www.jaegertracing.io/docs/1.16/apis/#protobuf-via-grpc-stable) API).

:::tip
If you already use Boomerang to collect the EUM metrics, you can automatically collect traces as using the [Boomerang OpenTelemetry plugin](https://github.com/NovatecConsulting/boomerang-opentelemetry-plugin).
:::
