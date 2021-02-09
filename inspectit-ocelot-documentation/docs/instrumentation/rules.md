---
id: rules
title: Rules
---

Rules define (a) how data should be extracted when the instrumented
method is executed and (b) which metrics shall be recorded.
The selection on which methods these actions are applied is done through [scopes](instrumentation/scopes.md).

A highlight of inspectIT is the fact that you are completely free in defining how the data is
extracted. In addition, the extracted data can be made visible outside of the instrumented method
in which it was collected: Data can be configured to be propagated up or down with the call stack,
which is explained in the section [Data Propagation](#data-propagation).

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
the `method/duration` metric.

As the name states, we define under the `entry` property of the rule which actions are performed on method entry.
Similarly, the `exit` property defines what is done when the instrumented method returns. In both sections we collect data.

On entry, we collect the current timestamp in a variable named `method_entry_time` and the name and class of the currently executed
method in `method_name_with_params` and `class_name`.
These variables are _data_, their names are referred to as _data keys_.
Note that we also define how the data is collected: For `method_entry_time` we invoke the [action](#actions) named `a_timing_nanos` and for `class_name` the one named `a_method_getClassFQN`.

This data is then used on method exit: using the action `a_timing_elapsedMillis` we compute the time which has passed since `method_entry_time`. Finally, the duration computed this way is used as a value for the `method/duration` metric. As shown in the [definition](metrics/custom-metrics.md) of this metric, the collected class and name of the method is used as a tag for all of its views.

## Data Propagation

As illustrated by the previous example, we can collect any amount of data in both the entry and the exit section of an instrumented method. Each data is hereby identified by its name, the _data key_.
Internally, inspectIT creates a dictionary like Object each time an instrumented method is executed. This object is basically a local variable for the method. Whenever data is written, inspectIT stores the value under the given data key in this dictionary. Similarly, whenever data is read, inspectIT looks it up based on the data key in the dictionary. This dictionary is called _inspectIT context_.

If the inspectIT context was truly implemented as explained above, all data would be only visible in the method where it was collected. This however often is not the desired behaviour.
Consider the following example: you instrument the entry method of your HTTP server and collect the request URL as data there. You now of course want this data to be visible as tag for metrics collected in methods called by your entry point. With the implementation above, the request URL would only be visible within the HTTP entry method.

For this reason the inspectIT context implements _data propagation_. The propagation can happen in two directions:

* **Down Propagation:** Data collected in your instrumented method will also be visible to all methods directly or indirectly called by your method. This behaviour already comes [with the OpenCensus Library for tags](https://opencensus.io/tag/#propagation).
* **Up Propagation:** Data collected in your instrumented method will be visible to the methods which caused the invocation of your method. This means that all methods which lie on the call stack will have access to the data written by your method

Up- and down propagation can also be combined: in this case then the data is attached to the control flow, meaning that it will appear as if its value will be passed around with every method call and return.

The second aspect of propagation to consider is the _level_. Does the propagation happen within each Thread separately or is it propagated across threads? Also, what about propagation across JVM borders, e.g. one micro service calling another one via HTTP? In inspectIT Ocelot we provide the following two settings for the propagation level.

* **JVM local:** The data is propagated within the JVM, even across thread borders. The behaviour when data moves from one thread to another is defined through [Special Sensors](instrumentation/special-sensors.md).
* **Global:** Data is propagated within the JVM and even across JVM borders. For example, when an application issues an HTTP request, the globally down propagated data is added to the headers of the request. When the response arrives, up propagated data is collected from the response headers. This protocol specific behaviour is realized through default instrumentation rules provided with the agent, but can be extended as needed.

### Defining the Behaviour

The propagation behaviour is not defined on rule level but instead globally based on the data keys under the configuration
property `inspectit.instrumentation.data`. Here are some examples extracted from the default configurations of inspectIT:

```yaml
inspectit:
  instrumentation:
    data:
      # for correlating calls across JVM borders
      'prop_origin_service': {down-propagation: "GLOBAL", is-tag: "false"}
      'prop_target_service': {up-propagation: "GLOBAL", down-propagation: "JVM_LOCAL", is-tag: "false"}

      #we allow the application to be defined at the beginning and to be down propagated from there
      'application': {down-propagation: "GLOBAL", is-tag: "true"}

      #this data will only be visible locally in the method where it is collected
      'http_method': {down-propagation: "NONE"}
      'http_status': {down-propagation: "NONE"}
```

Under `inspectit.instrumentation.data`, the data keys are mapped to their desired behaviour.
The configuration options are the following:

|Config Property|Default| Description
|---|---|---|
| `down-propagation` | `JVM_LOCAL` if the data key is also a [common tag](metrics/common-tags.md), `NONE` otherwise | Configures if values for this data key propagate down and the level of propagation. Possible values are `NONE`, `JVM_LOCAL` and `GLOBAL`. If `NONE` is configured, no down propagation will take place. | 
| `up-propagation` |  `NONE` |  Configures if values for this data key propagate up and the level of propagation. Possible values are `NONE`, `JVM_LOCAL` and `GLOBAL`. If `NONE` is configured, no up propagation will take place. | 
| `is-tag` | `true` if the data key is also a [common tag](metrics/common-tags.md) or is used as tag in any [metric definition](metrics/custom-metrics.md), `false` otherwise | If true, this data will act as a tag when metrics are recorded. This does not influence propagation. | 

Note that you are free to use data keys without explicitly defining them in the `inspectit.instrumentation.data` section. In this case simply all settings will have their default value.

### Interaction with OpenCensus Tags

As explained previously, our inspectIT context can be seen as a more flexible variation of OpenCensus tags. In fact, we designed the inspectIT context so that it acts as a superset of the OpenCensus TagContext.

Firstly, when an instrumented method is entered, a new inspectIT context is created. At this point, it imports any tag values published by OpenCensus directly as data. This also includes the [common tags](metrics/common-tags.md) created by inspectIT. This means, that you can simply read (and override) values for common tags such as `service` or `host_address` at any rule.

The integration is even deeper if you [configured the agent to also extract the metrics from manual instrumentation in your application](configuration/open-census-configuration.md).
Firstly, if a method instrumented by inspectIT Ocelot is executed within a TagContext opened by your application,
these application tags will also be visible in the inspectIT context. Secondly, after the execution of the entry phase of each rule, a new TagContext is opened making the tags written there accessible to metrics collected by your application. Hereby, only data for which down propagation was configured to be `JVM_LOCAL` or greater and for which `is-tag` is true will be visible as tags.

## Actions

Actions are the tool for extracting arbitrary data from your application or the context.
They are effectively Lambda-like functions you can invoke from the entry and the exit phase of rules. They are defined by (a) specifying their input parameters and (b) giving a Java code snippet which defines how the result value is computed from these.

Again, this is best explained by giving some simple examples extracted from inspectIT Ocelot default configuration:

```yaml
inspectit:
  instrumentation:
    actions:

      #computes a nanosecond-timestamp as a long for the current point in time
      'a_timing_nanos':
        value: 'new Long(System.nanoTime())'

      #computes the elapsed milliseconds as double since a given nanosecond-timestamp
      'a_timing_elapsedMillis':
        input:
          #the timestamp captured via System.nanoTime() to compare against
          'since_nanos': 'long'
        value: 'new Double( (System.nanoTime() - sinceNanos) * 1E-6)'

      'a_string_replace_all':
        input:
          'regex': 'String'
          'replacement': 'String'
          'string': 'String'
        value: 'string.replaceAll(regex,replacement)'

      'a_method_getClassFQN':
        input:
          _class: Class
        value: '_class.getName()'
```

The names of the first two actions, `a_timing_nanos` and `a_timing_elapsedMillis` should be familiar for you from the initial example in the [rules section](instrumentation/rules.md).

The code executed when an action is invoked is defined through the `value` configuration property.
In YAML, this is simply a string. InspectIT however will interpret this string as a Java expression to evaluate. The result value of this expression will be used as result for the action invocation.

Note that the code will not be interpreted at runtime, but instead inspectIT Ocelot will compile the expression to bytecode to ensure maximum efficiency.
As indicated by the manual primitive boxing performed for `timestamp_nanos` the compiler has some restrictions. For example Autoboxing is not supported.
However, actions are expected to return Objects, therefore manual boxing has to be performed. Under the hood, inspectIT uses the [javassist](http://www.javassist.org/) library, where all imposed restrictions can be found.
The most important ones are that neither Autoboxing, Generics, Anonymous Classes or Lambda Expressions are supported.

After actions have been compiled, they are placed in the same class loader as the class you instrument with them. This means that they can access any class that your application class could also access.

> Even if your action terminates with an exception or error, inspectIT will make sure that this does not affect your application. InspectIT will print information about the error and the faulting action. The execution of the action in the rule where the failure occurred will be disabled until you update your configuration.

### Input Parameters

As previously mentioned actions are also free to define any kind of _input parameters_ they need. This is done using the `input` configuration property.
This property maps the names of the input parameters to their expected Java type.
For example, the `a_timing_elapsedMillis` action declares a single input variable named `sinceNanos` which has the type `long`. Note that for input parameters automatic primitive unboxing is supported.

Another example where the action even defines multiple inputs is `a_string_replace_all`. Guess what this action does? [Hint](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#replaceAll-java.lang.String-java.lang.String).

The fourth example shown above is `a_method_getClassFQN`, which uses the _special_ input parameter `_class`. The fact that these variables are special is indicated by the leading underscore. When normally invoking actions from rules, the user has to take care that all input parameters are assigned a value. For special input parameters inspectIT automatically assigned the desired value. This means that for example `a_method_getClassFQN` can be called without manually assigning any parameter, like it was done in the initial example in the [rules section](instrumentation/rules.md). An overview of all available special input parameters is given below:

|Parameter Name|Type| Description
|---|---|---|
|`_methodName`| [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)| The name of the instrumented method within which this action is getting executed.
|`_class`| [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)| The class declaring the instrumented method within which this action is getting executed.
|`_parameterTypes`| [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)[]| The types of the parameters which the instrumented method declares for which the action is executed.
|`_this`| (depends on context) | The this-instance in the context of the instrumented method within which this action is getting executed.
|`_args`| [Object](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html)[] | The arguments with which the instrumented method was called within which this action is getting executed. The arguments are boxed if necessary and packed into an array.
|`_arg0,_arg1,...,_argN`| (depends on context)| The N-th argument with which the instrumented method was called within which this action is getting executed.
|`_returnValue`| (depends on context) | The value returned by the instrumented method within which this action is getting executed. If the method terminated with an exception or the action is executed in the entry phase this is `null`.
|`_thrown`| [Throwable](https://docs.oracle.com/javase/8/docs/api/java/lang/Throwable.html)| The exception thrown by the instrumented method within which this action is getting executed. If the method returned normally or the action is executed in the entry phase this is `null`.
|`_context`| [InspectitContext](https://github.com/inspectIT/inspectit-ocelot/blob/master/inspectit-ocelot-bootstrap/src/main/java/rocks/inspectit/ocelot/bootstrap/exposed/InspectitContext.java) | Gives direct read and write access to the current [context](#data-propagation). Can be used to implement custom data propagation.
|`_attachments`| [ObjectAttachments](https://github.com/inspectIT/inspectit-ocelot/blob/master/inspectit-ocelot-bootstrap/src/main/java/rocks/inspectit/ocelot/bootstrap/exposed/ObjectAttachments.java) | Allows you to attach values to objects instead of to the control flow, as done via `_context`.


### Multiple statements and Imports

Actions can easily become more complex, so that a single expression is not sufficient for expressing the functionality.
For this purpose we introduced the `value-body` configuration property for actions as an alternative to `value`.
`value-body` allows you to specify a Java method body which returns the result of the action. The body is given without surrounding curly braces. One example action from the default configuration making use of this is given below:

```yaml
inspectit:
  instrumentation:
    actions:
      'a_get_servlet_request_path':
        imports:
          - 'javax.servlet'
          - 'javax.servlet.http'
        input:
          _arg0: ServletRequest
        value-body: |
          'if(_arg0 instanceof HttpServletRequest) {
            return java.net.URI.create(((HttpServletRequest)_arg0).getRequestURI()).getPath();
          }
          return null;'
```

This action is designed to be applied on the Servlet API [doFilter](https://javaee.github.io/javaee-spec/javadocs/javax/servlet/Filter.html#doFilter-javax.servlet.ServletRequest-javax.servlet.ServletResponse-javax.servlet.FilterChain) and
[service](https://javaee.github.io/javaee-spec/javadocs/javax/servlet/Servlet.html#service-javax.servlet.ServletRequest-javax.servlet.ServletResponse) methods.
 It's purpose is to extract HTTP path, however in the servlet API it is not guaranteed that the `ServletRequest` is a `HttpServletRequest`.
 For this reason the action performs an instance-of check only returning the HTTP path if it is available, otherwise `null`.

Normally, all non `java.lang.*` types have to be referred to using their fully qualified name, as done for `java.net.URI` in the example above. However, just like in Java you can import packages using the `import` config option. In this example this allows us to refer to `ServletRequest` and `HttpServletRequest` without using the fully qualified name.

## Defining Rules

Rules glue together [scopes](instrumentation/scopes.md) and [actions](instrumentation/rules.md#actions) to define which actions you want to perform on which application methods.

As you might have noticed, the initial example rule shown in the [rules section](instrumentation/rules.md) did not define any reference to a scope. This is because this rule originates from the default configuration of inspectIT Ocelot, where we don't know yet of which methods you want to collect the response time. Therefore this rule is defined without scopes, but you can easily add some in your own configuration files:

```yaml
inspectit:
  instrumentation:
    rules:

      'r_record_method_duration':
        scopes:
          's_my_first_scope': true
          's_my_second_scope': true
```

With this snippet we defined that the existing rule `record_method_duration` gets applied on the two scopes named `my_first_scope` and `my_second_scope`. The `scopes` configuration option maps scope names to `true` or `false`. The rule will be applied on all methods matching any scope where the value is `true`.

Rules define their action within three _phases_:

* **Entry Phase:** The actions defined in this phase get invoked directly before the body of the instrumented method. You can imagine that these actions are "inlined" at the very top of every method instrumented by the given rule.

* **Exit Phase:** The actions defined in this phase get invoked after the body of the instrumented method has terminated. This can be the method either returning normally or throwing an exception. You can imagine that these actions are placed in a `finally` block of a `try` block surrounding the body of the instrumented method.

* **Metrics Phase:** These actions are executed directly after the _exit phase_.
Here, only values for metrics are recorded. No actions will be executed here.

The actions performed in this phases are defined in rules under the `entry`, `exit` and `metrics` configuration options. In the entry and in the exit phase the actions you perform are invocations of [actions](#actions). Please see the [Invoking Actions](#invoking-actions) section for information on how this is done.

In the _metrics phase_ you only can collect metrics, this is explained in the [Collecting Metrics](#collecting-metrics) section.

### Invoking Actions

In this section you will find out how to collect data in the entry and exit phase of rules by invoking [actions](#actions) and storing the results in the [inspectIT context](#data-propagation).

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
This means the keys used in the dictionaries define the data key for which a value is being defined. Correspondingly, the assigned value defines which action is invoked to define the value of the data key.

In the example above `method_entry_time` is a data key. The action which is invoked is defined through the `action` configuration option. In this case, it is the action named `timestamp_nanos`.

#### Assigning Input Parameter Values

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

Note that when assigning a constant value, inspectIT Ocelot automatically converts the given value to the type expected by the action. This is done using the [Spring Conversion Service](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/convert/ConversionService.html). For example, if your action expects a parameter of type `java.time.Duration`, you can simply pass in `"42s"` as constant.

As you might have noticed, `data-input` and `constant-input` are again YAML dictionaries.
This allows you to assign values for actions which expect multiple input parameters.
You can also mix which parameters you assign from data and which from constants:


```yaml
#inspectit.instrumentation.rules is omitted here
'r_example_rule':
  entry:
    'a_bye_world_text':
      action: 'a_string_replace_all'
      data-input:
        'string': 'hello_world_text'
      constant-input:
        'regex': 'Hello'
        'replacement': 'Bye'
```

As expected given the [definition](#actions) of the `string_replace_all` action, the value of `bye_world_text` will be `"Bye World!"`

#### Adding Conditions

It is possible to add conditions to action invocations. The invocation will only occur if the specified condition is met. Currently, the following configuration options can be used:

|Config Option| Description
|---|---|
|`only-if-null`| Only executes the invocation if the value assigned with the given data key is null.
|`only-if-not-null`| Only executes the invocation if the value assigned with the given data key is not null.
|`only-if-true`| Only executes the invocation if the value assigned with the given data key is the boolean value `true`.
|`only-if-false`| Only executes the invocation if the value assigned with the given data key is the boolean value `false`.


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
to `"My-Application"`. However, this assignment is only performed if `application_name` previously was null, meaning that no value has been assigned yet. This mechanism is in particular useful when `application_name` is [down propagated](#data-propagation).

If multiple conditions are given for the same action invocation, the invocation is only executed if *all* conditions are met.

#### Execution Order

As we can use data values for input parameters and for conditions, action invocations can depend on another. This means that a defined order on action executions within each phase is required for rules to work as expected.

As all invocations are specified under the `entry` or the `exit` config options which are YAML dictionaries, the order they are given in the config file does not matter. YAML dictionaries do not maintain or define an order of their entries.

However, inspectIT Ocelot _automatically_ orders the invocations for you correctly.
For each instrumented method the agent first finds all rules which have scopes matching the given method. Afterwards, these rules get combined into one "super"-rule by simply merging the `entry`, `exit` and `metrics` phases.

Within the `entry` and the `exit` phase, actions are now automatically ordered based on their dependencies. E.g. if the invocation writing `data_b` uses `data_a` as input, the invocation writing `data_a` is guaranteed to be executed first! Whenever you use a data value as value for a parameter or in a condition, this will be counted as a dependency.

In some rare cases you might want to change this behaviour. E.g. in tracing context you want to store the [down propagated](#data-propagation) `span_id` in `parent_span`, before the current method assigns a new `span_id`. This can easily be realized using the `before` config option for action invocations:

```yaml
#inspectit.instrumentation.rules is omitted here
'r_example_rule':
  entry:
    'parent_span':
      action: 'a_assign_value'
      data-input:
        'value': 'span_id'
    'before':
      'span_id': true
```

### Collecting Metrics

Metrics collection is done in the metrics phase of a rule, which can be configured using the `metrics` option:

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
The prerequisite for this is that tags have been declared in the [metric definition](metrics/custom-metrics.md) and [configured to be used as tags](#defining-the-behaviour).
Constant tags always have same values as defined in the configuration.
The data tags try to resolve value from the data key, which is previously wrote in the exit phase.
If data key for the data tag can not be found, then corresponding tag is omitted.
Note that `data-tags` have higher priority than the `constant-tags`, thus if both section define a tag with same key, the data tag will overwrite the constant one if it can be resolved.

> All [common tags](metrics/common-tags.md) are always included in the metric recording and do not need explicit specification.

:::warning Short notation is deprecated
The default way to specify metric collection in Ocelot versions up to and including v1.0 was a so called short notation, which is now deprecated and will be invalid in future Ocelot releases:

```yaml
#inspectit.instrumentation.rules is omitted here
'r_example_rule':
  #...
  exit:
    'method_duration':
      #action invocation here....

  metrics:
    '[method/duration]' : 'method_duration'
    '[some/other/metric]' : '42'
```

As short notation does not allow specification of tags to be recorded, using the short notation means that only common tags will be collected.
We advise to migrate to the new configuration style immediately.
Due to the way configuration loading works, the short notation will always take precedence over the explicit notation. This means that you cannot override settings made with the short-notation by using the explicit notation.
:::

### Collecting Traces

The inspectIT Ocelot agent allows you to record method invocations as [OpenCensus spans](https://opencensus.io/tracing/span/).

#### Tracing Methods

In order to make your collected spans visible, you must first set up a [trace exporter](tracing/trace-exporters.md).

Afterwards you can define that all methods matching a certain rule will be traced:

```yaml
inspectit:
  instrumentation:
    rules:
      'r_example_rule':
        tracing:
          start-span: true
```

For example, using the previous configuration snippet, each method that matches the scope definition of the `example_rule` rule will appear within a trace.
Its appearance can be customized using the following properties which can be set in the rule's `tracing` section.

|Property |Default| Description
|---|---|---|
|`start-span`|`false`|If true, all method invocations of methods matching any scope of this rule will be collected as spans.
|`name`|`null`|Defines a data key whose value will be used as name for the span. If it is `null` or the value for the data key is `null`, the fully qualified name of the method will be used. Note that the value for the data key must be written in the entry section of the rule at latest!
|`kind`|`null`|Can be `null`, `CLIENT` or `SERVER` corresponding to the [OpenCensus values](https://opencensus.io/tracing/span/kind/).
|`attributes`|`{}` (empty dictionary) |Maps names of attributes to data keys whose values will be used on exit to populate the given attributes.

Commonly, you do not want to have the fully qualified name of the instrumented method as span name. For example, for HTTP requests you typically want the HTTP path as span name. This behaviour can be customized using the `name` property:

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

> The name must exist at the end of the entry section and cannot be set in the exit section.

#### Trace Sampling

It is often desirable to not capture every trace, but instead [sample](https://opencensus.io/tracing/sampling/) only a subset.
This can be configured using the `sample-probability` setting under the `tracing` section:

```yaml
inspectit:
  instrumentation:
    rules:
      'r_servlet_api_service':
        tracing:
          start-span: true
          sample-probability: '0.2'
```

The example shown above will ensure that only 20% of all traces starting at the given rule will actually be exported.
Instead of specifying a fixed value, you can also specify a data key here, just like for `name`.
In this case, the value from the given data key is read and used as sampling probability.
This allows you for example to vary the sample probability based on the HTTP url.

If no sample probability is defined for a rule, the [default probability](tracing/tracing.md) is used.

#### Adding Attributes

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

#### Visualizing Span Errors

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

The value of the `error-status` property can be any value from the context or any [special variable](#input-parameters).

When the instrumented method finishes, Ocelot will read the value of the given variable.
If the value is neither `null` nor `false`, the span will be marked as an error.

In the example above, the special variable `_thrown` is used to define the error status.
This means if `_thrown` is not null (which means the method threw an exception), the span will be marked as error.

#### Adding Span Conditions

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
The conditions that can be defined are equal to the ones of actions, thus, please see the [action conditions description](#adding-conditions) for detailed information.

#### Auto-Tracing

:::warning Experimental Feature
Please note that this is an experimental feature.
This feature is currently experimental and can potentially have a high performance impact. We do **not** recommend using it for production environments yet!
:::

With the shown approach traces will only contain exactly the methods you instrumented.
Often however, one observes certain methods taking a long time without knowing where exactly the time is spent.
The "auto-tracing" feature can be used to solve this problem.

When auto-tracing is enabled, inspectIT Ocelot uses a profiler-like approach for recording traces.
With auto-tracing, stack traces of threads are collected periodically. Based on these samples, inspectIT Ocelot will reconstruct
an approximate trace showing where the time was spent.

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


#### Tracing Asynchronous Invocations

With the previous shown settings, it is possible to add an instrumentation which creates exactly one span per invocation of an instrumented method.
Especially in asynchronous scenarios, this might not be the desired behaviour:
For these cases inspectIT Ocelot offers the possibility to record multiple method invocations into a single span.
The resulting span then has the following properties:

* the span starts with the first invoked method and ends as soon as the last one returns
* all attributes written by each method are combined into the single span
* all invocations made from the methods which compose the single span will appear as children of this span

This can be configured by defining for rules that they (a) can continue existing spans and (b) can optionally end the span they started or continued.

Firstly, it is possible to "remember" the span created or continued using the `store-span` option:

```yaml
    rules:
      'r_span_starting_rule':
        tracing:
          start-span: true
          store-span: 'my_span_data'
          end-span: false
```

With this option, the span created at the end of the entry phase will be stored in the context with the data key `my_span_data`. Usually this span reference is then extracted from the context and attached to an object via the [_attachments](#input-parameters).

Without the `end-span: false` definition above, the span would be ended as soon as the instrumented method returns.
By setting `end-span` to false, the span is kept open instead. It can then be continued when another method is executed as follows:

```yaml
    rules:
      'r_span_finishing_rule':
        tracing:
          continue-span: 'my_span_data'
          end-span: true # actually not necessary as it is the default value
```

Methods instrumented with this rule will not create a new span. Instead at the end of the entry phase the data for the key `my_span_data` is read from the context. If it contains a valid span written via `store-span`, this span is continued in this method. This implies that all spans started by callees of this method will appear as children of the span stored in `my_span_data`. In addition, this rule also then causes the continued span to end with the execution of the method due to the `end-span` option. This is not required to happen: a span can be continued by any number of rules before it is finally ended.

It also is possible to define rules for which both `start-span` and `continue-span` is configured.
In this case, the rule will first attempt to continue the existing span. Only if this fails (e.g. because the specified span does not exist yet or the conditions are not met) a new span is started.

Again, conditions for the span continuing and span ending can be specified just like for the span starting.
The properties `continue-span-conditions` and `end-span-conditions` work just like `start-span-conditions`.

### Modularizing Rules

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
