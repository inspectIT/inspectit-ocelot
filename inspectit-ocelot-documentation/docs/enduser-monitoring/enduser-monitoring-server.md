---
id: enduser-monitoring-server
title: End User Monitoring Server
sidebar_label: EUM Server
---
This server provides Enduser Monitoring data by using the [OpenCensus](https://github.com/census-instrumentation/opencensus-java) toolkit.

## Metrics
The Ocelot EUM server offers a backend for Javascript monitoring with [Boomerang](https://developer.akamai.com/tools/boomerang/docs/index.html).
Boomerang is a Javascript metrics agent, which is able to capture arbitrary customizable metrics. 

For testing purposes, you can inject the following snipped in your webpage.
This snippet loads the boomerang version shipped with the Ocelot server and sends 
all measured metrics back to it:
```javascript
 <script src="http://[inspectit-eum-server-url]/boomerang/boomerang.js"></script>
 <script src="http://[inspectit-eum-server-url]/boomerang/plugins/rt.js"></script>
 <!-- any other plugins you want to include -->
 <script>
   BOOMR.init({
     beacon_url: "http://[inspectit-eum-server-url]:8080/beacon/"
   });
 </script>
```
All boomerang-specific scripts are exposed by the eum server under `http://{eum-server}/boomerang/**`.

You can access the boomerang main script under: `http://[inspectit-eum-server-url]/boomerang/boomerang.js`.
All boomerang plugins can be downloaded under `http://[inspectit-eum-server-url]/boomerang/plugins/{plugin_name}.js`.
The list of available plugins can be found [here](http://akamai.github.io/boomerang/BOOMR.plugins.html).

Note that this approach is not recommended for production use as the scripts are not minified and are loaded synchronously.

Boomerang recommends that you use a [minified JS containing all your plugins](https://developer.akamai.com/tools/boomerang/docs/tutorial-building.html#asynchronously). 
In addition you should use asynchronous injection as described in the [Boomerang documentation](https://developer.akamai.com/tools/boomerang/docs/index.html).

If enabled, the server exposes the metrics by using the [Prometheus exporter](https://github.com/census-instrumentation/opencensus-java/tree/master/exporters/stats/prometheus).
A tutorial on how to install Prometheus can be found [here](https://opencensus.io/codelabs/prometheus/#0).

## Server Setup
Before starting the server, please build the server by cloning the repository and executing the following command or download the [latest release](https://github.com/inspectIT/inspectit-ocelot/releases).
```bash
$ ./gradlew build
```
Start the server with the following command:
```bash
$ java -jar inspectit-ocelot-eum-{version}.jar
```
By default, the server is starting with the port `8080`. 
You can simply configure the port by using the Java property `-Dserver.port=[port]`:
```bash
$ java -Dserver.port=[port] -jar inspectit-ocelot-eum-0.0.1-SNAPSHOT.jar
```
Our server is delivered with a default configuration 
supporting the metrics `t_page`, `t_done`, `rt.tstart`, `rt.nstart` and `rt.end` of the Boomerang plugin [RT](https://developer.akamai.com/tools/boomerang/docs/BOOMR.plugins.RT.html).
In order to provide a custom configuration, please set the Java property `-Dspring.config.location=file:[path-to-config]`:

```bash
$ java -Dserver.port=[port] -Dspring.config.location=file:[path-to-config] -jar inspectit-ocelot-eum-0.0.1-SNAPSHOT.jar
```

## Configuration

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

### Metrics Definition

A metric is defined the same way as in the inspectIT Ocelot Java agent. Please see the section [Metrics / Custom Metrics](metrics/custom-metrics.md) for detailed information.

In contrast to the agent's metric definition, the EUM server's metric definition contains additional fields.
These additional fields are the following:

| Attributes | Note |
| --- | --- |
| `value-expression` | An expression used to calculate the measure's value from a beacon. |
| `beacon-requirements` | Requirements which have to be fulfilled by Beacons. Beacons which do not match all requirements will be ignored by this metric definition. |

#### Value Expressions

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

#### Beacon Requirements

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

### Tags Definition

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
      URL: u
```

#### Additional Tags

The EUM server provides a set of additional tags which can be used like all other tags. Currently, the following tags exist:

| Tag | Description |
| --- | --- |
| `COUNTRY_CODE` | Contains the geolocation of the beacon's origin. It is resolved by using the client IP and the [GeoLite2 database](https://www.maxmind.com). If the IP cannot be resolved, the tag value will be empty. |
| `U_NO_QUERY` | The Boomerang *u* property but without query parameters. Check out [Boomerang](https://developer.akamai.com/tools/boomerang/docs/BOOMR.html).|
| `PGU_NO_QUERY` | The Boomerang *pgu* property but without query parameters. Check out [Boomerang](https://developer.akamai.com/tools/boomerang/docs/BOOMR.html).|

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
#### Global Tags

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
The influx exporter is disabled by default and can be enabled by setting the URL via `inspectit-eum-server.exporters.metrics.influx.url`.

## Self-Monitoring

For the purpose of self-monitoring, the EUM server offers a set of metrics that reflect its state.
These metrics are exposed using its Prometheus endpoint which also is used for the EUM beacon data.
Currently, the following self monitoring metrics are available.

| Metric name | Description |
| --- | --- |
| `beacons_received` | Counts the number of received beacons | 