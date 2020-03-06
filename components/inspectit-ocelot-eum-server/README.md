# inspectit-ocelot-eum-server
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
inspectit-eum-server:
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
    global:
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
##### Metrics Definition
A metric is defined through the following attributes:
* `name`: Defines the name of the metric. The name of the exposed view will have the used aggregation as suffix.
* `measure-type`: Can be either `LONG` or `DOUBLE`.
* `beacon-field`: The beacon key name, which is used as source of metric.
* `description`: Optional. Defines an additional description of the exposed metric.
* `unit`: The unit of the metric.
* `tag-keys`: Optional. Defines a list of tag keys, which are exposed with the current metric.
* `views`: A list of the views, which should be exposed. The aggregation can be either `SUM`, `COUNT`, `LAST_VALUE` or `HISTORGRAM`. For using `HISTOGRAM`, the field `bucket-boundaries` is mandatory.
* `bucket-boundaries`: Used for the `HISTOGRAM` aggregation, defines the bucket boundaries as list of Doubles.

##### Tags Definition
We distinguish between to different types of tags:
* `extra`- tags: Extra tags define tags, which are manually set in the configuration. The field `extra` holds a list of key-value mappings.
* `beacon`- tags: Beacon tags define tags, whose tag value is resolved by a beacon entry. The defined value of the `beacon` map will be resolved by using the provided beacon.
In order to provide selected tags to each measurement by default, tags can be defined as global. `global` holds a list of already defined tags, which will be then exposed for each measurement.

##### Automated Geolocation Detection
By using the tag `COUNTRY_CODE`, the geolocation of the requester is resolved by using the requester IP and the [GeoLite2 database](https://www.maxmind.com). If the IP cannot be resolved, the tag value will be empty.

##### Exporters
By now, the prometheus exporter is available. If `ènabled` is set to true, the exporter is exposes the metrics under 
```bash
http://[host]:[port]/metrics
```
