---
id: enduser-monitoring-server
title: End User Monitoring Server
---
This server provides Enduser Monitoring data by using the [OpenCensus](https://github.com/census-instrumentation/opencensus-java) toolkit.

## Metrics
The inspectit-ocelot server offers a backend for Javascript monitoring with [Boomerang](https://developer.akamai.com/tools/boomerang/docs/index.html).
Boomerang is a Javascript metrics agent, which is able to capture arbitrary customizable metrics. 
By injecting the following snipped in your webpage, all measured metrics are sent to the inspectit-ocelot-eum-server:
```javascript
<script src="boomerang-1.0.0.min.js"></script>
 <script src="plugins/rt.js"></script>
 <!-- any other plugins you want to include -->
 <script>
   BOOMR.init({
     beacon_url: "http://[inspectit-eum-server-url]:8080/beacon/"
   });
 </script>
```
Boomerang recommends to use an advanced injection, where the boomerang agent is loaded in an asynchronous way. 
For further information, please visit the [Boomerang documentation](https://developer.akamai.com/tools/boomerang/docs/index.html).

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
inspectit-ocelot-eum-server:
  definitions:
    page_ready_time:
      measure-type: LONG
      beacon-field: t_page
      unit: ms
      views:
        '[page_ready_time/SUM]': {aggregation: SUM}
        '[page_ready_time/COUNT]': {aggregation: COUNT}
    load_time:
      measure-type: LONG
      beacon-field: t_done
      unit: ms
      views:
        '[load_time/SUM]': {aggregation: SUM}
        '[load_time/COUNT]': {aggregation: COUNT}

    start_timestamp:
      measure-type: LONG
      beacon-field: rt.tstart
      unit: ms

    navigation_start_timestamp:
      measure-type: LONG
      beacon-field: rt.nstart
      unit: ms

    end_timestamp:
      measure-type: LONG
      beacon-field: rt.end
      unit: ms
      views:
        end_timestamp:
          aggregation: LAST_VALUE
          tags: {APPLICATION : true}
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

  exporters:
    metrics:
      prometheus:
        enabled: true
        host: localhost
        port: 8888
```

### Metrics Definition

A metric are defined the same way as in the inspectIT Ocelot Java agent. Please see the section [Metrics / Custom Metrics](metrics/custom-metrics.md) for detailed information.

In contrast to the agent's metric definition, the EUM server's metric definition contains additional fields.
These additional fields are the following:

| Attributes | Note |
| --- | --- |
| `beacon-field` | The beacon field key, which is used as value for the metric. |

### Tags Definition

We distinguish between to different types of tags:

| Attributes | Note |
| --- | --- |
| `extra` | Extra tags define tags, which are manually set in the configuration and can be considered as constants. |
| `beacon` | Beacon tags define tags, whose value is resolved by an incoming beacon entry. |

For example, the following configuration specifies the two tags `APP` and `URL`.
The tag `APP` will always be resolved to the value `my-application`, where the tag `URL` will be resolved to the value of the field `u` of a received beacon.

```YAML
inspectit:
  tags:
    extra:
      APP: my-application
    beacon:
      URL: u
```

#### Default Tags

The EUM server provides a set of default tags which don't have to be specified and always exist. Currently, the following default tags exist:

| Tag | Description |
| --- | --- |
| `COUNTRY_CODE` | Contains the geolocation of the beacon's origin. It is resolved by using the client IP and the [GeoLite2 database](https://www.maxmind.com). If the IP cannot be resolved, the tag value will be empty. |

#### Global Tags

Tags will not be attached to metrics unless a metric explicitly defines to use a certain tag.

In order to simplify the configuration, it is possilbe to define tags which are *always* attached to metrics, even a metric does not explicitly specifies it.
This can be achieved by adding a tag's name to the `define-as-global` property.
Each tag which is listed under this property will be added to each registered metric.

For example, the following configuration causes that each metric will be enriched by a tag called `COUNTRY_CODE`.

```YAML
inspectit:
  tags:
    define-as-global:
      - COUNTRY_CODE
```

## Exporters
By now, the prometheus exporter is available. If `enabled` is set to true, the exporter is exposes the metrics under the following HTTP endpoint: `http://[host]:[port]/metrics`
