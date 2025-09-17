---
id: eum-server-metrics
title: EUM Server Metrics
sidebar_label: Collecting Metrics
---

The configuration file defines the mapping between the concrete Boomerang metric and a OpenTelemetry metric,
as the following sample configuration file shows:

```yaml
inspectit-eum-server:
  definitions:

    resource_time:
      instrument-type: HISTOGRAM
      value-type: DOUBLE
      value-expression: "{restiming}"
      unit: ms
      views:
        resource_time:
          aggregation: HISTOGRAM
          attributes:
            initiatorType: true
            cached: true
            crossOrigin: true

    page_ready_time:
      instrument-type: HISTOGRAM
      value-type: LONG
      value-expression: "{t_page}"
      unit: ms
      views:
        page_ready_time: { aggregation: HISTOGRAM }

    load_time:
      instrument-type: HISTOGRAM
      value-type: LONG
      value-expression: "{t_done}"
      beacon-requirements:
        - field: rt.quit
          requirement: NOT_EXISTS
      unit: ms
      views:
        load_time: { aggregation: HISTOGRAM }

    calc_load_time:
      instrument-type: HISTOGRAM
      value-type: LONG
      value-expression: "{rt.end} - {rt.tstart}"
      beacon-requirements:
        - field: rt.quit
          requirement: NOT_EXISTS
      unit: ms
      views:
        calc_load_time: { aggregation: HISTOGRAM }

    start_timestamp:
      instrument-type: GAUGE
      value-type: LONG
      value-expression: "{rt.tstart}"
      unit: ms
      views:
        start_timestamp:
          aggregation: LAST_VALUE
          attributes: { APPLICATION: true }

    navigation_start_timestamp:
      instrument-type: GAUGE
      value-type: LONG
      value-expression: "{rt.nstart}"
      unit: ms
      views:
        navigation_start_timestamp:
          aggregation: LAST_VALUE
          attributes: { APPLICATION: true }

    end_timestamp:
      instrument-type: GAUGE
      value-type: LONG
      value-expression: "{rt.end}"
      unit: ms
      views:
        end_timestamp:
          aggregation: LAST_VALUE
          attributes: { APPLICATION: true }

  attributes:
    extra:
      APPLICATION: my-application
    beacon:
      URL:
        input: u
        null-as-empty: true
      OS:
        input: ua.plt
        null-as-empty: true
    global:
      - URL
      - OS
      - COUNTRY_CODE

  exporters:
    metrics:
      otlp:
        enabled: ENABLED
        protocol: grpc
        endpoint: localhost:4317
```

## Metrics Definition

A metric is defined the same way as in the inspectIT Ocelot Java agent.
Please see the section [Metrics / Custom Metrics](metrics/custom-metrics.md) for detailed information.

In contrast to the agent's metric definition, the EUM server's metric definition contains additional fields.
These additional fields are the following:

| Attributes            | Note                                                                                                                                       |
|-----------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `value-expression`    | An expression used to calculate the measure's value from a beacon.                                                                         |
| `beacon-requirements` | Requirements which have to be fulfilled by Beacons. Beacons which do not match all requirements will be ignored by this metric definition. |

### Value Expressions

The `value-expression` field can be used to specify a field which value is used for the specified metrics.
In order to reference a field, the following pattern is used: `{FIELD_KEY}`.
For example, a valid expression, used to extract the value of a field `t_load`, would be `{t_load}`.

:::note
Note that a beacon has to contain all fields referenced by the expression in order to be evaluated and recorded.
:::

Value expressions also support operations for basic arithmetic operations. Thus, to calculate a difference of
two beacon fields, the following expression can be used:

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

The `beacon-requirements` field can be used to specify requirements which have to be fulfilled by the beacons in order
to be evaluated by a certain metric.
If any requirement does not fit a beacon, the beacon is ignored by the metric.

The following requirements are available:

| Type            | Note                                                  |
|-----------------|-------------------------------------------------------|
| `EXISTS`        | The targeted field must exist.                        |
| `NOT_EXISTS`    | The targeted field must not exist.                    |
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

The available initiators are `DOCUMENT` for the initial page load beacons, `XHR` for Ajax-Beacons and `SPA_SOFT` and `SPA_HARD`
for soft and hard SPA navigation beacons.
The metric will only be recorded for beacons whose `http.initiator` field matches any of the elements provided in the `initiators` list.

### Additional Beacon Fields

#### T_OTHER.*

The `t_other.*` fields are a special set of fields which are resolved based on the content of the beacon's `t_other` field.

The Boomerang agent allows to set custom-timer data, which represents a arbitrary key-value pair. The key represents
the name of the timer and the value the timer's value which may be a duration or any number. This can be used by
applications to measure custom durations or events. See the [Boomerang's documentation](https://akamai.github.io/boomerang/akamai/BOOMR.html#.sendTimer__anchor) for more information.

When using custom timers, Boomerang combines their values as a comma-separated list and sends them in the `t_other` attribute.
For example, a beacon can be structured as follows: `t_other=t_domloaded|437,boomerang|420,boomr_fb|252`

In order to use the custom-timer data for metrics or attributes, the EUM server will split the timer data contained in the `t_other`
field into separate fields. The syntax of the resulting fields is: `t_other.[CUSTOM_TIMTER_NAME]`. These resulting fields
can be used, for example, as a value input for metrics.

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
See the [Boomerang's documentation](https://akamai.github.io/boomerang/akamai/BOOMR.plugins.RT.html) for more information.

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

## Attributes Definition

We distinguish between to different types of attributes:

| Attributes | Note                                                                                                                |
|------------|---------------------------------------------------------------------------------------------------------------------|
| `extra`    | Extra attributes define attributes, which are manually set in the configuration and can be considered as constants. |
| `beacon`   | Beacon attributes define attributes, whose value is resolved by an incoming beacon entry.                           |

For example, the following configuration specifies the two attributes `APP` and `URL`.
The attribute `APP` will always be resolved to the value `my-application`, where the attribute `URL` will be resolved to the value
of the field `u` of a received beacon. Additionally, if the field `u` is null, an empty string will be used instead.

```YAML
inspectit-eum-server:
  attributes:
    extra:
      APP: my-application
    beacon:
      URL: 
        input: u
        null-as-empty: true
```

Attributes configured via `beacon` offer some additional flexibility: In addition to simply copying the input value,
it is possible to perform one or multiple regular expression replacements.

**Example:** in case the `u` attribute contains a URL which is: `http://server/user/100`.
The following configuration can be used to extract the HTTP-Path from it.

```YAML
inspectit-eum-server:
  attributes:
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
If it is set to `true`, the previous value is kept unchanged. If it is set to `false`, the given attribute won't be created in case no match is found.
Note that capture groups are supported and can be referenced in the replacement string using `$1`, `$2`, etc. as shown in the example.

The following example extends the previous one by additionally replacing all user-IDs within the path:


```YAML
inspectit-eum-server:
  attributes:
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

With these settings, the attribute will be extracted from `u` just like in the previous example.
However, an additional replacement will be applied afterward causing user-IDs to be erased from the path.
Note that we did not specify `keep-no-match` for the second replacement. `keep-no-match` default to `true`,
meaning that the path will be preserved without any changes in case it does not contain any user-IDs.

Using this mechanism, the EUM server provides the following attributes out of the box:

| Attribute      | Description                                                                                                                             |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `U_NO_QUERY`   | The Boomerang *u* property but without query parameters. Check out [Boomerang](https://akamai.github.io/boomerang/akamai/BOOMR.html).   |
| `U_HOST`       | The host specified in the *u* property. Check out [Boomerang](https://akamai.github.io/boomerang/akamai/BOOMR.html).                    |
| `U_PORT`       | The port specified in the *u* property. Check out [Boomerang](https://akamai.github.io/boomerang/akamai/BOOMR.html).                    |
| `U_PATH`       | The http path specified in the *u* property. Check out [Boomerang](https://akamai.github.io/boomerang/akamai/BOOMR.html).               |
| `PGU_NO_QUERY` | The Boomerang *pgu* property but without query parameters. Check out [Boomerang](https://akamai.github.io/boomerang/akamai/BOOMR.html). |
| `PGU_HOST`     | The host specified in the *pgu* property. Check out [Boomerang](https://akamai.github.io/boomerang/akamai/BOOMR.html).                  |
| `PGU_PORT`     | The port specified in the *pgu* property. Check out [Boomerang](https://akamai.github.io/boomerang/akamai/BOOMR.html).                  |
| `PGU_PATH`     | The http path specified in the *pgu* property. Check out [Boomerang](https://akamai.github.io/boomerang/akamai/BOOMR.html).             |


### Additional Attributes

The EUM server provides a set of additional attributes which can be used like all other attributes.
See the following sections for a detailed description of the existing additional attributes.

#### COUNTRY_CODE

The `COUNTRY_CODE` attribute contains the geolocation of the beacon's origin.
It is resolved by using the client IP and the [GeoLite2 database](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data/). If the IP cannot be resolved, the attribute value will be empty.

##### Custom COUNTRY_CODE Mapping

Besides using the internal GeoLite2 database, it is possible to define custom IP mappings.
The property `custom-ip-mapping` holds a map of possible attribute values, which are mapped to certain IPs or CIDRs.
The attribute values are published with the attribute `COUNTRY_CODE` and have a higher priority than the results of the GeoLite2 database.
If the IP cannot be resolved with the custom mapping, the mapping of the GeoLite2 database will be used.

```YAML
inspectit-eum-server:
  attributes:
    custom-ip-mapping:
      department-1:
        - 10.10.0.0/16
        - 11.11.0.3
      department-2:
        - 14.14.0.0/16
        - 14.15.0.1
```

### Global Attributes

Attributes will not be attached to metrics unless a metric explicitly defines to use a certain attribute.

In order to simplify the configuration, it is possible to define attributes which are *always* attached to metrics, even a metric does not explicitly specifies it.
This can be achieved by adding a attribute's name to the `define-as-global` property.
Each attribute which is listed under this property will be added to each registered metric.

For example, the following configuration causes that each metric will be enriched by a attribute called `COUNTRY_CODE`.

```YAML
inspectit-eum-server:
  attributes:
    define-as-global:
      - COUNTRY_CODE
```

## Resource Timings

The EUM server can extract information about the resources timings which are reported as part of the Boomerang beacon field `restiming`.
The resource timing information is decompressed from the beacon and exposed as part of the `resource_time` metric.
This metric contains following attributes:

| Attribute       | Description                                                                                                                                                                                                                                                                       |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `initiatorType` | The type of the element initiating the loading of the resource. See [all initiator types](https://akamai.github.io/boomerang/akamai/BOOMR.plugins.ResourceTiming.html#.INITIATOR_TYPES__anchor).                                                                                  |
| `crossOrigin`   | If a resource loading is considered as cross-origin request. See [more information about CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS).                                                                                                                           |
| `cached`        | If a resource was cached and loaded from the browser disk or memory storage. Note that cached tag will only be set for same-origin requests, as some resource timing  metrics are restricted and will not be provided cross-origin unless the Timing-Allow-Origin header permits. |

:::note
Please note that all [global attributes](#global-attributes) will be attached as well. 
Attaching custom attributes works in the same way as [defining metrics](#metrics-definition).
:::

For example, the following configuration causes that the resource timing processing is enabled and will be enriched 
by a attribute called `U_HOST`.

```YAML
inspectit-eum-server:
  resource-timing:
    enabled: true
    attributes: 
      U_HOST: true
```

## Exporters

The EUM server comes with the same Prometheus and OTLP metrics exporter as the Ocelot agent.
The exporter's configurations options are the same as for the [agent](metrics/metric-exporters.md).
However, they are located under the `inspectit-eum-server.exporters.metrics` configuration path.

### Prometheus
By default, the prometheus exporter is disabled.

The following configuration snippet shows how to make the prometheus-exporter expose the metrics on port `8888`:

```YAML
inspectit-eum-server:
  exporters:
    metrics:
      prometheus:
        # Determines whether the prometheus exporter is enabled
        enabled: ENABLED

        # The host of the prometheus HTTP endpoint
        host: localhost

        # The port of the prometheus HTTP endpoint
        port: 8888
```

### OTLP

By default, the OTLP exporter is enabled, but is not active as the `endpoint`-property is not set.
The property can be set via `inspectit-eum-server.exporters.metrics.otlp.endpoint`.

The following configuration snipped makes the OTLP exporter send metrics every 15s to an OTLP receiver located at `localhost:4317`:

```yaml
inspectit-eum-server:
  exporters:
    metrics:
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

The EUM server uses Basic Authentication for the request if a username is specified. Otherwise, no authentication is used.

## Self-Monitoring

:::important
Self-Monitoring is enabled by default and can be disabled by setting the property `inspectit-eum-server.self-monitoring.enabled` to `false.`
:::

For the purpose of self-monitoring, the EUM server offers a set of metrics that reflect its state and are recorded automatically.
Currently, the following self monitoring metrics are available:

| Metric name                               | Description                                                |
|-------------------------------------------|------------------------------------------------------------|
| `inspectit_eum_self_beacons_received`     | The total number of received beacons                       | 
| `inspectit_eum_self_beacons_export`       | Histogram for exporting beacons with count, sum & buckets  | 
| `inspectit_eum_self_beacons_export_batch` | The total number of exported beacons per batch             | 
| `inspectit_eum_self_beacons_processor`    | Histogram for processing beacons with count, sum & buckets | 
| `inspectit_eum_self_traces_received`      | Histogram for receiving traces with count, sum & buckets   | 
| `inspectit_eum_self_traces_span_size`     | The total number of exported spans per trace               | 
