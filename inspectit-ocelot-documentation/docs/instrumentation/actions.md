---
id: actions
title: Actions
---

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

:::note
Even if your action terminates with an exception or error, inspectIT will make sure that this does not affect your application. InspectIT will print information about the error and the faulting action. The execution of the action in the rule where the failure occurred will be disabled until you update your configuration.
:::

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
