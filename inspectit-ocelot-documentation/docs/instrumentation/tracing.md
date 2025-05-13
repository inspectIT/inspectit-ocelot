---
id: tracing
title: Collecting Tracing
---

The inspectIT Ocelot agent allows you to record method invocations as [OpenTelemetry spans](https://opentelemetry.io/docs/concepts/signals/traces/#spans) with the help
of [rules](instrumentation/rules.md).


In order to make your collected spans visible, you must first set up a [trace exporter](tracing/trace-exporters.md).
Afterward you can define that all methods matching a certain rule will be traced:

```yaml
inspectit:
  instrumentation:
    rules:
      'r_example_rule':
        tracing:
          start-span: true
```

For example, using the previous configuration snippet, each method that matches the scope definition of the `example_rule` 
rule will appear within a trace.
Its appearance can be customized using the following properties which can be set in the rule's `tracing` section.

| Property     | Default                 | Description                                                                                                                                                                                                                                                                      |
|--------------|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `start-span` | `false`                 | If true, all method invocations of methods matching any scope of this rule will be collected as spans.                                                                                                                                                                           |
| `name`       | `null`                  | Defines a data key whose value will be used as name for the span. If it is `null` or the value for the data key is `null`, the fully qualified name of the method will be used. Note that the value for the data key must be written in the entry section of the rule at latest! |
| `kind`       | `null`                  | Can be `null`, `CLIENT` or `SERVER` corresponding to the [OpenTelemetry values](https://opentelemetry.io/docs/concepts/signals/traces/#span-kind).                                                                                                                               |
| `attributes` | `{}` (empty dictionary) | Maps names of attributes to data keys whose values will be used on exit to populate the given attributes.                                                                                                                                                                        |

Commonly, you do not want to have the fully qualified name of the instrumented method as span name. 
For example, for HTTP requests you typically want the HTTP path as span name. 
This behaviour can be customized using the `name` property:

```yaml
inspectit:
  instrumentation:
    rules:
      'r_servlet_api_service':
        tracing:
          start-span: true
          name: 'http_path'
        entry:
          'http_path':
           #... action call to fetch the http path here
```

:::note
The name must exist at the end of the entry section and cannot be set in the exit section.
:::

## Trace Sampling

It is often desirable to not capture every trace, but instead [sample](https://opentelemetry.io/docs/concepts/sampling/) 
only a subset.
This can be configured using the `sample-probability` and `sample-mode` setting under the `tracing` section:

```yaml
inspectit:
  instrumentation:
    rules:
      'r_servlet_api_service':
        tracing:
          start-span: true
          sample-probability: '0.2'
          sample-mode: PARENT_BASED
```

The example shown above will ensure that only 20% of all traces starting at the given rule will actually be exported.
Instead of specifying a fixed value, you can also specify a data key here, just like for `name`.
In this case, the value from the given data key is read and used as sampling probability.
This allows you for example to vary the sample probability based on the HTTP url.

The `sample-mode` is explained at [tracing](tracing/tracing.md) and can either be `PARENT_BASED`, `TRACE_ID_RATIO_BASED`, 
or `HYBRID_PARENT_TRACE_ID_RATIO_BASED`.

By default, `HYBRID_PARENT_TRACE_ID_RATIO_BASED` sampler is used if `sample-mode` is unset and the `sample-probability` 
has been specified.

If no sample probability is defined for a rule, the [default probability](tracing/tracing.md) is used.

## Adding Attributes

Another useful property of spans is that you can attach any additional information in form of attributes.
In most tracing backends such as Zipkin and Jaeger, you can search your traces based on attributes.
The example below shows how you can define attributes:

```yaml
inspectit:
  instrumentation:
    rules:
      'r_servlet_api_service':
        tracing:
          start-span: 'true'
          attributes:
            'http_host': 'host_name'
        entry:
          'host_name':
           #... action call to fetch the http host here
```

The attributes property maps the names of attributes to data keys.
After the rule's exit phase, the corresponding data keys are read and attached as attributes to the span started or continued by the method.

Note that if a rule does not start or continue a span, no attributes will be written.

The [common tags](metrics/common-tags.md) are added as attributes in all local span roots by default.
This behavior can be configured in the global [tracing settings](tracing/tracing.md#common-tags-as-attributes).

## Visualizing Span Errors

Most tracing backends support highlighting of spans which are marked as errors.
InspectIT Ocelot allows you to configure exactly under which circumstances your spans are interpreted
as errors or successes.
This is done via the `error-status` configuration property of a rule's tracing section:

```yaml
inspectit:
  instrumentation:
    rules:
      'r_example_rule':
        tracing:
          start-span: true
          error-status: _thrown
```

The value of the `error-status` property can be any value from the context or any [special variable](instrumentation/actions.md#input-parameter).

When the instrumented method finishes, inspectIT Ocelot will read the value of the given variable.
If the value is neither `null` nor `false`, the span will be marked as an error.

In the example above, the special variable `_thrown` is used to define the error status.
This means if `_thrown` is not null (which means the method threw an exception), the span will be marked as error.

## Adding Span Conditions

It is possible to conditionalize the span starting as well as the attribute writing:

```yaml
inspectit:
  instrumentation:
    rules:
      'r_span_starting_rule':
        tracing:
          start-span: true
          start-span-conditions:
            only-if-true: 'my_condition_data'
#....
      'r_attribute_writing_rule':
        tracing:
          attributes:
            'attrA': 'data_a'
            'attrB': 'data_b'
          attribute-conditions:
            only-if-true: 'my_condition_data'
```

If any `start-span-conditions` are defined, a span will only be created when all conditions are met.
Analogous to this, attributes will only be written if each condition defined in `attribute-conditions` is fulfilled.
The conditions that can be defined are equal to the ones of actions, thus, please see the [action conditions description](instrumentation/rules.md#adding-conditions) for detailed information.

## Auto-Tracing

:::warning Experimental Feature
Please note that this is an experimental feature.
This feature is currently experimental and can potentially have a high performance impact. We do **not** recommend using it for production environments yet!
:::

With the shown approach traces will only contain exactly the methods you instrumented.
Often however, one observes certain methods taking a long time without knowing where exactly the time is spent.
The "auto-tracing" feature can be used to solve this problem.

When auto-tracing is enabled, inspectIT Ocelot uses a profiler-like approach for recording traces.
With auto-tracing, stack traces of threads are collected periodically. 
Based on these samples, inspectIT Ocelot will reconstruct an approximate trace showing where the time was spent.

Auto-tracing can be enabled on methods which are traced using either the `start-span` or `continue-span` options.
To enable it you can simply add the `auto-tracing` setting:

```yaml
inspectit:
  instrumentation:
    rules:
      'r_example_rule':
        tracing:
          start-span: true
          auto-tracing: true
```

When auto-tracing is enabled, callees of the method will be traced by periodically capturing stack traces.
Methods recorded via auto-tracing will be marked in the traces by a leading asterisk.

You can also set the `auto-tracing` option to `false`, which disables auto-tracing for the methods affected by this rule.
In this case any active `auto-traces` will be paused for the duration of the method, no samples will be recorded.

The frequency at which stack trace samples are captured is defined globally:
```yaml
inspectit:
  tracing:
    auto-tracing:
      frequency: 50ms
```

This setting specifies that each thread for which auto-tracing is enabled will be stopped every 50ms in order to capture a stack trace.
It also implicitly defines the granularity of your traces: Only methods with at least this duration will appear in your traces.


## Tracing Asynchronous Invocations

With the previous shown settings, it is possible to add an instrumentation which creates exactly one span per invocation of an instrumented method.
Especially in asynchronous scenarios, this might not be the desired behaviour:
For these cases inspectIT Ocelot offers the possibility to record multiple method invocations into a single span.
The resulting span then has the following properties:

* the span starts with the first invoked method and ends as soon as the last one returns
* all attributes written by each method are combined into the single span
* all invocations made from the methods which compose the single span will appear as children of this span

This can be configured by defining for rules that they (A) can continue existing spans and (B)
can optionally end the span they started or continued.

Firstly, it is possible to "remember" the span created or continued using the `store-span` option:

```yaml
    rules:
      'r_span_starting_rule':
        tracing:
          start-span: true
          store-span: 'my_span_data'
          end-span: false
```

With this option, the span created at the end of the entry phase will be stored in the context with the data key `my_span_data`. 
Usually this span reference is then extracted from the context and attached to an object via the [Object Attachments](instrumentation/actions.md#attaching-values).

Without the `end-span: false` definition above, the span would be ended as soon as the instrumented method returns.
By setting `end-span` to false, the span is kept open instead. It can then be continued when another method is executed as follows:

```yaml
    rules:
      'r_span_finishing_rule':
        tracing:
          continue-span: 'my_span_data'
          end-span: true # actually not necessary as it is the default value
```

Methods instrumented with this rule will not create a new span. 
Instead, at the end of the entry phase the data for the key `my_span_data` is read from the context. 
If it contains a valid span written via `store-span`, this span is continued in this method. 
This implies that all spans started by callees of this method will appear as children of the span stored in `my_span_data`. 
+In addition, this rule also then causes the continued span to end with the execution of the method due to the `end-span` option. 
This is not required to happen: a span can be continued by any number of rules before it is finally ended.

It also is possible to define rules for which both `start-span` and `continue-span` is configured.
In this case, the rule will first attempt to continue the existing span. Only if this fails 
(e.g. because the specified span does not exist yet or the conditions are not met) a new span is started.

Again, conditions for the span continuing and span ending can be specified just like for the span starting.
The properties `continue-span-conditions` and `end-span-conditions` work just like `start-span-conditions`.

## Distributed Tracing with Remote Parent Context

The inspectIT default configuration provides two options to use a remote parent context for distributed tracing. 
Of course, you may write another action by yourself.
If the remote parent context exists before your service gets called, you can use _readDownPropagationHeaders()_ 
inside your propagation action. If the remote parent context will be created after calling your service,
you can use _createRemoteParentContext()_ inside your action and pass the context information to the client.

#### readDownPropagationHeaders()

The method takes a _Map<String,String>_ as a parameter. This map should contain at least the trace context, which was sent by a Http request header.
The remote parent trace context should be in either B3-format, W3C-format or datadog-format.
The action, which uses this function, should be called in the `pre-entry` or `entry` phase.

InspectIT Ocelot offers a default action to read down propagated headers in javax HTTP Servlets: `a_servletapi_downPropagation`

#### createRemoteParentContext()

The method takes no parameter. It can be used, if the remote context will be created after calling your service, 
but still should be used as a parent.
The function creates a span context, which will be used as a parent for the current context. It returns a string, which contains
the trace context of the remote parent in W3C-format. You can pass the returned trace context to your remote client 
via HTTP response header and use the information to create the remote parent context there.

Note that, if a remote parent context was down propagated via _readDownPropagationHeaders()_, inspectIT Ocelot will 
use the down propagated context as a parent and ignore the context created with _createRemoteParentContext()_.

The action, which uses this function, should be called in the `pre-entry` or `entry` phase.

InspectIT Ocelot offers a default action to create a remote parent context when using javax HTTP Servlets: `a_servletapi_remoteParentContext`
