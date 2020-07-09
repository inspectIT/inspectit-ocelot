---
id: version-1.3.1-tracing
title: Tracing
original_id: tracing
---

This section provides all the information on trace collection with inspectIT Ocelot.

The inspectIT Ocelot configuration has a master switch for tracing, which can be used to completly disable anything that has to do with trace collection and exporting.
Tracing in the agent can be completely disabled by setting the `inspectit.tracing.enabled` property to `false`.
This way any default inspectIT setting or anything else defined for trace collection will be overruled.
If used, the switch makes sure that the inspectIT Ocelot agent:

* disables all trace exporters
* removes tracing from all [instrumentation rules](instrumentation/rules.md)


### Global Sampling Rate

It is possible to globally regulate the number of traces generated through [sampling](https://opencensus.io/tracing/sampling/).
You can configure the probability with which a trace ends up being collected via `inspectit.tracing.sampleProbability`.
E.g. setting the value to `0.1` will result in only 10% of all traces being collected.
By default, the sample probability is 100%. Note that this global setting only acts as a default value and can be overridden by [individual rules](instrumentation/rules.md#collecting-traces).

### Common Tags as Attributes

Globally defined [common tags](metrics/common-tags.md) used when recording metrics can also be inserted as attributes in traces.
The property `inspectit.tracing.add-common-tags` defines the behavior of this feature.
Available options are:

* `NEVER` - Never add common tags as attributes to spans.
* `ON_GLOBAL_ROOT` - Add common tags only to a global span root. Thus, only to the first span of each trace.
* `ON_LOCAL_ROOT` (default) - Add common tags to local span roots. If a trace spans over several JVMs, then attributes will be set on the first span of each JVM.
* `ALWAYS` - Add common tags as attributes to all spans.

