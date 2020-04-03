---
id: version-1.2-enduser-monitoring-server
title: End User Monitoring with Ocelot
sidebar_label: Overview
original_id: enduser-monitoring-server
---

The inspectIT Ocelot EUM Server can be used to collect EUM data, produced by the [Akamai Boomerang](https://developer.akamai.com/tools/boomerang) EUM Javascript Agent, and calculate metrics based on this data and store them in a metric backend like [Prometheus](https://prometheus.io/) or [InfluxDB](https://www.influxdata.com/products/influxdb-overview/).

For this purpose, [OpenCensus](https://github.com/census-instrumentation/opencensus-java) is used, which enables the server to use all existing metric exporter implementations of OpenCensus.

The EUM server is completely stateless and can be used as a network separation component between the EUM agents and the monitoring backend.

![EUM Server Architecture](assets/eum-architecture.png)