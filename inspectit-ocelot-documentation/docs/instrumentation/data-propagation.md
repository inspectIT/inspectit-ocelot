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

If the _inspectIT context_ was truly implemented as explained above, all data would be only visible in the method where it was collected. 
This however often is not the desired behaviour.
Consider the following example: you instrument the entry method of your HTTP server and collect the request URL as data there. 
You now of course want this data to be visible as tag for metrics collected in methods called by your entry point. 
With the implementation above, the request URL would only be visible within the HTTP entry method.

## Up- & Down-Propagation

For this reason the _inspectIT context_ implements _data propagation_.

* **Down Propagation:** Data collected in your instrumented method will also be visible to all methods directly 
  or indirectly called by your method. This behaviour already comes [with the OpenCensus Library for tags](https://opencensus.io/tag/#propagation).
* **Up Propagation:** Data collected in your instrumented method will be visible to the methods which caused the 
  invocation of your method. This means that all methods which lie on the call stack will have access to the data written by your method

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

      # we allow the application to be defined at the beginning and to be down propagated from there
      'application': {down-propagation: "GLOBAL", is-tag: "true"}

      # this data will only be visible locally in the method where it is collected
      'http_method': {down-propagation: "NONE"}
      'http_status': {down-propagation: "NONE"}

      # this data will be written to the inspectIT session storage
      'database_table': {session-storage: true}
      'browser': {session-storage: true}
```

Under `inspectit.instrumentation.data`, the data keys are mapped to their desired behaviour.
The configuration options are the following:

| Config Property                    | Default                                                                                                                                                          | Description                                                                                                                                                                                             |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `down-propagation`                 | `JVM_LOCAL` if the data key is also a [common tag](metrics/common-tags.md), `NONE` otherwise                                                                     | Configures if values for this data key propagate down and the level of propagation. Possible values are `NONE`, `JVM_LOCAL` and `GLOBAL`. If `NONE` is configured, no down propagation will take place. | 
| `up-propagation`                   | `NONE`                                                                                                                                                           | Configures if values for this data key propagate up and the level of propagation. Possible values are `NONE`, `JVM_LOCAL` and `GLOBAL`. If `NONE` is configured, no up propagation will take place.     | 
| `session-storage`                  | `false`                                                                                                                                                          | If true, this data will be written to the inspectIT session storage                                                                                                                                     |
| `is-tag`                           | `true` if the data key is also a [common tag](metrics/common-tags.md) or is used as tag in any [metric definition](metrics/custom-metrics.md), `false` otherwise | If true, this data will act as a tag when metrics are recorded. This does not influence propagation.                                                                                                    | 

Note that you are free to use data keys without explicitly defining them in the `inspectit.instrumentation.data` section. 
In this case simply all settings will have their default value.

### Interaction with OpenCensus Tags

As explained previously, our _inspectIT context_ can be seen as a more flexible variation of OpenCensus tags. 
In fact, we designed the _inspectIT context_ so that it acts as a superset of the OpenCensus TagContext.

Firstly, when an instrumented method is entered, a new i*nspectIT context* is created. 
At this point, it imports any tag values published by OpenCensus directly as data. 
This also includes the [common tags](metrics/common-tags.md) created by inspectIT. This means, that you can simply read (and override) 
values for common tags such as `service.name` or `host.name` at any rule.

The integration is even deeper if you configured the agent to also extract the metrics from manual instrumentation 
in your application via OpenCensus.
Firstly, if a method instrumented by inspectIT Ocelot is executed within a TagContext opened by your application,
these application tags will also be visible in the _inspectIT context_. Secondly, after the execution of the entry phase 
of each rule, a new TagContext is opened making the tags written there accessible to metrics collected by your application. 
Hereby, only data for which down propagation was configured to be `JVM_LOCAL` or greater and for which `is-tag` is true 
will be visible as tags.

## Data Tag Storages

Besides the propagation via _inspectIT context_ and special sensors, there are some possibilities
to exchange data via tag storages:

* **[Action Cache](instrumentation/actions.md#caching)**: Data is stored within one action and can be accessed in every call of this particular action.
* **[Object Attachments](instrumentation/actions.md#attaching-values)**: Data is attached to a Java object and can be accessed via this Java object globally.
* **[Session Storage](#session-storage)**: Data is stored for one specific session and can be accessed via the session ID globally.

## Baggage

As mentioned previously, data tags can be used across JVM borders when configured for `GLOBAL` propagation.
Then, the data can be read from incoming HTTP headers or can be written into outgoing HTTP headers.
InspectIT Ocelot will always read or write all globally propagated data via the `baggage` header.
The header contains a list of key-value pairs in the following format:

```
baggage: key1=value1,key2=value2
```

You can find more information about the header format [here](https://github.com/w3c/baggage/blob/main/baggage/HTTP_HEADER_FORMAT.md).

:::important
Please note that up to version `2.6.10` the header name was `correlation-context` instead of `baggage`. The header value format remained unchanged.
:::

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

### Restrictions

InspectIT Ocelot allows a maximum size of **4 KB** for the `baggage` header.
If incoming baggage exceeds this limit, inspectIT will not read the header at all.
If outgoing baggage exceeds this limit, all data tags exceeding the size limit will be dropped.
Thus, the outgoing baggage will be incomplete.

### Browser Security

Browser security measurements prevent JavaScript from reading custom headers such as `baggage` from cross-origin
requests. Thus, since version `2.6.12` the methods mentioned above will automatically add another 
`Access-Control-Expose-Headers` header for up-propagation. This additional header allows JavaScript 
to read the `baggage` header. Cross-origin requests between JVMs normally do not require this header.

```
access-control-expose-headers: baggage
```

## Session Storage

If data should be available across multiple requests to the JVM, inspectIT Ocelot allows you to cache such data
within a global session storage. After data has been written into the storage, every _inspectIT context_
assigned to the session has access to this data. To assign a session to a context,
you will have to write the session identification into the `remote_session_id` key via [actions](instrumentation/actions.md).
By default, this data key will be down propagated within the JVM.
**Only after** this specific data key has been set within the context, it can read from or write into the session storage.
Note, that every context can be assigned to only **one** session.

How the session ID is determined depends on your particular application and is not subject to any further restrictions.
It is also possible to read the session ID from HTTP headers, which will be explained in more detail below.

The following example shows how to read or write via the session storage. 
Reading data from the storage - for example for tracing - only works, if the [rule](instrumentation/rules.md) writing the tag 
has been executed before the reading one. Also note, that both contexts have to use the exact same session ID 
to access the storage. Otherwise, the contexts will use different storages with different data.

```yaml
inspectit:
  instrumentation:
    data:
      my-tag:
        # enable this tag to be stored for sessions
        session-storage: true 
        
    rules:
      r_write_session:
        # ...
        pre-entry:
          # assign the session id to the context
          remote_session_id: 
            action: a_read_my_session_id
        entry:
          my-tag:
            action: a_set_tag
        
      r_read_session:
        # ...
        pre-entry:
          # assign the session id to the context
          remote_session_id:
            action: a_read_my_session_id
        tracing:
          attributes:
            # use the tag in the session storage e.g. for tracing
            tag: my-tag

```

Under `inspectit.instrumentation.sessions`, there are some additional configuration options available.
All options can be updated at runtime.

| Config Property     | Default      | Description                                               |
|---------------------|--------------|-----------------------------------------------------------|
| `session-id-header` | `Session-Id` | HTTP-header, which will be used to determine the session. | 
| `session-limit`     | `100`        | The maximum limit of sessions stored at the same time.    | 
| `time-to-live`      | `5m`         | The duration for which data will be stored.               |

### Reading Session ID via HTTP

The session ID can also be passed to the _inspectIT context_ via HTTP headers. 
Within [actions](instrumentation/actions.md), you can call the method `readDownPropagationHeaders(Map<String,String> headers)`
via the `_context` [special parameter](instrumentation/actions.md#special-parameter) to read the session ID. 
The method will look for the configured `session-id-header` and will store its value in the `remote_session_id` 
data key. The inspectIT [default instrumentation](default-instrumentation/default-instrumentation.md) 
of the HTTP Servlet already reads the down propagated headers automatically to extract the session ID.

Alternatively, you can always include the session ID into the [baggage](#baggage) header by configuring the data key
`remote_session_id` for `GLOBAL` propagation. However, the context will always prioritize the value of 
the `session-id-header`, if it can be found. 

### Restrictions

To prevent excessive memory consumption, the maximum amount of active sessions can be configured via the `session-limit` option.
The session ID itself is restricted to a minimum of 16 characters and a maximum of 512 characters. Otherwise, the session
cannot be created. One session can store up to 128 data tags. Inside one session, data keys are restricted to a 
maximum of 128 characters and data value are restricted to a maximum of 2048 chars.
If a date entry does not comply with these size restrictions, the entry will not be stored.

The configuration option `time-to-live` determines, how long each tag should be stored after it's last update.
Every 30 seconds all expired data tags from each session storage will be cleaned up. If a session storage has been empty
for at least 60 seconds, the session storage will be removed completely.
