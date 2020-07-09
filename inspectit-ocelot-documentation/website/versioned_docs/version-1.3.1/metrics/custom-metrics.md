---
id: version-1.3.1-custom-metrics
title: Custom Metrics
original_id: custom-metrics
---

One of the main benefits of inspectIT Ocelot is that it gives you complete
freedom in what data you want to capture and how you want to capture it.
You can easily define custom performance and business metrics depending on your needs.

In this section we explain how you can define custom OpenCensus [metrics](https://opencensus.io/stats/)
and [views](https://opencensus.io/stats/view/). We only show how to define the structure of both,
not how the data is collected. For details on the data collection please see the [instrumentation section](instrumentation/rules.md#collecting-metrics).

## Configuration

All metrics are defined under the `inspectit.metrics.definitions` property.
This is best explained by giving a simple example which comes from the default
configuration of inspectIT Ocelot:

```YAML
inspectit:
  metrics:
    definitions:
      '[method/duration]':
        unit: ms
        description: 'the duration from method entry to method exit'
        views:
          '[method/duration/sum]':
            aggregation: SUM
            tags:
              'method_name' : true
          '[method/duration/count]':
            aggregation: COUNT
            tags:
              'method_name': true
```

This snippet defines a metric with the name `method/duration` and the two views `method/duration/sum`
and `method/duration/count`. Note that the special quoting syntax `'[...]'` is required because of
the slashes in the names. As the name suggests, the first view stores the total sum of all observed
response times whereas the second one stores the number of observations. These two views therefore allow
us to compute the average, for example for creating dashboards.

The configuration options given above are only a subset of the available options. The full set of options for metrics is shown below:

|Config Property|Default| Description
|---|---|---|
|`enabled`|`true`|When a metric is not enabled, it and all of it's views will *not* be registered at the OpenCensus library.
|`unit`|-|*Required*. A textual representation of the unit of the data represented by the metric.
|`description`| Generated based on name and unit| A textual description of the purpose of the metric.
|`type`|`DOUBLE`|Specifies whether the metric data is given as integer (`LONG`) or floating point number (`DOUBLE`).

For a metric to be exposed it is necessary to define [views](https://opencensus.io/stats/view/) in OpenCensus.
This can be done through the `views` config property shown in the sample YAML above.

If you do not explicitly define any view for a metric you defined, inspectIT Ocelot will automatically generate one:
The view will have the same name as the metric and will simply expose the last recorded value.
As soon as you specify at least one view for a metric yourself, inspectIT will not add the default view.

All configuration options for customizing views are given below:

|Config Property|Default| Description
|---|---|---|
|`enabled`|`true`|When set to `false`, the view will not be registered at the OpenCensus library.
|`description`| Generated based on name and aggregation| A textual description of the purpose of this view.
|`aggregation`|`LAST_VALUE`|Specifies how the measurement data is aggregated in this view. Possible values are `LAST_VALUE`, `COUNT`, `SUM`, `HISTOGRAM` and `QUANTILES`. Except for `QUANTILES`, these correspond to the [OpenCensus Aggregations](https://opencensus.io/stats/view/#aggregations).
|`with-common-tags`| `true` | If true, all [common tags](metrics/common-tags.md) will be used for this view. Individual tags can still be disabled via the `tags` option.
|`tags`| `{}` | Specifies which tags should be used for this view. `tags` is a map containing tag names as key and either `true` or false as value. For example the value `{service: false, my_tag: true}` would remove the common tag `service` from the view and add the user tag `my_tag` to it.
|`bucket-boundaries`|-| *Required if aggregation is `HISTOGRAM`.* A list of the boundaries of the histogram buckets. E.g. `[7.5,42]` defines three histogram buckets split at `7.5` and `42`.
|`quantiles`|`[0, 0.5, 0.9, 0.95, 0.99, 1]`| *Required if aggregation is `QUANTILES`.* A list of quantiles to capture - see the section below for details.
|`time-window`|`${inspectit.metrics.frequency}`| *Required if aggregation is `QUANTILES`.* The time window over which the quantiles are captured.
|`max-buffered-points`|`16384`| *Required if aggregation is `QUANTILES`.* A safety limit defining the maximum number of points to be buffered.

:::note
Due to a limitation of the current OpenCensus library, it is **not possible to remove or alter views and metrics** once they have been registered.
However, you can still add new views and metrics through dynamic configuration updates after the agent has already started.
:::

## Quantile Views

OpenCensus itself does not provide support for computing quantiles or the minimum and maximum value of a given metric.
However, the average value alone is not always useful when analyzing response times.
Hereby, the `HISTOGRAM` aggregation can help, however, it can be very difficult to define the boundaries of the histogram.
For this reason, the inspectIT Ocelot agent contains a custom implemented aggregation type, providing the possibility to compute quantiles for any metric on top of OpenCensus.

The calculation of quantiles is done by keeping **all** observed values for a given metric in memory over a fixed time window.
This time window can be configured using the `time-window` option of the view, which defaults to `15s`.
You can use this feature by settings the `aggregation` of your view to `QUANTILES`.

The quantiles to export can be defined via the `quantiles` option of the view, where values from `0` to `1` can be specified.

Whenever the recorded metrics are exported, inspectIT Ocelot computes the requested quantiles adhoc based on the buffered values of the given metric.
For example, when using the default time window of 15 seconds, the inspectIT Ocelot agent will expose the quantiles of the metric values observed within the last 15 seconds at the point of time when doing the export.
For this reason, `time-window` property should always be **equal or greater** than your metrics scrape or export interval.

:::important
It is important to note that depending on the amount of gathered data the computations of percentiles can be a lot more expensive than just sums or histograms!
To avoid that this feature causes a too high memory footprint, the `max-buffered-points` property exists, limiting the amount of data buffered for a view.
The default value of `16384` was chosen so that the view can handle roughly 1000 data points per second with the default time window of `15s`.
If this limit is exceeded, the quantiles will become meaningless due to data dropping and a warning will be printed in the logs.
:::

### Collecting Min and Max Values

The quantiles aggregation of a view also allows the capturing of minimum and maximum values of metrics.
This can be done by using the special quantiles `0` and `1`, which enables the export of the minimum and maximum observed value respectively.