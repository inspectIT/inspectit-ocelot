---
id: metrics
title: Collecting Metrics
---

Metrics collection is done in the metrics phase of a [rule](instrumentation/rules.md), 
which can be configured using the `metrics` option:

```yaml
#inspectit.instrumentation.rules is omitted here
'r_example_rule':
  #...
  exit:
    'method_duration':
      #action invocation here....
    'method_name':
      #action invocation here....

  metrics:
    '[method/duration]':
      value: 'method_duration'
      constant-tags:
        action: 'checkout'
      data-tags:
        method_name: 'method_name'
    'write_my_other_metric':
      metric: 'some/other/metric'
      value: '42'
```

The metrics phase is executed after the exit phase of the rule.
As shown above, you can assign values to metrics based on their name or explicitly define the metric name in `metric` property.
This allows to write multiple values for the same metric from within the same rule.
You must however have [defined the metric](metrics/custom-metrics.md) to use them.

The measurement value written to the metric can be specified by giving a data key in the `value` property.
This was done in the example above for `method/duration`:
Here, the `value` for the data key `method_duration` is taken, which we previously wrote in the exit phase.
Alternatively you can just specify a constant which will be used, like shown for `some/other/metric`.

In addition, you should define tags that are be recorded alongside the metric value.
The prerequisite for this is that tags have been declared in the [metric definition](metrics/custom-metrics.md) 
and [configured to be used as tags](instrumentation/data-propagation.md#defining-the-behaviour).
Constant tags always have same values as defined in the configuration.
The data tags try to resolve value from the data key, which is previously wrote in the exit phase.
If data key for the data tag can not be found, then corresponding tag is omitted.
Note that `data-tags` have higher priority than the `constant-tags`, thus if both section define a tag with same key, the data tag will overwrite the constant one if it can be resolved.

:::note
All [common tags](metrics/common-tags.md) are always included in the metric recording and do not need explicit specification.
:::
