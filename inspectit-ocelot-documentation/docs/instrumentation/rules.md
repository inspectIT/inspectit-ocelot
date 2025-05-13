---
id: rules
title: Rules
---

Rules define (A) how data should be extracted when the instrumented method is executed and 
(B) which metrics and traces shall be recorded.
The selection on which methods these [actions](instrumentation/actions.md) are applied is done through [scopes](instrumentation/scopes.md).

A highlight of inspectIT is the fact that you are completely free in defining how the data is extracted. 
In addition, the extracted data can be made visible outside the instrumented method
in which it was collected: Data can be configured to be propagated up or down with the call stack,
which is explained in the section [Data Propagation](instrumentation/data-propagation.md).

The overall concept of rules is best explained with a simple example which is part of the inspectIT Ocelot default configuration:

```yaml
inspectit:
  instrumentation:
    rules:

      'r_record_method_duration':

        entry:
          'method_entry_time':
            action: 'a_timing_nanos'
          'class_name':
            action: 'a_method_getClassFQN'
          'method_name_with_params':
            action: 'a_method_getNameWithParameters'

        exit:
          'method_duration':
            action: 'a_timing_elapsedMillis'
            data-input:
              'since_nanos': 'method_entry_time'

        metrics:
          '[method/duration]' : 
            value: 'method_duration'
            data-tags:
              'class': 'class_name'
              'method': 'method_name_with_params'
             
```

This example rule named `r_record_method_duration` measures the duration of the instrumented method and outputs the value using
the `method/duration` metric. The actions `a_timing_nanos` and `a_timing_elapsedMillis` should be familiar 
for you from the previous [actions](instrumentation/actions.md) section.

As the name states, we define under the `entry` property of the rule which actions are performed on method entry.
Similarly, the `exit` property defines what is done when the instrumented method returns. In both sections we collect data.

On entry, we collect the current timestamp in a variable named `method_entry_time` and the name and class of the currently executed
method in `method_name_with_params` and `class_name`.
These variables are _data_, their names are referred to as _data keys_.
Note that we also define how the data is collected: For `method_entry_time` we invoke the [action](instrumentation/actions.md) 
named `a_timing_nanos` and for `class_name` the one named `a_method_getClassFQN`.

This data is then used on method exit: using the action `a_timing_elapsedMillis` we compute the time which has passed 
since `method_entry_time`. Finally, the duration computed this way is used as a value for the `method/duration` metric. 
As shown in the [definition](metrics/custom-metrics.md) of this metric, the collected class and name of the method is used as 
a tag for all of its views.


## Defining Rules

Rules glue together [scopes](instrumentation/scopes.md) and [actions](instrumentation/actions.md) to define which actions 
you want to perform on which application method.

As you might have noticed, the initial example rule shown in the [rules section](instrumentation/rules.md) did not define 
any reference to a scope. This is because this rule originates from the default configuration of inspectIT Ocelot, 
where we don't know yet of which methods you want to collect the response time. 
Therefore, this rule is defined without scopes, but you can easily add some in your own configuration files:

```yaml
inspectit:
  instrumentation:
    rules:

      'r_record_method_duration':
        scopes:
          's_my_first_scope': true
          's_my_second_scope': true
```

With this snippet we defined that the existing rule `record_method_duration` gets applied on the two scopes named 
`my_first_scope` and `my_second_scope`. The `scopes` configuration option maps scope names to `true` or `false`. 
The rule will be applied on all methods matching any scope where the value is `true`.

Rules define their action within multiple _phases_:

* **Entry Phase:** The actions defined in this phase get invoked directly before the body of the instrumented method. 
  You can imagine that these actions are "inlined" at the very top of every method instrumented by the given rule.

* **Exit Phase:** The actions defined in this phase get invoked after the body of the instrumented method has terminated. 
  This can be the method either returning normally or throwing an exception. You can imagine that these actions are 
  placed in a `finally` block of a `try` block surrounding the body of the instrumented method.

* **Tracing Phase:** To start and end tracing, two actions are executed.
  The starting action gets invoked after the _entry phase_.
  The ending action gets invoked after the _exit phase_.

* **Metrics Phase:** These actions are executed directly after the _exit phase_ (and after the ending _tracing phase_).
  Here, only values for metrics are recorded. No actions will be executed here.

The actions performed in these phases are defined in rules under the `entry`, `exit`, `tracing` and `metrics` configuration options. 
In the entry and in the exit phase the actions you perform are invocations of [actions](instrumentation/actions.md). 
Please see the [Invoking Actions](#invoking-actions) section for information on how this is done.

In the _metrics phase_ you only can collect metrics, this is explained in the [Collecting Metrics](instrumentation/metrics.md) section.

## Invoking Actions

In this section you will find out how to collect data in the entry and exit phase of rules by invoking [actions](instrumentation/actions.md) 
and storing the results in the [inspectIT context](instrumentation/data-propagation.md).

Let's take a look again at the entry phase definitions of the ``record_method_duration`` rule:

```yaml
#inspectit.instrumentation.rules is omitted here
'r_record_method_duration':
  entry:
    'method_entry_time':
      action: 'a_timing_nanos'
    'class_name':
      action: 'a_method_getClassFQN'
    'method_name_with_params':
      action: 'a_method_getNameWithParameters'
```

The `entry` and `exit` configuration options are YAML dictionaries mapping data keys to _action invocations_.
This means the keys used in the dictionaries define the data key for which a value is being defined. 
Correspondingly, the assigned value defines which action is invoked to define the value of the data key.

In the example above `method_entry_time` is a data key. The action which is invoked is defined through the `action` 
configuration option. In this case, it is the action named `timestamp_nanos`.

### Assigning Input Parameter Values

Actions [can require input parameters](#input-parameters) which need to be assigned when invoking them.
There are currently two possible ways of doing this:

* **Assigning Data Values:** In this case, the value for a given data key is extracted from the [inspectIT context](#data-propagation) and passed to the action
* **Assigning Constant Values:** In this case a literal specified in the configuration will directly be passed to the action.

We have already seen how the assignment of data values to parameters is done in the exit phase of the `r_record_method_duration` rule:

```yaml
#inspectit.instrumentation.rules is omitted here
'r_record_method_duration':
  exit:
    'method_duration':
      action: 'a_timing_elapsedMillis'
      data-input:
        'since_nanos': 'method_entry_time'
```

The `a_timing_elapsedMillis` action requires a value for the input parameter `since_nanos`.
In this example we defined that the value for the data key `method_entry_time` is used for `since_nanos`.

The assignment of constant values works very similar:

```yaml
#inspectit.instrumentation.rules is omitted here
'r_example_rule':
  entry:
    'hello_world_text':
      action: 'a_assign_value'
      constant-input:
        'value': 'Hello World!'
```

Note that when assigning a constant value, inspectIT Ocelot automatically converts the given value to the type expected 
by the action. This is done using the [Spring Conversion Service](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/convert/ConversionService.html). For example, if your action expects a parameter of 
type `java.time.Duration`, you can simply pass in `"42s"` as constant.

As you might have noticed, `data-input` and `constant-input` are again YAML dictionaries.
This allows you to assign values for actions which expect multiple input parameters.
You can also mix which parameters you assign from data and which from constants:


```yaml
#inspectit.instrumentation.rules is omitted here
'r_example_rule':
  entry:
    'bye_world_text':
      action: 'a_string_replace_all'
      data-input:
        'string': 'hello_world_text'
      constant-input:
        'regex': 'Hello'
        'replacement': 'Bye'
```

As expected given the [definition](/instrumentation/actions.md) of the `string_replace_all` action, 
the value of `bye_world_text` will be `"Bye World!"`

### Adding Conditions

It is possible to add conditions to action invocations. The invocation will only occur if the specified condition is met. 
Currently, the following configuration options can be used:

| Config Option      | Description                                                                                              |
|--------------------|----------------------------------------------------------------------------------------------------------|
| `only-if-null`     | Only executes the invocation if the value assigned with the given data key is null.                      |
| `only-if-not-null` | Only executes the invocation if the value assigned with the given data key is not null.                  |
| `only-if-true`     | Only executes the invocation if the value assigned with the given data key is the boolean value `true`.  |
| `only-if-false`    | Only executes the invocation if the value assigned with the given data key is the boolean value `false`. |

An example for the usage of a condition is given below:

```yaml
#inspectit.instrumentation.rules is omitted here
'r_example_rule':
  entry:
    'application_name':
      action: 'a_assign_value'
      constant-input:
        'value': 'My-Application'
      only-if-null: 'application_name'
```

In this example we define an invocation to set the value of the data key `application_name`
to `"My-Application"`. However, this assignment is only performed if `application_name` previously was null, 
meaning that no value has been assigned yet. This mechanism is in particular useful when `application_name` 
is [down propagated](instrumentation/data-propagation.md).

If multiple conditions are given for the same action invocation, the invocation is only executed if *all* conditions are met.

### Execution Order

As we can use data values for input parameters and for conditions, action invocations can depend on another. 
This means that a defined order on action executions within each phase is required for rules to work as expected.

In addition to the previously mentioned `entry` and `exit` config options, there are also
`pre-entry`, `post-entry`, `pre-exit` and `post-exit` config options which are YAML dictionaries as well. 
The order they are given in the config file does not matter. YAML dictionaries do not maintain or define an order 
of their entries.

However, inspectIT Ocelot _automatically_ orders the invocations for you correctly.
For each instrumented method the agent first finds all rules which have scopes matching the given method. 
Afterward, these rules get combined into one "super"-rule by simply merging the 
`entry`, `exit`, `pre-entry`, `post-entry`, `pre-exit` and `post-exit` and `metrics` phases.

Within the `entry` and the `exit` phase, actions are now automatically ordered based on their dependencies.
`pre-entry` and `pre-exit` actions will be executed before their particular phase.
`post-entry` and `post-exit` actions will be executed after their particular phase.

For example, the invocation writing `data_b` uses `data_a` as input, the invocation writing `data_a` is guaranteed to 
be executed first! Whenever you use a data value as value for a parameter or in a condition, this will be counted 
as a dependency.

In some rare cases you might want to change this behaviour. E.g. in tracing context you want to store 
the [down propagated](instrumentation/data-propagation.md) `span_id` in `parent_span`, before the current method assigns 
a new `span_id`. This can easily be realized using the `pre-entry` phase for action invocations:

```yaml
#inspectit.instrumentation.rules is omitted here
'r_example_rule':
  pre-entry:
    'parent_span':
      action: 'a_assign_value'
      data-input:
        'value': 'span_id'
```

## Modularizing Rules

When writing complex instrumentation, it can happen that you want to reuse parts of your instrumentation across different rules.
For example when instrumenting a HTTP library, you typically extract the HTTP path using custom actions. This HTTP path is meant
to be used for multiple concerns, e.g. for tracing and for metrics which are specified in separate rules.

A simple solution would be to copy and paste the action invocation for extracting the path into both the metrics and the tracing rule.
This has two main downsides:
* The work is done twice: Your action for extracting the HTTP path is invoked twice, leading to unnecessary overhead
* When altering how the HTTP path is extracted, you need to remember every rule where you copy-pasted your instrumentation

To overcome these issues, Ocelot allows you to include rules from within other rules:

```yaml
    rules:
      'r_myhttp_extract_path':
        entry:
          'my_http_path':
            #logic to extract the http path and save it in the context here...
          
      'r_myhttp_tracing':
        include:
          'myhttp_extract_path': true
        scopes:
          's_myhttp_scope': true
        tracing:
          start-span: true
          attributes:
            'path': 'my_http_path'
            
      'r_myhttp_record_metric':
        include:
          'myhttp_extract_path': true
        scopes:
          's_myhttp_scope': true
        metrics:
          #record http metric here...
```

In the above example we defined a rule `myhttp_extract_path`, which contains the logic for extracting the HTTP path.
Note that this rule does not have any scope attached and therefore does not result in any instrumentation by default.

However, the example also contains the two rules `myhttp_tracing` and `myhttp_record_metric`.
They both reference the `myhttp_extract_path` rule via their `include` property.
While in the example exactly one rule is included, it is possible to include any amount of rules.
Includes also work transitively.

If a rule is included, it has the same effect as adding all the scopes of this rule to the included one.
This means that all actions, tracing settings and metrics recordings of the included rule are also applied.

In this example this means that if either `myhttp_tracing` or `myhttp_record_metric` are enabled,
`myhttp_extract_path` will also be applied to all methods matching the scope `myhttp_scope`.
As a result, the `my_http_path` data variable will be populated.

The key point is now that even if the rule is included multiple times for a given method, it will only be applied exactly once.
This means that if both `myhttp_tracing` and `myhttp_record_metric` are enabled, the `myhttp_extract_path` will still only be applied once.
This therefore solves the problem of accidentally doing the same work twice.

## Default Rules

InspectIT Ocelot provides some default rules, which can be used included into other rules, to reduce the effort of 
collecting simple data.

### Collecting Method Metrics

Include the rule `r_method_metric` to record the duration of the method call as metric.
In addition, some tags are added to the metric, like method name or error status.
In the upcoming section you will learn more about [Collecting Metrics](instrumentation/metrics.md).

```yaml
rules:
  r_rule:
    include:
      r_method_metric: true
```

### Collecting Traces

Include the rule `r_method_metric` to record the current method as part of a trace.
In addition, some attributes are added to the trace, like method name or error status.
In the upcoming section you will learn more about [Collecting Traces](instrumentation/tracing.md).

```yaml
rules:
  r_rule:
    include:
      r_trace_method: true
```
