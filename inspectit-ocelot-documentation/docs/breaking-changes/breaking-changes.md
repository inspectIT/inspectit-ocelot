---
id: Breaking Changes
title: Breaking Changes
---

## Breaking changes in 1.16.0 / 2.0? todo

### Agent commands now use grpc

Formerly agents polled the configuration server repeatedly over a REST-endpoint for new commands. 
Now agents start a grpc stream connection once and the configuration server sends new commands to agents and agents their responses over this connection.

This means that new agents are not able to exchange agent commands with old config-servers and vice versa.

## Breaking changes in 1.15.0

### New definition of exporters' enabled property

Instead of a Boolean, the `enabled` property of exporters is now an enum with the values `DISABLED`, `ENABLED` and `IF_CONFIGURED` to express the behaviour of this property more clearly.
For now old configurations using `true` and `false` will still work and be converted to their equivalent new values `IF_CONFIGURED` and `DISABLED` respectively.
This conversion is however deprecated, so configurations should still be updated to the new values.

### Prometheus exporter disabled by default

By default, the Prometheus exporter is now disabled.
This was changed so the exporter's behaviour is in line with the behaviour of other exporters, i.e. it will not run without changing the default settings.
To enable the Prometheus exporter, set its `enabled` property to `ENABLED`.

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