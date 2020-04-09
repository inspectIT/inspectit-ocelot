---
id: version-1.2-eum-server-configuration
title: EUM Server and Metrics Configuration
sidebar_label: Server and Metrics Configuration
original_id: eum-server-configuration
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
```

## Metrics Definition

A metric is defined the same way as in the inspectIT Ocelot Java agent. Please see the section [Metrics / Custom Metrics](metrics/custom-metrics.md) for detailed information.

In contrast to the agent's metric definition, the EUM server's metric definition contains additional fields.
These additional fields are the following:

| Attributes | Note |
| --- | --- |
| `value-expression` | An expression used to calculate the measure's value from a beacon. |
| `beacon-requirements` | Requirements which have to be fulfilled by Beacons. Beacons which do not match all requirements will be ignored by this metric definition. |

### Value Expressions

The `value-expression` field can be used to specify a field which value is used for the specified metrics.
In order to reference a field, the following pattern is used: `{FIELD_KEY}`.
For example, a valid expression, used to extract the value of a field `t_load`, would be `{t_load}`.

> Note that a beacon has to contain all fields referenced by the expression in order to be evaluated and recorded.

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

Beacon requirements consist of two attributes `field` and `requirement`. `field` specified the beacon's field which is validated using the requirement type specified in `NOT_EXISTS`.

```YAML
  ...
    my-metric:
      ...
      beacon-requirements:
        - field: rt.quit
          requirement: NOT_EXISTS
```

The following requirement types are currently be supported:

| Type | Note |
| --- | --- |
| `EXISTS` | The targeted field must exist. |
| `NOT_EXISTS` | The targeted field must not exist. |

### Additional Beacon Fields

#### T_OTHER.*

The `t_other.*` fields are a special set of fields which are resolved based on the content of the beacon's `t_other` field.

The Boomerage agent allows to set custom-timer data, which represents a arbitrary key-value pair. The key represents the name of the timer and the value the timer's value which may be a duration or any number. This can be used by applications to measure custom durations or events. See the [Boomerang's documentation](https://developer.akamai.com/tools/boomerang/#BOOMR.sendTimer(name,value)) for more information.

When using custom timers, Boomerang combines their values as a comma-separated list and sends them in the `t_other` attribute. For example, a beacon can be structured as follows: `t_other=t_domloaded|437,boomerang|420,boomr_fb|252`

In order to use the custom-timer data for metrics or tags, the EUM server will split the timer data contained in the `t_other` field into separate fields. The syntax of the resulting fields is: `t_other.[CUSTOM_TIMTER_NAME]`. These resulting fields can be used, for example, as a value input for metrics.

##### Example

A `t_other` field containing `t_domloaded|437,boomr_fb|252` will produce the following results:

* `t_other.t_domloaded` = `437`
* `t_other.boomr_fb` = `252`

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

Tags configured via `beacon` offer some additional flexibility: In addition to simply copying the input value, it is possible to perform a RegEx replacement.

**Example:** in case the `u` attribute contains a URL which is: `http://server/user/100`.
The following configuration can be used to erases the path segment after `/user/` which represents a user ID and replaces it with the constant text `{id}`.

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

The `regex` property defines the regex to use for the replacement.
All matches of the `regex` in the input value are replaced with the string defined by `replacement`.
The `keep-no-match` options defines what to do if the given input does not match the given regex at any place.
If it is set to `true`, the original value will be kept. If it is set to `false`, the given tag won't be created in case no match is found.

Note that capture groups are supported and can be referenced in the replacement string using `$1`, `$2`, etc.
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

## Exporters

The EUM server comes with the same Prometheus and InfluxDB exporter as the Ocelot agent.
The exporter's configurations options are the same as for the [agent](metrics/metric-exporters.md).
However, they are located under the `inspectit-eum-server.exporters.metrics` configuration path.

By default, the prometheus exporter is enabled and available on port `8888`.
The InfluxDB exporter is disabled by default and can be enabled by setting the URL via `inspectit-eum-server.exporters.metrics.influx.url`.

## Self-Monitoring

For the purpose of self-monitoring, the EUM server offers a set of metrics that reflect its state.
These metrics are exposed using its Prometheus endpoint which also is used for the EUM beacon data.
Currently, the following self monitoring metrics are available.

| Metric name | Description |
| --- | --- |
| `beacons_received` | Counts the number of received beacons | 