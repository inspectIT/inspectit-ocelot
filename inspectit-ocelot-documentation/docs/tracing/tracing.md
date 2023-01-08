---
id: tracing
title: Tracing
---

This section provides all the information on trace collection with inspectIT Ocelot.

The inspectIT Ocelot configuration has a master switch for tracing, which can be used to completely disable anything that has to do with trace collection and exporting.
Tracing in the agent can be completely disabled by setting the `inspectit.tracing.enabled` property to `false`.
This way any default inspectIT setting or anything else defined for trace collection will be overruled.
If used, the switch makes sure that the inspectIT Ocelot agent:

* disables all trace exporters
* removes tracing from all [instrumentation rules](instrumentation/rules.md)


### Global Sampling Rate

It is possible to globally regulate the number of traces generated through [sampling](https://opencensus.io/tracing/sampling/).
You can configure the probability with which a trace ends up being collected via `inspectit.tracing.sampleProbability`.
E.g. setting the value to `0.1` will record only 10% of all traces.

The global sampling is also influenced by the `sample-mode` that can be set in `inspectit.tracing.sampleMode`, see the table below.
By default, `PARENT_BASED` is used.

| Sample mode                                                                                                             | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|-------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PARENT_BASED` (default for `inspectit.tracing.sample-mode`)                                                            | The setting of the `sample-probability` only has an effect if **no sampling decision** has been made yet. If a parent span has already decided that a trace is sampled or not sampled, this decision will continue to be used. This means that, for example, a method within a trace that has already been started will always be recorded, even if the sample setting has been set to `0`. This also applies to the opposite case where a span has decided not to sample and the sampling rate has been set to `1`. In this case the method will **not** be recorded!. See also the [official documentation of OpenTelemetry](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#parentbased). |
| `TRACE_ID_RATIO_BASED`                                                                                                  | The sampling decision is made for each span, regardless of the parent span's sampling decision, see also the [official documentation of OpenTelemetry](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#parentbased).                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `HYBRID_PARENT_TRACE_ID_RATIO_BASED` (default for `inspectit.instrumentation.rules.[name-of-rule].tracing.sample-mode`) | The span is sampled if the parent span has been sampled, otherwise applies a [TraceIdRatioBasedSampler](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#sampler). This behavior is similar to the previously used [ProbabilitySampler](https://github.com/census-instrumentation/opencensus-java/blob/master/api/src/main/java/io/opencensus/trace/samplers/ProbabilitySampler.java) from OpenCensus.                                                                                                                                                                                                                                                                                        |


By default, the sample probability is `1.0`, meaning 100% (each trace is recorded).

:::tip
This global setting only acts as a default value and can be **overridden** by [individual rules](instrumentation/rules.md#trace-sampling).
For example, with this technique it can be achieved that a method (e.g. HTTP entrypoint) uses different sampling rates depending on the parameters (e.g. current HTTP path).
:::

### Additional Properties

You can additionally define the following global properties (`inspectit.tracing`-property)

|Property|Default| Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|---|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|`sample-mode`|`PARENT_BASED`| The root sample mode to be used for sampling, see above. Supported modes are [`PARENT_BASED`](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#parentbased), [`TRACE_ID_RATIO_BASED`](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#parentbased) and `HYBRID_PARENT_TRACE_ID_RATIO_BASED` (similar to [OpenCensus ProbabilitySampler](https://github.com/census-instrumentation/opencensus-java/blob/master/api/src/main/java/io/opencensus/trace/samplers/ProbabilitySampler.java)). For more information visit the [official documentation](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#sampler). |
|`max-export-batch-size`|512| The max export batch size for every export, i.e., the maximum number of spans exported by the used `BatchSpanProcessor`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|`schedule-delay-millis`|5000| The delay interval between two consecutive exports in milliseconds.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                

:::warning
These properties take only effect once when the agent is starting. If you change these properties while the agent is running, they will not take effect until the agent retarted.
:::
### Common Tags as Attributes

Globally defined [common tags](metrics/common-tags.md) used when recording metrics can also be inserted as attributes in traces.
The property `inspectit.tracing.add-common-tags` defines the behavior of this feature.
Available options are:

* `NEVER` - Never add common tags as attributes to spans.
* `ON_GLOBAL_ROOT` - Add common tags only to a global span root. Thus, only to the first span of each trace.
* `ON_LOCAL_ROOT` (default) - Add common tags to local span roots. If a trace spans over several JVMs, then attributes will be set on the first span of each JVM.
* `ALWAYS` - Add common tags as attributes to all spans.

### Trace Correlation and Distributed Tracing

The inspectIT Ocelot agent supports out-of-the-box distributed tracing, which allows traces to be correlated across multiple components to trace the entire flow through a system.
By default, the agent supports correlating a communication via HTTP and JMS.
To achieve this, correlation information is exchanged during the communication (for example by injecting additional headers into requests), for which the **B3 Propagation format is used by default**.

If you want to use the agent together with other components that also perform distributed tracing but do not support the correlation information in B3 format, this can be adjusted with the following configuration:

```YAML
inspectit:
  tracing:
    propagation-format: B3 # the format for propagating correlation headers
```

Currently the following formats are supported for sending correlation information:

| Property | Format | Description
|---|---|---|
|`B3` *(default)*|[B3 Propagation](https://github.com/openzipkin/b3-propagation/blob/master/README.md)|B3 Propagation used by, e.g. Zipkin.
|`TRACE_CONTEXT`|[W3C Trace Context](https://www.w3.org/TR/trace-context/#traceparent-header)|Standard headers and a value format to propagate context information.
|`DATADOG`|[Datadog Format](https://github.com/inspectIT/inspectit-ocelot/issues/792)|Headers used by Datadog for context correlation.

:::important
It is important to note that this configuration refers to the format of the correlation information used to **send this data**. When processing correlation information that the agent receives, it automatically uses the correct format.
:::

### Using 64-Bit Trace IDs

Since version 2.0.0, the inspectIT Ocelot Agent is able to generate trace IDs with a size of 64 bits instead of the 128 bit trace IDs used by default by the agent.
The functionality that trace IDs with a length of 64 bits are generated can be activated with the following configuration:

```YAML
inspectit:
  tracing:
    use-64-bit-trace-ids: true
```

:::important
Please note that some propagation formats do not support 64-bit Ids, such as the W3C "Trace Context". In this case the 64-bit trace IDs are padded with leading zeros.
:::