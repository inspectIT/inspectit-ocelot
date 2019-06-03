---
id: version-0.2-custom-metrics
title: Custom Metrics
original_id: custom-metrics
---

One of the main benefits of inspectIT Ocelot is that it givers you complete
freedom in what data you want to capture and how you want to capture it.
You can easily define custom performance and business metrics depending on your needs.

In this section we explain how you can define custom OpenCensus [metrics](https://opencensus.io/stats/)
and [views](https://opencensus.io/stats/view/). We only show how to define the structure of both,
not how the data is collected. For details on the data collection please see the [instrumentation section](instrumentation/rules.md#collecting-metrics).

All metrics are defined under the `inspectit.metrics.definitions` property.
This is best explained by giving a simple example which comes from the default
configuration of inspectIT Ocelot:

```YAML
inspectit:
  metrics:
    definitions:
      '[method/duration]':
        unit: ms
        description: "the duration from method entry to method exit"
        views:
          '[method/duration/sum]':
            aggregation: SUM
            tags:
              method_name : true
          '[method/duration/count]':
            aggregation: COUNT
            tags:
              method_name: true
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
|`aggregation`|`LAST_VALUE`|Specifies how the measurement data is aggregated in this view. Possible values are `LAST_VALUE`, `COUNT`, `SUM` and `HISTOGRAM`. These correspond to the [OpenCensus Aggregations](https://opencensus.io/stats/view/#aggregations).
|`bucket-boundaries`|-| *Required if aggregation is `HISTOGRAM`.* A list of the boundaries of the histogram buckets. E.g. `[7.5,42]` defines three histogram buckets split at `7.5` and `42`.
|`with-common-tags`| `true` | If true, all [common tags](metrics/common-tags.md) will be used for this view. Individual tags can still be disabled via the `tags` option.
|`tags`| `{}` | Specifies which tags should be used for this view. `tags` is a map containing tag names as key and either `true` or false as value. For example the value `{service: false, my_tag: true}` would remove the common tag `service` from the view and add the user tag `my_tag` to it.

> Due to a limitation of the current OpenCensus library, it is *not* possible to remove or alter views and metrics once they have been registered. However you can still add new views and metrics through dynamic configuration updates after the agent has already started.