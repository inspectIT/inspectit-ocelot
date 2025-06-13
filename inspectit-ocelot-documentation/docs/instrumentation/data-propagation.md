---
id: data-propagation
title: Data Propagation
---

We can collect any amount of data in both the entry and the exit section of an instrumented method. 
Each data is hereby identified by its name, the _data key_.
Internally, inspectIT creates a dictionary like object each time **an instrumented method is executed**. 
This object is basically a local variable for the method. **Whenever data is written**, inspectIT stores the value 
under the given data key in this dictionary. Similarly, whenever data is read, inspectIT looks it up based on the 
data key in the dictionary. 

This dictionary is called **inspectIT context**.

If the inspectIT context was truly implemented as explained above, all data would be only visible in the method where it was collected. 
This however often is not the desired behaviour.
Consider the following example: you instrument the entry method of your HTTP server and collect the request URL as data there. 
You now of course want this data to be visible as tag for metrics collected in methods called by your entry point. 
With the implementation above, the request URL would only be visible within the HTTP entry method.

For this reason the inspectIT context implements _data propagation_.

* **Down Propagation:** Data collected in your instrumented method will also be visible to all methods directly 
  or indirectly called by your method. This behaviour already comes [with the OpenCensus Library for tags](https://opencensus.io/tag/#propagation).
* **Up Propagation:** Data collected in your instrumented method will be visible to the methods which caused the 
  invocation of your method. This means that all methods which lie on the call stack will have access to the data written by your method
* **Browser Propagation:** An additional form of propagation. Data collected in your instrumented method will be 
  stored inside a data storage, which is accessible to the outside. 

Up- and down propagation can also be combined. In this case then the data is attached to the control flow, 
meaning that it will appear as if its value will be passed around with every method call and return.

Also note, that you should only assign Java Objects into data keys and not native data types, like _boolean_.

The second aspect of propagation to consider is the _level_. Does the propagation happen within each thread separately 
or is it propagated across threads? Also, what about propagation across JVM borders, e.g. one micro-service 
calling another one via HTTP? In inspectIT Ocelot we provide the following two settings for the propagation level.

* **JVM local:** The data is propagated within the JVM, even across thread borders. The behaviour when data moves from one thread to another is defined through [Special Sensors](instrumentation/special-sensors.md).
* **Global:** Data is propagated within the JVM and even across JVM borders. For example, when an application issues an HTTP request, the globally down propagated data is added to the headers of the request. When the response arrives, up propagated data is collected from the response headers. This protocol specific behaviour is realized through default instrumentation rules provided with the agent, but can be extended as needed.

## Defining the Behaviour

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

      #this data will be written to the inspectIT browser data storage
      'database_tag': {browser-propagation: true}
      #this data can be written by the browser
      'transaction_id': {down-propagation: "JVM_LOCAL", browser-propagation: true}
```

Under `inspectit.instrumentation.data`, the data keys are mapped to their desired behaviour.
The configuration options are the following:

| Config Property       | Default                                                                                                                                                          | Description                                                                                                                                                                                             |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `down-propagation`    | `JVM_LOCAL` if the data key is also a [common tag](metrics/common-tags.md), `NONE` otherwise                                                                     | Configures if values for this data key propagate down and the level of propagation. Possible values are `NONE`, `JVM_LOCAL` and `GLOBAL`. If `NONE` is configured, no down propagation will take place. | 
| `up-propagation`      | `NONE`                                                                                                                                                           | Configures if values for this data key propagate up and the level of propagation. Possible values are `NONE`, `JVM_LOCAL` and `GLOBAL`. If `NONE` is configured, no up propagation will take place.     | 
| `browser-propagation` | `false`                                                                                                                                                          | If true, this data will be written to the inspectIT browser data storage                                                                                                                                | 
| `is-tag`              | `true` if the data key is also a [common tag](metrics/common-tags.md) or is used as tag in any [metric definition](metrics/custom-metrics.md), `false` otherwise | If true, this data will act as a tag when metrics are recorded. This does not influence propagation.                                                                                                    | 

Note that you are free to use data keys without explicitly defining them in the `inspectit.instrumentation.data` section. 
In this case simply all settings will have their default value.

## Interaction with OpenCensus Tags

As explained previously, our inspectIT context can be seen as a more flexible variation of OpenCensus tags. 
In fact, we designed the inspectIT context so that it acts as a superset of the OpenCensus TagContext.

Firstly, when an instrumented method is entered, a new inspectIT context is created. 
At this point, it imports any tag values published by OpenCensus directly as data. 
This also includes the [common tags](metrics/common-tags.md) created by inspectIT. This means, that you can simply read (and override) 
values for common tags such as `service.name` or `host.name` at any rule.

The integration is even deeper if you configured the agent to also extract the metrics from manual instrumentation 
in your application via OpenCensus.
Firstly, if a method instrumented by inspectIT Ocelot is executed within a TagContext opened by your application,
these application tags will also be visible in the inspectIT context. Secondly, after the execution of the entry phase 
of each rule, a new TagContext is opened making the tags written there accessible to metrics collected by your application. 
Hereby, only data for which down propagation was configured to be `JVM_LOCAL` or greater and for which `is-tag` is true 
will be visible as tags.

## Baggage

As mentioned previously, data tags can be used across JVM borders when configured for `GLOBAL` propagation.
Then, the data can be read from incoming HTTP headers or can be written into outgoing HTTP headers.
inspectIT Ocelot will always read or write all globally propagated data via the `baggage` header.
The header contains a list of key-value pairs in the following format:

```
baggage: key1=value1,key2=value2
```

You can find more information about the header format [here](https://github.com/w3c/baggage/blob/main/baggage/HTTP_HEADER_FORMAT.md).

Within [actions](instrumentation/actions.md), data can be read from or written into HTTP headers. 
The _inspectIT context_ `_context` [special parameter](instrumentation/actions.md#special-parameter) offers 
multiple methods for `GLOBAL` propagation. 

- `readDownPropagationHeaders(Map<String,String> headers)`: Read the baggage header from incoming HTTP request headers
- `readUpPropagationHeaders(Map<String,String> headers)`: Read the baggage header from returned HTTP response headers
- `getDownPropagationHeaders()`: Get the recorded baggage header to include them in outgoing HTTP request headers 
- `getUpPropagationHeaders()`: Get the recorded baggage header to include them in answering HTTP response headers

With these methods data can only be read from or written into the current _inspectIT context_. The data has to be further propagated
to be accessible in other locations, e.g. other [scopes](instrumentation/scopes.md).
The [inspectIT default instrumentation](https://github.com/inspectIT/inspectit-ocelot/tree/master/inspectit-ocelot-config/src/main/resources/rocks/inspectit/ocelot/config/default/instrumentation/actions/http) provides some examples for using these methods.

:::important
Please note that up to version `2.6.10` the header name was `correlation-context` instead of `baggage`. The header value format remained unchanged.
:::

## Browser Propagation (deprecated)

<!-- 
Please rework (or remove) browser propagation, so it is no longer possible to speak with the agent from outside
See: https://github.com/inspectIT/inspectit-ocelot/issues/1703
-->

Data enabled for browser propagation will be stored inside a special dictionary.
This dictionary can be exposed via REST-API, if the [Tags-HTTP-Exporter](tags/tags-exporters.md#http-exporter) is enabled.
A browser can read this data via GET-requests.

Additionally, a browser can write data into the storage via PUT-requests, if browser- as well as down-propagation is 
enabled for a data key. The data written by the browser will be stored in the _inspectIT context_.
Please note, before writing or reading browser propagation data, you need to provide a session-ID inside the request-header.
After that, all data belonging to the current session will be stored behind this session-ID.
For more information, see [Tags-HTTP-Exporter](tags/tags-exporters.md#http-exporter).

:::important
We recommend to propagate data via [baggage](#baggage) instead, so the agent does not expose an API.
:::
