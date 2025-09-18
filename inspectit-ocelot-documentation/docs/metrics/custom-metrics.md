---
id: custom-metrics
title: Custom Metrics
---

One of the main benefits of inspectIT Ocelot is that it gives you complete
freedom in what data you want to capture and how you want to capture it.
You can easily define custom performance and business metrics depending on your needs.

In this section we explain how you can define custom OpenTelemetry [metrics](https://opentelemetry.io/docs/concepts/signals/metrics/)
and [views](https://opentelemetry.io/docs/concepts/signals/metrics/#views). We only show how to define the structure of both,
not how the data is collected. For details on the data collection please see the [instrumentation section](instrumentation/metrics.md).

## Configuration

All metrics are defined under the `inspectit.metrics.definitions` property.
This is best explained by giving a simple example which comes from the default
configuration of inspectIT Ocelot:

```YAML
inspectit:
  metrics:
    definitions:
      '[method/duration]':
        instrument-type: HISTOGRAM
        unit: ms
        description: 'the duration from method entry to method exit'
        views:
          '[method/duration]':
            aggregation: HISTOGRAM
            tags:
              method-name: true
```

This snippet defines a metric and view with the name `method/duration`.
The view will store all observed method durations as histogram, which exposes multiple calculations like
count, sum, min or max.
By using the sum and count values, we will be able to also compute the average durations, for example for creating dashboards.

### Metrics

The configuration options given above are only a subset of the available options. The full set of options for metrics is shown below:

| Config Property   | Default                          | Description                                                                                                               |
|-------------------|----------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `enabled`         | `true`                           | When a metric is not enabled, it and all of it's views will *not* be registered in OpenTelemetry.                         |
| `unit`            | -                                | *Required*. A textual representation of the unit of the data represented by the metric.                                   |
| `description`     | Generated based on name and unit | A textual description of the purpose of the metric.                                                                       |
| `instrument-type` | `GAUGE`                          | Specifies which OpenTelemetry instrument to use for recording value (`COUNTER`, `UP_DOWN_COUNTER`, `GAUGE`, `HISTOGRAM`). |
| `value-type`      | `DOUBLE`                         | Specifies whether the metric data is given as integer (`LONG`) or floating point number (`DOUBLE`).                       |
| `views`           | -                                | A list of view, which should be exposed. If no view are specified, OpenTelemetry will create default views automatically. |


### Views

All configuration options for customizing views are given below:

| Config Property          | Default                                                              | Description                                                                                                                                                                                                                                                                                                                   |
|--------------------------|----------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`                | `true`                                                               | When set to `false`, the view will not be registered in OpenTelemetry.                                                                                                                                                                                                                                                        |
| `description`            | Generated based on name and aggregation                              | A textual description of the purpose of this view.                                                                                                                                                                                                                                                                            |
| `aggregation`            | `LAST_VALUE`                                                         | Specifies how the metric data is aggregated in this view. There are OTel aggregations (`SUM`, `LAST_VALUE`, `HISTORGRAM`; `EXPONENTIAL_HISTOGRAM`) as well as custom aggregations (`QUANTILES`, `SMOOTHED_AVERAGE`).                                                                                                          |
| `attributes`             | `{}`                                                                 | Specifies which attributes should be used for this view. `attributes` is a map containing attribute names as key and either `true` or false as value. For example the value `{service.name: false, my_attr: true}` would remove the common attribute `service.name` from the view and add the user attribute `my_attr` to it. |
| `with-common-attributes` | `true`                                                               | If true, all [common tags](metrics/common-tags.md) will be used for this view. Individual attributes can still be disabled via the `attributes` option.                                                                                                                                                                       |
| `cardinality-limit`      | `2000`                                                               | Specifies the maximum amount of unique combinations of attributes for this view.                                                                                                                                                                                                                                              |
| `bucket-boundaries`      | `[0,5,10,25,50,75,100,250,500,`<br/>`750,1000,2500,5000,7500,10000]` | *Required if aggregation is `HISTOGRAM`.* A list of the boundaries of the histogram buckets. E.g. `[7.5,42]` defines three histogram buckets split at `7.5` and `42`.                                                                                                                                                         |
| `max-buckets`            | `160`                                                                | *Required if aggregation is `EXPONENTIAL_HISTOGRAM`.* The max number of positive buckets and negative buckets                                                                                                                                                                                                                 |
| `max-scale`              | `20`                                                                 | *Required if aggregation is `EXPONENTIAL_HISTOGRAM`.* The maximum and initial scale.                                                                                                                                                                                                                                          |

:::note
Due to a limitation of the current OpenCensus library, it is **not possible to remove or alter views and metrics** once they have been registered.
However, you can still add new views and metrics through dynamic configuration updates after the agent has already started.
:::

<!-- TODO Does this still apply with our OpenTelemetry implementation? -->

## Time Windowed Views

inspectIT Ocelot also offers some custom aggregations, which allow to create time windowed views additionally to
OpenTelemetry views. To use time windowed views, the following configuration options extend the [view configuration](#views).
All of them have to be specified when using time windowed views!

| Config Property       | Default                          | Description                                                          |
|-----------------------|----------------------------------|----------------------------------------------------------------------|
| `time-window`         | `${inspectit.metrics.frequency}` | The time window over which the data points are captured.             |
| `max-buffered-points` | `16384`                          | A safety limit defining the maximum number of points to be buffered. |

Note that time-windowed view will always use `GAUGE` as instrument type. the specified `instrument-type` of 
the [metric configuration](#metrics) will be ignored for such views.

### Quantiles View

OpenTelemetry itself does not provide support for computing percentiles of a given metric.
However, the average value alone is not always useful when analyzing response times.
Hereby, the `HISTOGRAM` aggregation can help. Observability backends like Prometheus then allow to interpolate 
percentiles from histogram buckets.
However, it can be very difficult to define the bucket boundaries of the histogram.

For this reason, the inspectIT Ocelot agent contains a custom implemented aggregation type, 
providing the possibility to compute percentiles for any metric on top of OpenTelemetry.

The calculation of percentiles is done by keeping **all raw** observed values for a given metric in memory over a fixed time window.
This time window can be configured using the `time-window` option of the view, which defaults to `15s`.
You can use this feature by setting the `aggregation` of your view to `QUANTILES`.

The percentiles to export can be defined via the additional `quantiles` option within the view:

| Config Property | Default                   | Description                                                     |
|-----------------|---------------------------|-----------------------------------------------------------------|
| `quantiles`     | `[0,0.5,0.9,0.95,0.99,1]` | A list of quantiles between `0` and `1` to capture data points. |

Whenever the recorded metrics are exported, inspectIT Ocelot computes the requested percentiles adhoc based on the 
buffered values of the given metric.
For example, when using the default time window of 15 seconds, the inspectIT Ocelot will expose the percentiles 
of the metric values observed within the last 15 seconds at the point of time when doing the export.
For this reason, `time-window` property should always be **equal or greater** than your metrics scrape or export interval.

:::important
It is important to note that depending on the amount of gathered data the computations of percentiles can be a lot more expensive than just sums or histograms!
To avoid that this feature causes a too high memory footprint, the `max-buffered-points` property exists, limiting the amount of data buffered for a view.
The default value of `16384` was chosen so that the view can handle roughly 1000 data points per second with the default time window of `15s`.
If this limit is exceeded, the percentiles will become meaningless due to data dropping and a warning will be printed in the logs.
:::

#### Collecting Min and Max Values

The `QUANTILES` aggregation of a view also allows the capturing of minimum and maximum values of metrics.
This can be done by using the special quantiles `0` and `1`, which enables the export of the minimum and 
maximum observed value respectively.


As an example, the following snippet defines a metric with the name `load_time` and a view `load_time_quantiles`:
The configuration has the effect of capturing the 50th and 95th percentile for values in a 1 minuted time window.
Additionally, the minimum and maximum values for these time windows will be observed.

```YAML
inspectit:
  metrics:
    definitions:
      load_time:
        unit: ms
        views:
          load_time_quantiles:
              aggregation: QUANTILES
              time-window: 1m
              quantiles: [ 0.0, 0.5, 0.95, 1.0 ]
```

### Smoothed Average View

In contrast to the quantiles, the smoothed-average view allows to drop values of a certain time window. 
This can be useful to deliberately remove outliers before averaging.

:::note
Please read the section on [Quantiles View](#quantiles-view) to get an insight into the data collection. The smoothed-average view is based on the same principle.
:::

The actual [view configuration](#views) is extended by the following properties:

| Config Property       | Default                          | Description                                                                                  |
|-----------------------|----------------------------------|----------------------------------------------------------------------------------------------|
| `drop-upper`          | `0.0`                            | Specifies the percentage of the highest values to be dropped before calculating the average. |
| `drop-lower`          | `0.0`                            | Specifies the percentage of the lowest values to be dropped before calculating the average.  |

As an example, the following snippet defines a metric with the name `load_time` and a view `load_time_smoothed`:
The configuration has the effect of ordering the values in a 1-minute time window by size and dropping the upper 
10 percent before calculating the average.

```YAML
inspectit:
  metrics:
    definitions:
      load_time:
        unit: ms
        views:
          load_time_smoothed:
              aggregation: SMOOTHED_AVERAGE
              time-window: 1m
              drop-upper: 0.1
              drop-lower: 0.0
```
