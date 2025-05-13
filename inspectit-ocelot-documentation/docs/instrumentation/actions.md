---
id: actions
title: Actions
---

Actions are the tool for extracting arbitrary data from your application or the context.
They are effectively lambda-like functions you can invoke from the entry and the exit phase of rules. 
They are defined by (A) specifying their input parameter and (B) giving a Java code snippet which defines 
how the result value is computed from these.

## Defining Actions

Again, this is best explained by giving some simple examples extracted from the inspectIT Ocelot default configuration:

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

The code executed when an action is invoked is defined through the `value` configuration property.
In YAML, this is simply a string. InspectIT however will interpret this string as a Java expression to evaluate. The result value of this expression will be used as result for the action invocation.

Note that the code will not be interpreted at runtime, but instead inspectIT Ocelot will compile the expression 
to bytecode at runtime to ensure maximum efficiency.
As indicated by the manual primitive boxing performed for `timestamp_nanos` the compiler has some restrictions. 
For example Autoboxing is not supported!
However, actions are expected to return `Object` instances, therefore manual boxing has to be performed.
Under the hood, inspectIT uses the [javassist](http://www.javassist.org/) library, where all imposed restrictions can be found.
The most important ones are that neither Autoboxing, Generics, Anonymous Classes nor Lambda expressions are supported.

After actions have been compiled, they are placed in the same class loader as the class you instrument with them. 
**This means that they can access any class that your application class could also access.**

:::note
**Exceptions within actions**

Even if your action terminates with an exception or error, inspectIT will make sure that this does not affect your application. 
InspectIT will print information about the error and the faulting action. The execution of the action in the rule where the 
failure occurred will be disabled until you update your instrumentation configuration.
:::

## Input Parameter

As previously mentioned actions are also free to define any kind of _input parameter_ they need. 
This is done using the `input` configuration property.
This property maps the names of the input parameter to their expected Java type.
For example, the `a_timing_elapsedMillis` action declares a single input variable named `sinceNanos`
which has the type `long`. Note that for input parameter automatic primitive unboxing is supported.

Another example where the action even defines multiple inputs is `a_string_replace_all`.
Guess what this action does? ([Hint](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#replaceAll-java.lang.String-java.lang.String))

### Special Parameter

The fourth example shown above is `a_method_getClassFQN`, which uses the _special_ input parameter `_class`.
The fact that these variables are special is indicated by the leading underscore.
When normally invoking actions from rules, the user has to take care that all input parameter are assigned a value.

For special input parameter inspectIT automatically assigns the desired value.
This means that for example `a_method_getClassFQN` can be called without manually assigning any parameter, 
like it was done in the example of the [rules section](instrumentation/rules.md). An overview of all available special 
input parameter is given below:

| Parameter Name          | Type                                                                                                                                                                                    | Description                                                                                                                                                                                        |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `_methodName`           | [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)                                                                                                               | The name of the instrumented method within which this action is getting executed.                                                                                                                  |
| `_class`                | [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)                                                                                                                 | The class declaring the instrumented method within which this action is getting executed.                                                                                                          |
| `_parameterTypes`       | [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)[]                                                                                                               | The types of the parameter which the instrumented method declares for which the action is executed.                                                                                                |
| `_this`                 | (depends on context)                                                                                                                                                                    | The this-instance in the context of the instrumented method within which this action is getting executed.                                                                                          |
| `_args`                 | [Object](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html)[]                                                                                                             | The arguments with which the instrumented method was called within which this action is getting executed. The arguments are boxed if necessary and packed into an array.                           |
| `_arg0,_arg1,...,_argN` | (depends on context)                                                                                                                                                                    | The N-th argument with which the instrumented method was called within which this action is getting executed.                                                                                      |
| `_returnValue`          | (depends on context)                                                                                                                                                                    | The value returned by the instrumented method within which this action is getting executed. If the method terminated with an exception or the action is executed in the entry phase this is `null`. |
| `_thrown`               | [Throwable](https://docs.oracle.com/javase/8/docs/api/java/lang/Throwable.html)                                                                                                         | The exception thrown by the instrumented method within which this action is getting executed. If the method returned normally or the action is executed in the entry phase this is `null`.         |
| `_context`              | [InspectitContext](https://github.com/inspectIT/inspectit-ocelot/blob/master/inspectit-ocelot-bootstrap/src/main/java/rocks/inspectit/ocelot/bootstrap/exposed/InspectitContext.java)   | Gives direct read and write access to the current [context](instrumentation/data-propagation.md). Can be used as data dictionary or to implement custom data propagation.                          |
| `_attachments`          | [ObjectAttachments](https://github.com/inspectIT/inspectit-ocelot/blob/master/inspectit-ocelot-bootstrap/src/main/java/rocks/inspectit/ocelot/bootstrap/exposed/ObjectAttachments.java) | Allows you to attach values to Java objects instead of to the control flow, as done via `_context`. This enables sharing data across multiple threads.                                             |

## Multiple Statements and Imports

Actions can easily become more complex, so that a single expression is not sufficient for expressing the functionality.
For this purpose we introduced the `value-body` configuration property for actions as an alternative to `value`.
`value-body` allows you to specify a Java method body which returns the result of the action. 
The body is given without surrounding curly braces. One example action from the default configuration making use of this is given below:

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
          if(_arg0 instanceof HttpServletRequest) {
            return java.net.URI.create(((HttpServletRequest)_arg0).getRequestURI()).getPath();
          }
          return null;
```

This action is designed to be applied on the Servlet API [doFilter](https://javaee.github.io/javaee-spec/javadocs/javax/servlet/Filter.html#doFilter-javax.servlet.ServletRequest-javax.servlet.ServletResponse-javax.servlet.FilterChain) and
[service](https://javaee.github.io/javaee-spec/javadocs/javax/servlet/Servlet.html#service-javax.servlet.ServletRequest-javax.servlet.ServletResponse) methods.
Its purpose is to extract HTTP path, however in the servlet API it is not guaranteed that the `ServletRequest` is a `HttpServletRequest`.
For this reason the action performs an instance-of check only returning the HTTP path if it is available, otherwise `null`.

Normally, all non `java.lang.*` types have to be referred to using their fully qualified name, as done for `java.net.URI` 
in the example above. However, just like in Java you can import packages using the `import` config option. 
In this example this allows us to refer to `ServletRequest` and `HttpServletRequest` without using the fully qualified name.

## Default Actions

InspectIT Ocelot provides a large set of default actions, which can be used within any configuration.
Such actions allow you for example to assign values to data keys, print debug information or replace strings via regex.
You can find the full set of default actions [here](https://github.com/inspectIT/inspectit-ocelot/tree/master/inspectit-ocelot-config/src/main/resources/rocks/inspectit/ocelot/config/default/instrumentation/actions/_shared).

Below, some important actions are explained in more detail.
The examples show how to apply the actions within [rules](instrumentation/rules.md). 
In the upcoming section you will find more detailed information about [Invoking Actions](instrumentation/rules.md#invoking-actions).

### Assigning Values

Data keys can only be used withing rules (e.g. for metric tags or tracing attributes), if they have an assigned value.
You can assign values via any action. If you would like to assign a constant value or copy another value to a data key,
you can use the actions `a_assign_value`, `a_assign_null`, `a_assign_true` or `a_assign_false`.

```yaml
rules:
  r_rule:
    entry:
      some_value:
        action: a_any_action
      copied_value:
        action: a_assign_value
        data-input:
          value: some_value
      constant_value:
        action: a_assign_value
        constant-input:
          value: "constant"
      is_entry:
        action: a_assign_true
```

### Attaching Values

Object attachments allow you to store and access data outside the current control flow.
The inspectIT agent provides one global dictionary, where any Java object can serve as a key, 
which points to another dictionary of data keys and values. 
In Java, this would be resembled by a `Map<Object, Map<String, Object>>`.
For instance, you would like to use recorded data within two separate threads. 
If these threads are accessing the same Java object, you can attach this object to the global dictionary 
together with its data key and value in the first thread. After that, you can read the data in the second thread 
via the attached Java object. 
For this, we provide the default actions `a_attachment_get`, `a_attachment_put` and `a_attachment_remove`.

```yaml
rules:
  r_first: # first thread
    entry:
      some_value: # record value
        action: a_any_action
      shared_object: # somehow access shared object
        action: a_assign_value
        data-input: 
          value: _arg0
      attach:
        action: a_attachment_put
        data-input:
          target: shared_object
          value: some_value
        constant-input:
          key: 'my-key'

  r_second: # second thread
    entry:
      shared_object: # somehow access shared object
        action: a_assign_value 
        data-input: 
          value: _arg0
      some_value: # get previously recorded value
        action: a_attachment_get
        data-input:
          target: shared_object
        constant-input:
          key: 'my-key
```

### Debugging

We provide two debug actions, which might help you with understanding the data flow of your instrumentation:
`a_debug_println` and `a_debug_println_2`. These will print the provided values at the standard system output.

```yaml
rules:
  r_rule:
    entry:
      some_value:
        action: a_any_action
      another_value:
        action: a_another_action
      debug:
        action: a_debug_println
        data-input: 
          value: some_value
      debug_2:
        action: a_debug_println_2
        data-input:
          a: some_value
          b: another_value
```

### Logical Operators

We also provide some logical operators for checking specific conditions of data values:
`a_logic_isNull`, `a_logic_isNotNull`, `a_logic_and`, `a_logic_or` and `a_logic_isTrueOrNotNull`.

```yaml
rules:
  r_rule:
    entry:
      isEntry:
        action: a_assign_true
      isFalse:
        action: a_logic_isNull
        data-input:
          value: isEntry
      isTrue:
        action: a_logic_isNull
        data-input: 
          value: isExit # data value not set
      isAlsoFalse:
        action: a_logic_and
        data-input:
          a: isFalse
          b: isTrue
```

### Replacing Strings via Regex

If you would like to sanitize some strings, for instance to remove sensitive data, you can use the following default
actions: `a_regex_replaceAll`, `a_regex_replaceAll_multi`, `a_regex_replaceMatch`, `a_regex_replaceMatch_multi`, 
`a_regex_replaceFirst`, `a_regex_extractFirst`, `a_regex_extractFirst_multi`, `a_regex_extractMatch` 
and `a_regex_extractMatch_multi`.

```yaml
rules:
  r_rule:
    entry:
      http_path_raw:
        action: a_any_action
      http_path: # sanitize http path
        action: a_regex_replaceMatch
        data-input:
          string: http_path_raw
        constant-input:
          'pattern': '\/apps\/.+\/.+'
          'replacement': '/apps/{service}/{location}'
```
