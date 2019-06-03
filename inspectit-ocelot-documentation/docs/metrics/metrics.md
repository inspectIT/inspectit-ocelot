---
id: metrics
title: Metrics
---

This section provides all the information on metrics collection with inspectIT Ocelot.

The inspectIT Ocelot configuration has a master switch for metrics, which can be used to completly disable anything that has to do with metrics collection and exporting.
Disabling of metrics completly in the agent can be done by setting the `inspectit.metrics.enabled` property to `false`.
This way any default inspectIT setting or anything else defined for metrics collection will be overruled.

If used, the switch makes sure that the inspectIT Ocelot agent:

* disables all metrics recorders
* does not set up any metrics exporter
* disables the registration of [custom OpenCensus metric and view definitions](metrics/custom-metrics.md)
