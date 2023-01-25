---
id: eum-server-configuration
title: EUM Server, Metrics and Exporter Configuration
sidebar_label: Server, Metrics and Exporter Configuration
---

The configuration file defines the mapping between the concrete Boomerang metric and a OpenCensus metric, as the following sample configuration file shows:

```yaml
inspectit-eum-server:
  definitions:
     page_ready_time:
      measure-type: LONG
      value-expression: "{t_page}"
      unit: ms
      views:
        '[page_ready_time/SUM]': {aggregation: SUM}
        '[page_ready_time/COUNT]': {aggregation: COUNT}

    load_time:
      measure-type: LONG
      value-expression: "{t_done}"
      beacon-requirements:
        - field: rt.quit
          requirement: NOT_EXISTS
      unit: ms
      views:
        '[load_time/SUM]': {aggregation: SUM}
        '[load_time/COUNT]': {aggregation: COUNT}

    calc_load_time:
      measure-type: LONG
      value-expression: "{rt.end} - {rt.tstart}"
      beacon-requirements:
        - field: rt.quit
          requirement: NOT_EXISTS
      unit: ms
      views:
        '[calc_load_time/SUM]': {aggregation: SUM}
        '[calc_load_time/COUNT]': {aggregation: COUNT}

    start_timestamp:
      measure-type: LONG
      value-expression: "{rt.tstart}"
      unit: ms

    navigation_start_timestamp:
      measure-type: LONG
      value-expression: "{rt.nstart}"
      unit: ms

    end_timestamp:
      measure-type: LONG
      value-expression: "{rt.end}"
      unit: ms
      views:
        end_timestamp:
          aggregation: LAST_VALUE
          tags: {APPLICATION: true}

  tags:
    extra:
      APPLICATION: my-application
    
    beacon:
      URL: u
      OS: ua.plt
    
    define-as-global:
      - URL
      - OS
      - COUNTRY_CODE
    
    custom-ip-mapping:
      department-1:
        - 10.10.0.0/16
        - 11.11.0.3
      department-2:
        - 14.14.0.0/16
        - 14.15.0.1
  exporters:
    metrics:
      prometheus:
        enabled: true
        host: localhost
        port: 8888
    tracing:
      jaeger:
        enabled: true
        grpc: localhost:14250
        service-name: browser-js
  security:
    enabled: false
    authorization-header: Authorization
    permitted-urls:
      - "/actuator/health"
      - "/boomerang/**"
    auth-provider:
      simple:
        enabled: false
        watch: true
        frequency: 60s
        token-directory: ""
        default-file-name: "default-token-file.yaml"
```

## Metrics Definition

A metric is defined the same way as in the inspectIT Ocelot Java agent. Please see the section [Metrics / Custom Metrics](metrics/custom-metrics.md) for detailed information.

In contrast to the agent's metric definition, the EUM server's metric definition contains additional fields.
These additional fields are the following:

| Attributes | Note |
| --- | --- |
| `value-expression` | An expression used to calculate the measure's value from a beacon. |
| `beacon-requirements` | Requirements which have to be fulfilled by Beacons. Beacons which do not match all requirements will be ignored by this metric definition. |

### Smoothed Average View
:::note
The Smoothed Average View is currently only available in the EUM server.
:::

In addition to the [Quantile View](../metrics/custom-metrics#quantile-views), which is already described in the inspectIT Ocelot Java agent configuration, 
the EUM Server provides a Smoothed Average View.
In contrast to the quantiles, it is possible to drop values of a certain time window. This can be useful to deliberately remove outliers before averaging.

:::note
Please read the section on [Quantile Views](../metrics/custom-metrics#quantile-views) to get an insight into the data collection. The Smoothed Average View is based on the same principle.
:::

The actual metric [configuration](../metrics/custom-metrics#configurations) are extended in the EUM server by the following properties:
|Config Property|Default| Description
|---|---|---|
|`aggregation`|`LAST_VALUE`|Specifies how the measurement data is aggregated in this view. Possible values are `LAST_VALUE`, `COUNT`, `SUM`, `HISTOGRAM` `QUANTILES` and `SMOOTHED_AVERAGE`. Except for `QUANTILES` and `SMOOTHED_AVERAGE`, these correspond to the [OpenCensus Aggregations](https://opencensus.io/stats/view/#aggregations).
|`drop-upper`|`0.0`| *Required if aggregation is `SMOOTHED_AVERAGE`.* Specifies the percentage of the highest values to be dropped before calculating the average.
|`drop-lower`|`0.0`| *Required if aggregation is `SMOOTHED_AVERAGE`.* Specifies the percentage of the lowest values to be dropped before calculating the average.
|`time-window`|`${inspectit.metrics.frequency}`| *Required if aggregation is `QUANTILES` or `SMOOTHED_AVERAGE`.* The time window over which the quantiles or the smoothed average are captured.
|`max-buffered-points`|`16384`| *Required if aggregation is `QUANTILES` or `SMOOTHED_AVERAGE`.* A safety limit defining the maximum number of points to be buffered.

As an example, the following snippet defines a metric with the name `load_time` and a view `loadtime/smoothed`:
The configuration has the effect of ordering the values in a 1-minute time window by size and dropping the upper 10 percent before calculating the average.

```YAML
inspectit:
  metrics:
    definitions:
      load_time:
        measure-type: LONG
        value-expression: "{t_done}"
        beacon-requirements:
          - field: rt.quit
            requirement: NOT_EXISTS
        unit: ms
        views:
          '[load_time/smoothed]':
              aggregation: SMOOTHED_AVERAGE
              time-window: 1m
              drop-upper: 0.1
              drop-lower: 0.0
```

### Value Expressions

The `value-expression` field can be used to specify a field which value is used for the specified metrics.
In order to reference a field, the following pattern is used: `{FIELD_KEY}`.
For example, a valid expression, used to extract the value of a field `t_load`, would be `{t_load}`.

:::note
Note that a beacon has to contain all fields referenced by the expression in order to be evaluated and recorded.
:::

Value expressions also support operations for basic arithmetic operations. Thus, to calculate a difference of two beacon fields, the following expression can be used:
```YAML
  ...
    my-metric:
      ...
      value-expression: "{field.a} - {field.b}"
      ...
```

Value expression are supporting the following operations:
* addition
* subtraction
* multiplication
* division
* unary plus/minus
* parentheses

Using the operations above, complex calculations can be done, for example:
```YAML
  ...
    my-metric:
      ...
      value-expression: "- {field.c} * ({field.a} - {field.b}) / {field.a}"
      ...
```

### Beacon Requirements

The `beacon-requirements` field can be used to specify requirements which have to be fulfilled by the beacons in order to be evaluated by a certain metric.
If any requirement does not fit a beacon, the beacon is ignored by the metric.

The following requirements are available:

| Type | Note |
| --- | --- |
| `EXISTS` | The targeted field must exist. |
| `NOT_EXISTS` | The targeted field must not exist. |
| `HAS_INITIATOR` | The beacon must have one of the specified initiators. |

Firstly, you can specify that the metric expects a field to exist or not exist in the beacon:

```YAML
  ...
    my-metric:
      ...
      beacon-requirements:
        - field: rt.quit
          requirement: NOT_EXISTS
```
In this example, `my-metric` will only be recorded if the field `rt.quit` does not exist in the beacon.
Alternatively you can use the `requirement` `EXISTS` to make sure the metric is only recorded if the given field is present.

Additionally you can specify that you only want to record the metric if the received beacon has a specific initiator:

```YAML
  ...
    my-metric:
      ...
      beacon-requirements:
        - initiators: [SPA_SOFT, SPA_HARD]
          requirement: HAS_INITIATOR
```

The available initiators are `DOCUMENT` for the initial pageload beacons, `XHR` for Ajax-Beacons and `SPA_SOFT` and `SPA_HARD` for soft and hard SPA navigation beacons.
The metric will only be recorded for beacons whose `http.initiator` field matches any of the elements provided in the `initiators` list.

### Additional Beacon Fields

#### T_OTHER.*

The `t_other.*` fields are a special set of fields which are resolved based on the content of the beacon's `t_other` field.

The Boomerang agent allows to set custom-timer data, which represents a arbitrary key-value pair. The key represents the name of the timer and the value the timer's value which may be a duration or any number. This can be used by applications to measure custom durations or events. See the [Boomerang's documentation](https://developer.akamai.com/tools/boomerang/#BOOMR.sendTimer(name,value)) for more information.

When using custom timers, Boomerang combines their values as a comma-separated list and sends them in the `t_other` attribute. For example, a beacon can be structured as follows: `t_other=t_domloaded|437,boomerang|420,boomr_fb|252`

In order to use the custom-timer data for metrics or tags, the EUM server will split the timer data contained in the `t_other` field into separate fields. The syntax of the resulting fields is: `t_other.[CUSTOM_TIMTER_NAME]`. These resulting fields can be used, for example, as a value input for metrics.

##### Example

A `t_other` field containing `t_domloaded|437,boomr_fb|252` will produce the following results:

* `t_other.t_domloaded` = `437`
* `t_other.boomr_fb` = `252`

#### CLIENT.HEADER.*

The `client.header.*` fields are resolved based on the headers of the beacons request.
Each header will be available at a new attribute with the `client.header` prefix in the beacon.
Note: the capitalisation of the header name is preserved!

##### Example

Assuming the request for sending a beacon to the EUM server contains the header `Accept-Encoding: gzip,deflate` and `Connection: keep-alive`.
In this case, the following beacon properties will be generated and accessible via the configuration:

- `client.header.Accept-Encoding` with value `gzip,deflate`
- `client.header.Connection` with value `keep-alive`

#### RT.BMR.*

The `rt.bmr.*` fields are resolved based on the content of the beacon's `rt.bmr` field.
See the [Boomerang's documentation](https://developer.akamai.com/tools/boomerang/docs/BOOMR.plugins.RT.html) for more information.

The `rt.bmr` parameter consists of a comma-separated list with the following resource timing information for Boomerang itself:

- `startTime`
- `responseEnd`
- `responseStart`
- `requestStart`
- `connectEnd`
- `secureConnectionStart`
- `connectStart`
- `domainLookupEnd`
- `domainLookupStart`
- `redirectEnd`
- `redirectStart`

##### Example

A `rt.bmr` field containing `123,477,,1` will produce the following results:

- `rt.bmr.startTime` = `123`
- `rt.bmr.responseEnd` = `477`
- `rt.bmr.responseStart` = `0`
- `rt.bmr.requestStart` = `1`


- `rt.bmr.connectEnd` = `0` _*_
- `rt.bmr.secureConnectionStart` = `0` _*_
- ...

_\* All values which does not exist in the parameter will be populated with zero!_

## Tags Definition

We distinguish between to different types of tags:

| Attributes | Note |
| --- | --- |
| `extra` | Extra tags define tags, which are manually set in the configuration and can be considered as constants. |
| `beacon` | Beacon tags define tags, whose value is resolved by an incoming beacon entry. |

For example, the following configuration specifies the two tags `APP` and `URL`.
The tag `APP` will always be resolved to the value `my-application`, where the tag `URL` will be resolved to the value of the field `u` of a received beacon.

```YAML
inspectit-eum-server:
  tags:
    extra:
      APP: my-application
    beacon:
      URL: 
        input: u
```

Tags configured via `beacon` offer some additional flexibility: In addition to simply copying the input value, 
it is possible to perform one or multiple regular expression replacements.

**Example:** in case the `u` attribute contains a URL which is: `http://server/user/100`.
The following configuration can be used to extract the HTTP-Path from it.

```YAML
inspectit-eum-server:
  tags:
    beacon:
      MY_PATH: 
        input: u
        replacements:
         -  pattern:  '^.*\/\/([^\/]*)([^?]*).*$'
            replacement: "$2"
            keep-no-match: false
```
The `replacements` property defines a list of regular expressions and corresponding replacements to apply.
They will be applied in the order they are listed.
For each list element, the `pattern` property defines the regex to use for the replacement.
All matches of the `pattern` in the input value are replaced with the string defined by `replacement`.
The `keep-no-match` option of each entry defines what to do if the given input does not match the given regex at any place.
If it is set to `true`, the previous value is kept unchanged. If it is set to `false`, the given tag won't be created in case no match is found.
Note that capture groups are supported and can be referenced in the replacement string using `$1`, `$2`, etc. as shown in the example.

The following example extends the previous one by additionally replacing all user-IDs within the path:


```YAML
inspectit-eum-server:
  tags:
    beacon:
      MY_PATH: 
        input: u
        replacements:
         -  pattern:  '^.*\/\/([^\/]*)([^?]*).*$'
            replacement: "$2"
            keep-no-match: false
         -  pattern:  '\/user\/\d+'
            replacement: '/user/{id}'
```

With these settings, the tag will be extracted from `u` just like in the previous example.
However, an additional replacement will be applied afterwards causing user-IDs to be erased from the path.
Note that we did not specify `keep-no-match` for the second replacement. `keep-no-match` default to `true`,
meaning that the path will be preserved without any changes in case it does not contain any user-IDs.

Using this mechanism, the EUM server provides the following tags out of the box:

| Tag | Description |
| --- | --- |
| `U_NO_QUERY` | The Boomerang *u* property but without query parameters. Check out [Boomerang](https://developer.akamai.com/tools/boomerang/docs/BOOMR.html).|
| `U_HOST` | The host specified in the *u* property. Check out [Boomerang](https://developer.akamai.com/tools/boomerang/docs/BOOMR.html).|
| `U_PORT` | The port specified in the *u* property. Check out [Boomerang](https://developer.akamai.com/tools/boomerang/docs/BOOMR.html).|
| `U_PATH` | The http path specified in the *u* property. Check out [Boomerang](https://developer.akamai.com/tools/boomerang/docs/BOOMR.html).|
| `PGU_NO_QUERY` | The Boomerang *pgu* property but without query parameters. Check out [Boomerang](https://developer.akamai.com/tools/boomerang/docs/BOOMR.html).|
| `PGU_HOST` | The host specified in the *pgu* property. Check out [Boomerang](https://developer.akamai.com/tools/boomerang/docs/BOOMR.html).|
| `PGU_PORT` | The port specified in the *pgu* property. Check out [Boomerang](https://developer.akamai.com/tools/boomerang/docs/BOOMR.html).|
| `PGU_PATH` | The http path specified in the *pgu* property. Check out [Boomerang](https://developer.akamai.com/tools/boomerang/docs/BOOMR.html).|


### Additional Tags

The EUM server provides a set of additional tags which can be used like all other tag.
See the following sections for a detailed description of the existing additional tags.

#### COUNTRY_CODE
 
The `COUNTRY_CODE` tag contains the geolocation of the beacon's origin. It is resolved by using the client IP and the [GeoLite2 database](https://www.maxmind.com). If the IP cannot be resolved, the tag value will be empty.

##### Custom COUNTRY_CODE Mapping

Besides using the internal GeoLite2 database, it is possible to define custom IP mappings.
The property `custom-ip-mapping` holds a map of possible tag values, which are mapped to certain IPs or CIDRs.
The tag values are published with the tag `COUNTRY_CODE` and have a higher priority than the results of the GeoLite2 database.
If the IP cannot be resolved with the custom mapping, the mapping of the GeoLite2 database will be used.

```YAML
inspectit-eum-server:
  tags:
    custom-ip-mapping:
      department-1:
        - 10.10.0.0/16
        - 11.11.0.3
      department-2:
        - 14.14.0.0/16
        - 14.15.0.1
```

### Global Tags

Tags will not be attached to metrics unless a metric explicitly defines to use a certain tag.

In order to simplify the configuration, it is possible to define tags which are *always* attached to metrics, even a metric does not explicitly specifies it.
This can be achieved by adding a tag's name to the `define-as-global` property.
Each tag which is listed under this property will be added to each registered metric.

For example, the following configuration causes that each metric will be enriched by a tag called `COUNTRY_CODE`.

```YAML
inspectit-eum-server:
  tags:
    define-as-global:
      - COUNTRY_CODE
```

## Resource Timings

:::important
The resource timing processing is enabled by default and can be disabled by setting the property `inspectit-eum-server.resource-timing.enabled` to `false.`
:::

The EUM server can extract information about the resources timings which are reported as part of the Boomerang beacon field `restiming`.
The resource timing information is decompressed from the beacon and exposed as part of the `resource_time` metric.
This metric contains following tags:

| Tag | Description |
| --- | --- |
| `initiatorType` | The type of the element initiating the loading of the resource. See [all initiator types](https://developer.akamai.com/tools/boomerang/docs/BOOMR.plugins.ResourceTiming.html#.INITIATOR_TYPES). |
| `crossOrigin` | If a resource loading is considered as cross-origin request. See [more information about CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS). |
| `cached` | If a resource was cached and loaded from the browser disk or memory storage. Note that cached tag will only be set for same-origin requests, as some resource timing  metrics are restricted and will not be provided cross-origin unless the Timing-Allow-Origin header permits. |

:::note
Please note that all [global tags](#global-tags) will be attached as well. Attaching custom tags works in the same way as [defining metrics](#metrics-definition).
:::

For example, the following configuration causes that the resource timing processing is enabled and will be enriched by a tag called `U_HOST`.

```YAML
inspectit-eum-server:
  resource-timing:
    enabled: true
    tags: 
      U_HOST: true
```

## Exporters

### Metrics

The EUM server comes with the same Prometheus, InfluxDB and OTLP metrics exporter as the Ocelot agent.
The exporter's configurations options are the same as for the [agent](metrics/metric-exporters.md).
However, they are located under the `inspectit-eum-server.exporters.metrics` configuration path.

#### Prometheus
By default, the prometheus exporter is disabled.

The following configuration snippet shows makes the prometheus-exporter exposing the metrics on port `8888`:
```YAML
inspectit-eum-server:
  exporters:
    metrics:
      prometheus:
        # Determines whether the prometheus exporter is enabled.
        enabled: ENABLED

        # The host of the prometheus HTTP endpoint.
        host: localhost

        # The port of the prometheus HTTP endpoint.
        port: 8888
```

#### InfluxDB
By default, the InfluxDB exporter is enabled, but it is not active since the `endpoint`-property is not set. The property can be set via `inspectit-eum-server.exporters.metrics.influx.endpoint`.

The following configuration snippet makes the InfluxDB Exporter send every 15 seconds metrics to an InfluxDB available under `localhost:8086` to the database `inspectit`:
```YAML
inspectit-eum-server:
  exporters:
    metrics:
      influx:
        # Determines whether the InfluxDB exporter is enabled.
        enabled: IF_CONFIGURED

        # the export interval of the metrics.
        export-interval: 15s

        # The http url of influx.
        # If this property is not set, the InfluxDB exporter will not be started.
        endpoint: "http://localhost:8086"

        # The database to write to.
        # If this property is not set, the InfluxDB exporter will not be started.
        database: "inspectit"

        # The username to be used to connect to the InfluxDB.
        # username:

        # The password to be used to connect to the InfluxDB.
        # password:

        # The retention policy to write to.
        # If this property is not set, the InfluxDB exporter will not be started.
        retention-policy: "autogen"

        # If true, the specified database will be created with the autogen retention policy.
        create-database: true

        # If disabled, the raw values of each counter will be written to the InfluxDB on each export.
        # When enabled, only the change of the counter in comparison to the previous export will be written.
        # This difference will only be written if the counter has changed (=the difference is non-zero).
        # This can greatly reduce the total data written to InfluxDB and makes writing queries easier.
        counters-as-differences: true

        # The size of the buffer for failed batches.
        # E.g. if the exportInterval is 15s and the buffer-size is 4, the export will keep up to one minute of data in memory.
        buffer-size: 40
```

#### OTLP (Metrics)
By default, the OTLP exporter is enabled, but is not active as the `endpoint`-property is not set.
The property can be set via `inspectit-eum-server.exporters.metrics.otlp.endpoint`.

The following configuration snipped makes the OTLP exporter send metrics every 15s to an OTLP receiver located at `localhost:4317`:

```yaml
inspectit-eum-server:
  exporters:
    metrics:
      # settings for the OtlpGrpcMetricExporter used in OtlpGrpcMetricExporterService
      otlp:
        enabled: ENABLED
        # the export interval of the metrics
        export-interval: 15s
        # the URL endpoint, e.g., http://127.0.0.1:4317
        endpoint: http://localhost:4317
        # the transport protocol, e.g., 'grpc' or 'http/protobuf'
        protocol: grpc
        # headers
        headers: { }
        # the aggregation temporality, e.g., CUMULATIVE or DELTA
        preferredTemporality: CUMULATIVE
```
### Tracing

:::note
In case Boomerang is used as EUM agent, 
Note that if you use Boomerang as your EUM agent, it will not capture traces by default.
To capture traces with Boomerang, a special tracing plugin must be used.
More information can be found in the chapter on [installing the EUM agent](https://inspectit.github.io/inspectit-ocelot/docs/enduser-monitoring/install-eum-agent#tracing).
:::

#### Trace Exporter
The EUM server comes with the same Jaeger and OTLP trace exporter as the Ocelot agent.
The exporter's configurations options are the same as for the [agent](tracing/trace-exporters.md).
However, they are located under the `inspectit-eum-server.exporters.tracing` configuration path.

##### General Trace Exporter Settings

These settings apply to all trace exporters and can set below the `inspectit-eum-server.exporters.tracing` property.

| Property        | Default                     | Description                                                                                                                                                               |
|-----------------|-----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `.service-name` | `${inspectit.service-name}` | The value of this property will be used to identify the service a trace came from. Please note that changes of this property only take effect after restarting the agent. |

##### Jaeger
InspectIT EUM Server supports thrift and gRPC Jaeger exporter.

By default, the Jaeger exporter is enabled, but it is not active since the `endpoint` property is not set.

The following configuration snippet makes the Jaeger exporter send traces to a Jaeger instance avialable under `localhost:14250`.

```YAML
inspectit-eum-server:
  exporters:
    tracing:
      jaeger:
      # If jaeger exporter for the OT received spans is enabled.
      enabled: ENABLED

      # Location of the jaeger gRPC API.
      # Either a valid NameResolver-compliant URI, or an authority string.
      # If this property is not set, the jaeger-exporter will not be started.
      endpoint: localhost:14250
      # the transport protocol, e.g., 'grpc' or 'http/protobuf'
      protocol: grpc
      # service name for all exported spans.
      service-name: browser-js
```

##### OTLP (tracing)
By default, the OTLP exporter is enabled, but is not active as the `endpoint`-property is not set.
The property can be set via `inspectit-eum-server.exporters.tracing.otlp.endpoint`.

The following configuration snippet makes the OTLP exporter send traces to an OTLP receiver available under `localhost:4317`.
```yaml
inspectit-eum-server:
  exporters:
    tracing:
      otlp:
        # If OTLP exporter for the OT received spans is enabled.
        enabled: ENABLED
        # the URL endpoint, e.g., http://127.0.0.1:4317
        endpoint: localhost:4317
        # the transport protocol, e.g., 'http/thrift' or 'grpc'
        protocol: grpc
```
#### Additional Span Attributes

The EUM server is able to enrich a received span with additional attributes.
Currently, the following attributes are added to **each** span.

| Attribute | Description |
| --- | --- |
| `client.ip` | The sender IP address of the request that sent the spans. This address can be anonymised if required by data protection rules. See [Masking Client IP Addresses](#masking-client-ip-addresses). |

##### Masking Client IP Addresses

The attribute `client.ip` is added to each span, which contains the sender IP address of the request with which the span was sent.
This IP address can be anonymised with the following configuration:

```YAML
inspectit-eum-server:
  exporters:
    tracing:
      # Specifies whether client IP addresses which are added to spans should be masked.
      mask-span-ip-addresses: true
```

If the `mask-span-ip-addresses` setting is set to `true`, the last 8 bits are set to `0` for IPv4 addresses.
For IPv6 addresses, the last 48 bits are set to `0`.

### Beacons

The EUM Server supports that received beacons can be exported or sent to other systems.
Currently only export via HTTP is supported.
In this case, the beacons are sent in JSON format.
This allows the received beacons to be sent to an HTTP endpoint (e.g. Logstash).

The following configuration snippet can be used in the EUM server for enabling beacon exportation via HTTP.

```YAML
inspectit-eum-server:
  exporters:
    beacons:
      http:
        # Whether beacons should be exported via HTTP
        enabled: true
        # The endpoint to which the beacons are to be sent
        endpoint-url: https://localhost:8080
        # The max. amount of threads exporting beacons (min. 1)
        worker-threads: 2
        # The maximum number of beacons to be exported using a single HTTP request (min. 1)
        max-batch-size: 100
        # The flush interval to export beacons in case the 'max-batch-size' has not been reached (min. 1 second)
        flush-interval: 5s
        # When specified, the request will be using this username for Basic authentication
        username: user
        # The password used for Basic authentication
        password: 123
```

The EUM server uses Basic Authentication for the request if a username is specified. Otherwise no authentication is used.

## Self-Monitoring

:::important
Self-Monitoring is enabled by default and can be disabled by setting the property `inspectit-eum-server.self-monitoring.enabled` to `false.`
:::

For the purpose of self-monitoring, the EUM server offers a set of metrics that reflect its state.
These metrics are exposed using its Prometheus endpoint which also is used for the EUM beacon data.
Currently, the following self monitoring metrics are available.

| Metric name | Description |
| --- | --- |
| `inspectit_eum_self_beacons_received_count` | Counts the number of received beacons | 
| `inspectit_eum_self_beacons_export_count` | Counts the number of beacon exports | 
| `inspectit_eum_self_beacons_export_duration_sum` | The total duration needed for beacon exports | 
| `inspectit_eum_self_beacons_export_batch_sum` | The number of exported beacons per export | 
| `inspectit_eum_self_beacons_processor_count` | Counts the number of processed beacons | 
| `inspectit_eum_self_beacons_processor_duration_sum` | The total duration needed for beacon processing | 
| `inspectit_eum_self_traces_received_count` | Counts the number of received traces | 
| `inspectit_eum_self_traces_received_duration_sum` | The total duration needed for trace exports | 
| `inspectit_eum_self_traces_span_size_sum` | The number of exported spans per trace | 

## Security
In order to allow only authorized instead of public access, EUM-Server offers a simple, shared secret solution.

The following configuration snippet shows the default configuration for security.
```yaml
inspectit-eum-server:
  security:
    # Enable/Disable Security
    enabled: false
    # Change name of authentication header if required
    authorization-header: Authorization
    # White list certain urls which will not be secured
    permitted-urls:
      - "/actuator/health"
      - "/boomerang/**"
    auth-provider:
      simple:
        # Enable/Disable Provider
        enabled: false
        # The directory where token files are stored. Empty by default to force users to provide one
        token-directory: ""
        # Flag indicates if token-directory should be watched for changes and tokens reloaded
        # The name of the initial token file. If a name is provided file will be created with an initial token
        default-file-name: "default-token-file.yaml"
        watch: true
        # How often token-directory should be watched for changes
        frequency: 60s
```

### Configuration options

All properties share the common prefix `inspectit-eum-server.security`.

| Property               | Default                                | Description                                                                                                                |
|------------------------|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `enabled`              | `false`                                | If `true` access to EUM-Server is secured.                                                                                 |
| `authorization-header` | `Authorization`                        | The HTTP-Header EUM-Server uses to the fetch the authorization information from.                                           |
| `permitted-urls`       | `/actuator/health` and `/boomerang/**` | A list of whitelisted url patterns. These URLs are publicly available without Authorization.                               |
| `auth-provider`        |                                        | Allows to configure different AuthenticationProviders that EUM-Server supports. <br/>Currently only `simple` is supported. |

Please note that if security is enabled, at least one AuthenticationProvider has to be enabled. Otherwise, all endpoints of EUM-Server are not accessible except for the whitelisted ones.

#### AuthenticationProvider `simple`
The AuthenticationProvider `simple` supports to secure EUM-Server using shared secrets aka tokens.

If this AuthenticationProvider is enabled, every client request needs to contain a known token as HTTP header (see `authorization-header` above). Otherwise, the request is rejected with a `403 - Forbidden` status code.

The known tokens are stored in yaml files below a configurable directory. Each file may contain multiple shared secrets.

Such a token consists of a name and a token value. `name` allows to identify the token. `token` is the actual shared secret.

Example:
```yaml
  # Identifies a token. E.g. you can document which application, person, organization, etc. knows about this token. It has no influence on security.
- name: "Name for first token"
  # The value of the token. If an HTTP-request contains this value (without opening and closing double quotes), access is allowed.
  token: "755f3e71-e43f-4715-bd26-b6e112fd30dd"
  # You man specify as many elements as you like
- name: "Name of other token"
  token: "any token value you like"
```

If this AuthenticationProvider is enabled, a default token file containing a generated token will be created, if that file does not yet exist.

It is possible to add, change and remove tokens without a restart. All files below the token directory will be scanned regularly by default. 

The following table describes the configuration options for the AuthenticationProvider `simple`.

All properties share the common prefix `inspectit-eum-server.security.auth-provider.simple`.

| Property name       | Default                   | Description                                                                                                                                                                  |
|---------------------|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`           | `false`                   | If `true` this AuthenticationProvider is enabled.                                                                                                                            |
| `token-directory`   |                           | The directory where token files are stored. If this authentication provider is enabled this value must not be empty. Otherwise, the application will not start successfully. |
| `default-file-name` | `default-token-file.yaml` | The name of the default token file name.                                                                                                                                     |
| `watch`             | `true`                    | If `true` the token directory is scanned for changes regularly.                                                                                                              |
| `frequency`         | `60s`                     | The frequency how often the token directory is scanned for changes.                                                                                                          |
#### Client configuration
You need to configure your client to use one of the known tokens during requests to EUM-Server.

You should make sure to connect via https. Otherwise, it is trivial to intercept the token.

Example configuration using boomerang.js:
```javascript
BOOMR.init({
  beacon_url: "http://your.target.url",
  // for other beacon_types boomerang never sends an authorization header 
  beacon_type: "POST",
  // this must be one of your known tokens configured in EUM-Server
  beacon_auth_token: "fancy but secret token"
})
```
