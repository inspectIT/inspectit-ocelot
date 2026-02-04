---
id: metrics
title: Collecting Metrics
---

## Custom Metrics

Custom metrics collection is done in the metrics phase of a [rule](instrumentation/rules.md), 
which can be configured using the `metrics` option:

```yaml
#inspectit.instrumentation.rules is omitted here
r_example_rule:
  #...
  exit:
    method_duration_value:
      #action invocation here....
    method_name:
      #action invocation here....

  metrics:
    method_duration:
      value: method_duration_value
      constant-tags:
        action: checkout
      data-tags:
        method_name: method_name
    write_my_other_metric:
      metric: some_other_metric
      value: 42
```

The metrics phase is executed after the exit phase of the rule.
As shown above, you can assign values to metrics based on their name or explicitly define the metric name in `metric` property.
This allows to write multiple values for the same metric from within the same rule.
You must however have [defined the metric](metrics/custom-metrics.md) to use them.

The measurement value written to the metric can be specified by giving a data key in the `value` property.
This was done in the example above for `method_duration`:
Here, the `value` for the data key `method_duration_value` is taken, which we previously wrote in the exit phase.
Alternatively you can just specify a constant which will be used, like shown for `some_other_metric`.

In addition, you should define attributes that are be recorded alongside the metric value.
The prerequisite for this is that attributes have been declared in the [metric definition](metrics/custom-metrics.md).
Constant tags always have same values as defined in the configuration.
The data tags try to resolve value from the data key, which is previously wrote in the exit phase.
If data key for the data tag can not be found, then corresponding attribute is omitted.
Note that `data-tags` have higher priority than the `constant-tags`, thus if both section define a attribute with same key, 
the data tag will overwrite the constant one if it can be resolved.

:::note
All [common attributes](metrics/common-attributes.md) are always included in the metric recording and do not need explicit specification.
:::

## Concurrent Invocations

The concurrent invocations of all methods within the current rule can be collected using the `concurrent-invocations` option:

```yaml
#inspectit.instrumentation.rules is omitted here
'r_example_rule':
  # ...
  concurrent-invocations:
    enabled: true
    operation: 'example-operation'
```

There is an internal storage, which holds every active invocation for each operation.
An operation is just the name used for identifying invocations. If a [rule](instrumentation/rules.md) contains multiple methods within 
it's [scopes](instrumentation/scopes.md), then all of these methods will use the same operation name.
Calling any of these methods counts as one new invocation.
Currently, the operation name has to be defined via a constant string. The operation name will be used for the 
`operation` attribute of the recorded metric. If no operation name has been specified within the rule, the default operation
name (_default_) will be used.

When `concurrent-invocations` is enabled for the current rule, every invocation will be added to the 
internal storage during the entry phase. In the exit phase one invocation will be removed again.
Since invocations are started and ended within one method call, asynchronous operations are not fully supported yet.

:::note
Metrics for concurrent invocations can only be recorded, if the proper [metrics recorder](metrics/metric-recorders.md#concurrent-invocations) is enabled. Otherwise, the data will only be collected internally but never properly recorded and exported.
:::
