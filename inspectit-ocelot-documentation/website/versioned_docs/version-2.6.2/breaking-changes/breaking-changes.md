---
id: version-2.6.2-Breaking Changes
title: Breaking Changes
original_id: Breaking Changes
---

## Breaking changes in 2.6.0

### Git-Staging of Agent Mappings

In addition to all agent configurations, the agent mappings will be staged as well now.
Thus, it is possible to keep a version history of the agent mappings. 
There have been changed to the agent-mappings-page in the configuration server.

Furthermore, the source branch of the entire agent mappings configuration is adjustable. 
Now you can choose, whether the agent mappings configuration should be used from `WORKSPACE` (like previously) 
or from `LIVE` (promoted changes).
You can find more information [here](config-server/agent-mappings.md#Git-Staging).

### Tag-Guard

The Tag-Guard allows to limit the amount of tag values, which will be exported for a specific tag of a specific measure.
This may help to prevent high cardinalities in your time series database.

The recorded tag values for each tag per measure of an agent will be stored inside a local JSON file. This file serves
as the tag-guard-database and helps to check, if tag values exceeded their limit.
If a tag value has exceeded its limit, the agent health will change to `ERROR`. Therefore, the configured limit should be
evaluated carefully.
You can find more information [here](metrics/tag-guard.md).


## Breaking changes in 2.4.0

### Sampling adjusted

Moving from OpenCensus to OpenTelemetry, the [global sampling](tracing/tracing.md#global-sampling-rate) and [rule-based sampling](instrumentation/rules.md#trace-sampling) has slightly changed.
We now offer three different sampling modes that can be set to `inspectit.tracing.sample-mode` and under the `tracing.sample-mode` property in individual rules.

:::important
We do not expect any breaking change, but we want to promote this.
:::

In case you encounter issues with sampling, the following explanation may help you resolving your issues.
Previously, a mix of the [ProbabilitySampler](https://github.com/census-instrumentation/opencensus-java/blob/master/api/src/main/java/io/opencensus/trace/samplers/ProbabilitySampler.java)
and [SpanBuilderImpl](https://github.com/census-instrumentation/opencensus-java/blob/52f38e48e2ac6cb65e28dcd97b4f7e9650357bba/impl_core/src/main/java/io/opencensus/implcore/trace/SpanBuilderImpl.java#L71) 
from OpenCensus was used. This resulted in *child* spans being sampled if the parent span was not sampled but a sample probability was specified for a rule.
It may be the case that you need to set the `sample-mode` in your individual rules in case `PARENT_BASED` is set globally and parent spans would not be sampled, but individual child spans should be sampled.

| Sample mode                                                                                                             | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|-------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PARENT_BASED` (default for `inspectit.tracing.sample-mode`)                                                            | The setting of the `sample-probability` only has an effect if **no sampling decision** has been made yet. If a parent span has already decided that a trace is sampled or not sampled, this decision will continue to be used. This means that, for example, a method within a trace that has already been started will always be recorded, even if the sample setting has been set to `0`. This also applies to the opposite case where a span has decided not to sample and the sampling rate has been set to `1`. In this case the method will **not** be recorded!. See also the [official documentation of OpenTelemetry](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#parentbased). |
| `TRACE_ID_RATIO_BASED`                                                                                                  | The sampling decision is made for each span, regardless of the parent span's sampling decision, see also the [official documentation of OpenTelemetry](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#parentbased).                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `HYBRID_PARENT_TRACE_ID_RATIO_BASED` (default for `inspectit.instrumentation.rules.[name-of-rule].tracing.sample-mode`) | The span is sampled if the parent span has been sampled, otherwise applies a [TraceIdRatioBasedSampler](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#sampler). This behavior is similar to the previously used [ProbabilitySampler](https://github.com/census-instrumentation/opencensus-java/blob/master/api/src/main/java/io/opencensus/trace/samplers/ProbabilitySampler.java) from OpenCensus.                                                                                                                                                                                                                                                                                        |

## Breaking changes in 2.0.0

### Integration of the OpenTelemetry OpenCensus Shim

Starting with the current release, inspectIT Ocelot migrates from OpenCensus to [OpenTelemetry](https://github.com/open-telemetry). As a first step, we include the [OpenTelemetry OpenCensus Shim](https://github.com/open-telemetry/opentelemetry-java/tree/main/opencensus-shim). inspectIT Ocelot still uses and supports the [OpenCensus-API](https://opencensus.io/quickstart/java/), but the exporter implementations of OpenTelemetry are used.

### AutoTracing currently not available

Due to the migration from OpenCensus to OpenTelemetry, the agent's AutoTracing feature is currently **not** available.
The AutoTracing feature will be available again in the next release of the inspectIT Ocelot agent.

### Removed the tag `service-name` from all exporters

Due to the migration to OpenTelemetry, the tag `service-name` was removed from all exporters. Analogous to this tag, the tag `inspectit.service-name` can now be set globally for all exporters. 

### Updated and removed exporter 

#### Removed `OpenCensusAgentExporter`

Due to the migration from OpenCensus to OpenTelemetry, the `OpenCensus Agent Exporter` (for metrics and traces) has been removed and will not be supported in the future.

#### Added `OTLPMetricsExporter` and `OTLPTraceExporter`

Due to the migration to OpenTelemetry, inspectIT Ocelot now supports OpenTelemetry Protocol (OTLP) exporter for metrics and tracing.

#### Exporter property `url` and `grpc` replaced by `endpoint`

Due to the migration to OpenTelemetry, we approach the naming of OpenTelemetry for the exporters' properties. For this, the previously used properties `url` and `grpc` are replaced by the property `endpoint`. The deprecated properties `url` and `grpc` are still supported in this release but will be removed in future releases.

#### New property `protocol` for Jaeger and OTLP exporter

This release introduces the property `protocol` for the Jaeger and OpenTelemetry Protocol (OTLP) exporter. In case for Jaeger, supported protocols are `http/thrift` and `grpc`. For OTLP, supported protocols are `grpc` and `http/protobuf`.

## Breaking changes in 1.16.0

### Configuration sources only accept valid `YAML`, `JSON` (keys must be wrapped in double quotes) or `properties` notation 

As of version 1.16.0, the [configuration sources](configuration/configuration-sources.md) only accept valid `YAML`, `JSON` or `properties` notation. The "mix and match" of JSON and YAML should be avoided. For JSON, all keys need to be wrapped in double quotes. 
Upgrading to version 1.16.0 may break your startup routines if the JSON passed as command line arguments is invalid. For example, the following code will **not** work anymore and cause an exception:  

```bash
# invalid JSON (keys not wrapped in double quotes) causes an exception
 java -jar inspectit-ocelot-agent-1.16.0.jar 1337 '{inspectit:{service-name:"my-agent"}}'
```
Instead, use valid JSON and wrap the keys in double quotes:

```bash
# valid JSON (keys wrapped in double quotes)
 java -jar inspectit-ocelot-agent-1.16.0.jar 1337 '{"inspectit":{"service-name":"my-agent"}}'
```

## Breaking changes in 1.15.2

There are no breaking changes for version 1.15.2.

## Breaking changes in 1.15.1

There are no breaking changes for version 1.15.1.


## Breaking changes in 1.15.0

### New definition of exporters' enabled property

Instead of a Boolean, the `enabled` property of exporters is now an enum with the values `DISABLED`, `ENABLED` and `IF_CONFIGURED` to express the behaviour of this property more clearly.
For now old configurations using `true` and `false` will still work and be converted to their equivalent new values `IF_CONFIGURED` and `DISABLED` respectively.
This conversion is however deprecated, so configurations should still be updated to the new values.

### Prometheus exporter disabled by default

By default, the Prometheus exporter is now disabled.
This was changed so the exporter's behaviour is in line with the behaviour of other exporters, i.e. it will not run without changing the default settings.
To enable the Prometheus exporter, set its `enabled` property to `ENABLED`.

## Breaking changes in 1.14.0

There are no breaking changes for version 1.14.0.

## Breaking changes in 1.13.0

There are no breaking changes for version 1.13.0.

## Breaking changes in 1.12.2

There are no breaking changes for version 1.12.2.

## Breaking changes in 1.12.1

There are no breaking changes for version 1.12.1.

## Breaking changes in 1.12.0

There are no breaking changes for version 1.12.0.

## Breaking changes in 1.11.1

There are no breaking changes for version 1.11.1.

## Breaking changes in 1.11.0

There are no breaking changes for version 1.11.0.


## Breaking changes in 1.10.1

There are no breaking changes for version 1.10.1.


## Breaking changes in 1.10.0

There are no breaking changes for version 1.10.0.


## Breaking changes in 1.9.0

### Upgraded OpenTelemetry of the EUM Server

The EUM server provides the functionality to receive OpenTelemetry spans and forward them to a tracing backend.

Until now, an early alpha version of OpenTelemetry was used for this purpose, which has now been upgraded to the current, stable version.
As a result, spans from OpenTelemetry libraries older than version `0.9` are no longer compatible and can no longer be processed.

This also means that third-party applications are no longer compatible with the EUM server that did not use OpenTelemetry in version `0.9` or higher, for example like the [Boomerang-OpenTelemetry plugin](https://github.com/NovatecConsulting/boomerang-opentelemetry-plugin) in version `0.7.0`.

### Naming Changes of the `rt.bmr.*` Beacon Properties

In the current release, the existing `rt.bmr` beacon parameter is parsed into new key-value pairs with the naming `rt.bmr.index`, where `index` represents the element's position in the beacon parameter.
These indices are changed to qualified names:

| Deprecated Name | New Name |
| --- | --- | 
|`rt.bmr.0`| `rt.bmr.startTime`|
|`rt.bmr.1`| `rt.bmr.responseEnd`|
|`rt.bmr.2`| `rt.bmr.responseStart`|
|`rt.bmr.3`| `rt.bmr.requestStart`|
|`rt.bmr.4`| `rt.bmr.connectEnd`|
|`rt.bmr.5`| `rt.bmr.secureConnectionStart`|
|`rt.bmr.6`| `rt.bmr.connectStart`|
|`rt.bmr.7`| `rt.bmr.respdomainLookupEndonseEnd`|
|`rt.bmr.8`| `rt.bmr.domainLookupStart`|
|`rt.bmr.9`| `rt.bmr.redirectEnd`|
|`rt.bmr.10`| `rt.bmr.redirectStart`|

## Breaking changes in 1.8.1

There are no breaking changes for version 1.8.1.


## Breaking changes in 1.8

### Changing Capitalization of Self-Monitoring Metric Names

In the current release, the capitalization of the name of the EUM server's self-monitoring metric for counting incoming beacons has been changed.
It was changed from `inspectit-eum_self_beacons_received_COUNT` to `inspectit_eum_self_beacons_received_count`, thus, it consists only of lowercase characters, now.


## Breaking changes in 1.7

There are no breaking changes for version 1.7.


## Breaking changes in 1.6.1

There are no breaking changes for version 1.6.1.


## Breaking changes in 1.6

There are no breaking changes for version 1.6.


## Breaking changes in 1.5

### Regex Replacement in the EUM-Server

In the previous versions, it was possible to specify derived tags in the EUM-Server based
on a single regular expression extraction:

```YAML
inspectit-eum-server:
  tags:
    beacon:
      URL_USER_ERASED: 
        input: u
        regex: "\\/user\\/\d+"
        replacement: "\\/user\\/{id}"
        keep-no-match: true
```

This configuration uses of the tag value `u`, replaces all user-IDs and stores the result in the tag `URL_USER_ERASED`.
While for backwards compatibility reasons this approach still is functional, it has been deprecated in favor of the new syntax:

```YAML
inspectit-eum-server:
  tags:
    beacon:
      URL_USER_ERASED: 
        input: u
        replacements:
         -  regex: "\\/user\\/\d+"
            replacement: "\\/user\\/{id}"
            keep-no-match: true
```

This new syntax allows to specify a list of regular expressions to apply instead of a single one.
The EUM-Server will process all regular expressions in their order and will replace all matches within the input tag.
The `keep-no-match` property defines how to behave in case no match is found in the source tag for a given regex.
If `keep-no-match` is `false`, no tag value will be output in this case. If it is `true`, 
the previous value will be used without performing any replacements.
Hereby the default value of `keep-no-match` has changed: it is now `true` instead of `false`.


## Breaking changes in 1.4

### Using Access Permissions in the Web UI

The web interface of the configuration server now respects the access rights of the individual users.
Previously, every user who could log in had all rights and could therefore do everything.
However, this may cause problems for users who remain logged in during the upgrade.

If the configuration server was recently upgraded to version 1.4 and active users exist,
it can happen that they are no longer able to access the web UI after the upgrade.
In this case the browser cache and cookies must be deleted.
Users should then be able to log on to and use the Web UI as usual.


## Breaking changes in 1.3.1

There are no breaking changes for version 1.3.1.


## Breaking changes in 1.3

### Changed Trace Attributes Behaviour

In previous version it was possible to write [trace attributes](instrumentation/rules.md#adding-attributes)
without actually starting a span in the same method. In this case, attributes would be written to a parent method which was traced.
This could sometimes lead to a surprising behaviour and therefore has been removed.

This behaviour can still be realized if desired via [data propagation](instrumentation/rules.md#data-propagation).

### Reworked Default Agent Configuration

The default configuration of the agent has completely been reworked.

This means that in case your custom instrumentation is based on existing scopes, actions or rules it might stop working.
In order to fix this it is required to adapt your instrumentation to the new default configuration.

### Changed Default Behaviour of the InfluxDB exporter

The InfluxDB metrics exporter has been extended with an optimized handling of counter and sum metrics, which is now the default behaviour.
This new behaviour changes how data is written to InfluxDB, so your existing queries have to be adapted or reworked.

It is still possible to use the previous handling of counters using the exporter's `counters-as-differences` option and setting it to `false`:

    inspectit:
      exporters:
        metrics:
          influx:
            counters-as-differences: false

For more details have a look at the [InfluxDB exporter documentation](metrics/metric-exporters.md#influxdb-exporter).


## Breaking changes in 1.2.1

There are no breaking changes for version 1.2.1.


## Breaking changes in 1.2

There are no breaking changes for version 1.2.


## Breaking changes in 1.1

### Changed default data-propagation behaviour

In previous versions data collected in [instrumentation rules](instrumentation/rules.md) could already be used without
explicitly defining it's propagation behaviour under `inspectit.instrumentation.data`. However, in this case the data
would be automatically down-propagated and used as a tag, which comes with a performance penalty.

To avoid unnecessary propagation and usage as tag for e.g. temporary local data, the behaviour had to be explicitly defined:

```yaml
inspectit:
  instrumentation:
    data:
      temp_variable: {down-propagation: NONE, is-tag: false}
```

As these definitions could be easily forgotten, we changed the default behaviour of data:
It now does *not* propagate and is *not* used as a tag automatically. The exception hereby
is (a) if the data is a [common tag](metrics/common-tags.md) or (b) the data is used as a tag in any
[metric definition](metrics/custom-metrics.md). Common Tags default to JVM_LOCAL down propagation and being a tag.
When a data_key is also used as a tag in a metric definition, it defaults to being a tag but the propagation is not affected.
You can still freely override the behaviour by configuring the settings for your data under `inspectit.instrumentation.data`.
For details see the corresponding [documentation section](instrumentation/rules.md#defining-the-behaviour).

This is a breaking change because your configurations might not work as expected anymore.
You now have to make sure that for each data, where you expect down-propagation to take place, you add a behaviour definition:

```yaml
inspectit:
  instrumentation:
    data:
      down_propagated_var: {down-propagation: JVM_LOCAL}
```

The change of the `is-tag` setting usually should not affect you, as it automatically picks up tags from the metrics definitions.

### Changed metric collection configuration

In previous versions Ocelot allowed the short notation in the configuration when defining the metrics collection.

```yaml
#inspectit.instrumentation.rules is omitted here
example_rule:
  #...
  exit:
    method_duration:
      #action invocation here....

  metrics:
    '[method/duration]' : method_duration
```

This notation is now deprecated and has to be migrated to the explicit notation:

```yaml
#inspectit.instrumentation.rules is omitted here
example_rule:
  #...
  exit:
    method_duration:
      #action invocation here....
    method_name:
      #action invocation here....

  metrics:
    '[method/duration]': 
      value: method_duration
      constant-tags:
        action: checkout
      data-tags:
        method_name: method_name
```

This is a breaking change because your configurations might not work as expected anymore.
In previous versions tags were automatically picked up from the tag context at the moment of the metric recording.
The new notation allows explicit and flexible definition of constant and data tags to be collected with their associated values.
Staying with the short notation means that metric recording will only include addition of the common tags and values, thus most likely breaking the expected behavior.

More information about the new notation can be found in the [collecting metrics](instrumentation/rules.md#collecting-metrics) section.

### Changed EUM server tag configuration

In previous versions of the EUM Server, tags derived from beacon values were specified as follows:

```yaml
inspectit-eum-server:
  tags:
    beacon:
      URL: u
```

This has been extended with the possibility to perform regex replacements, therefore the equivalent configuration now looks as follows:

```yaml
inspectit-eum-server:
  tags:
    beacon:
      URL: 
        input: u
```

For the details on how to perform regex replacements see the [EUM server documentation](enduser-monitoring/enduser-monitoring-server.md#tags-definition).


## Breaking changes in 1.0

There are no breaking changes for version 1.0.


## Breaking changes in 0.6

There are no breaking changes for version 0.6.


## Breaking changes in 0.5

This section discusses the changes that you need to be aware of when migrating your inspectIT Ocelot components to version 0.5.

### Change of the configuration server's configuration prefix

Until now, all components used the configuration prefix `inspectit` for the inspectIT Ocelot specific configuration settings.
This often led to confusion because, especially in examples, it was not clear to which component a particular setting belonged.
To avoid confusion between the configurations of different components the configuration prefix of the configuration server has been changed to `inspectit-config-server`.

The configuration server will not load the configuration if it is still located under the `inspectit` prefix.


## Breaking changes in 0.4

There are no breaking changes for version 0.4.
