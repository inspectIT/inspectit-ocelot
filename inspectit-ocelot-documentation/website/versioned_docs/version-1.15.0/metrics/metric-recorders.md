---
id: version-1.15.0-metric-recorders
title: Metrics Recorders
original_id: metric-recorders
---

Metrics recorders are responsible for capturing system metrics, such as processor or memory usage. All metrics recorders provided by inspectIT Ocelot publish the recorded data through the OpenCensus API.
Therefore all recorded metrics can be exported via any of the [supported metrics exporters](metrics/metric-exporters).
Currently the inspectIT Ocelot agent is capable of recording the following metrics:

* [CPU](#cpu-metrics) (usage and number of cores)
* [Disk Space](#disk-space-metrics) (used and total)
* [Memory](#memory-metrics) (used and available for various regions)
* [Threads](#thread-metrics) (counts and states)
* [Garbage Collection](#garbage-collection-metrics) (Pause times and collection statistics)
* [Class Loading](#class-loading-metrics) (loaded and unloaded counts)
* [JMX](#jmx-metrics) (all exposed JMX targets)


:::note
The metrics above and their capturing logic are based on the open-source [micrometer](https://micrometer.io/) project.
:::

In the following sections we provide detailed information on the collected metrics and how they can be configured.
In general metrics are grouped together based on the recorder which provides them.

Many recorders poll the system APIs for extracting the metrics. The rate at which this polling occurs can be configured.
By default all polling based recorders use the duration specified by `inspectit.metrics.frequency`. The default value
of this property is `15s`. Overwriting `inspectit.metrics.frequency` will cause all recorders to use the given
frequency in case they do not have an explicit frequency in their own configuration.

:::tip Default metrics settings
 By default, all metrics are captured if they are available on the system. If you do not want certain metrics to be recorded, you need to disable them manually. For example, if you want to disable the `system.average` metric of the `processor` recorder, you need to use the following configuration:
```YAML
inspectit:
  metrics:
    processor:
      enabled:
        system.average: false
```
:::

## CPU Metrics

Processor metrics are recorded by the `inspectit.metrics.processor` recorder.
This recorder polls the captured data from the system with a frequency specified by `inspectit.metrics.processor.frequency` which defaults to `inspectit.metrics.frequency`.
The available metrics are explained in the table below.

|Metric|Description|Unit|OpenCensus Metric Name|
|---|---|---|---|
|`count`|The number of processor cores available to the JVM|cores|`system/cpu/count` |
|`system.average`|The sum of the number of runnable entities queued to the available processors and the number of runnable entities running on the available processors averaged over a minute for the whole system. See the definition of [getSystemAverageLoad()](https://docs.oracle.com/javase/7/docs/api/java/lang/management/OperatingSystemMXBean.html#getSystemLoadAverage()) for more details.|percentage|`system/load/average/1m`
|`system.usage`|The recent CPU usage for the whole system|percentage|`system/cpu/usage`
|`process.usage`|The recent CPU usage for the JVM's process|percentage|`process/cpu/usage`

:::note
The availability of each processor metric depends on the capabilities of your JVM in combination with your OS. If a metric is not available, the inspectit Ocelot agent will print a corresponding info in its logs on startup.
:::

## Disk Space Metrics

Disk space metrics are recorded by the `inspectit.metrics.disk` recorder.
This recorder polls the captured data from the system with a frequency specified by `inspectit.metrics.disk.frequency` which defaults to `inspectit.metrics.frequency`.
The available metrics are explained in the table below.

|Metric|Description|Unit|OpenCensus Metric Name
|---|---|---|---|
|`free`|The free disk space|bytes|`disk/free`
|`total`|The total size of the disk|bytes|`disk/total`

## Memory Metrics

All memory related metrics are recorded by the `inspectit.metrics.memory` recorder.
This recorder polls the captured data from the system with a frequency specified by `inspectit.metrics.memory.frequency` which defaults to `inspectit.metrics.frequency`.

The first set of available metrics are general JVM memory metrics:

|Metric|Description|Unit|OpenCensus Metric Name|
|---|---|---|---|
|`used`|The amount of used memory|bytes|`jvm/memory/used`|
|`committed`|The amount of memory that is committed for the Java virtual machine to use|bytes|`jvm/memory/committed`|
|`max`|The maximum amount of memory in bytes that can be used for memory management|bytes|`jvm/memory/max`|

For all these metrics inspectIT adds two tags in addition to the [common tags](metrics/common-tags.md): Firstly `area` which either is `heap` or `non-heap`.
Secondly an `id` tag is added specifying the exact memory region, for example `PS Old Gen` depending on the used garbage collector.

Most JVMs also provide metrics regarding the usage of [buffer](https://docs.oracle.com/javase/8/docs/api/java/nio/ByteBuffer.html) pools, which are reflected in the following metrics provided by inspectIT Ocelot:

|Metric|Description|Unit|OpenCensus Metric Name
|---|---|---|---|
|`buffer.count`|An estimate of the number of buffers for each buffer pool|buffers|`jvm/buffer/count`
|`buffer.used`|An estimate of the memory that the JVM is currently using for each buffer pool|bytes|`jvm/buffer/memory/used`
|`buffer.capacity`| An estimate of the total capacity of the buffers in each pool|bytes|`jvm/buffer/total/capacity`

Again for each metric an `id` tag is added. This tag hereby contains the name of the buffer pool for which the metrics was captured.

## Thread Metrics

Thread metrics provide statistics about the number and the state of all JVM threads.
They are recorded by the `inspectit.metrics.threads` recorder.
This recorder polls the captured data from the JVM with a frequency specified by `inspectit.metrics.threads.frequency` which defaults to `inspectit.metrics.frequency`.
The available thread metrics are explained in the table below.

|Metric|Description|Unit|OpenCensus Metric Name
|---|---|---|---|
|`peak`|The peak number of live threads since the start of the JVM|threads|`jvm/threads/peak`
|`live`|The total number of currently live threads including both daemon and non-daemon threads|threads|`jvm/threads/live`
|`daemon`|The total number of currently live daemon threads|threads|`jvm/threads/daemon`
|`states`|The total number of currently live threads for each state|threads|`jvm/threads/states`

The `states` metric provides the amount of threads grouped by their state.
For this purpose, an additional tag `state` is added whose values correspond to the Java [Thread.State enum](https://docs.oracle.com/javase/7/docs/api/java/lang/Thread.State.html).

## Garbage Collection Metrics

The `inspectit.metrics.gc` recorder provides metrics about the time spent for garbage collection as well as about the collection effectiveness.
This recorder is not polling based. Instead, it listens to garbage collection events published by the JVM and records metrics on occurrence.

:::note
The availability of all garbage collection metrics depends on the capabilities of your JVM. If the garbage collection metrics are unavailable, the inspectit Ocelot agent will print a corresponding info in its logs on startup.
:::

The recorder offers the following timing related metrics:

|Metric|Description|Unit|OpenCensus Metric Name
|---|---|---|---|
|`pause`|The total time spent for Garbage Collection Pauses|milliseconds|`jvm/gc/pause`
|`concurrent.phase.time`|The total time spent in concurrent phases of the Garbage Collector|milliseconds|`jvm/gc/concurrent/phase/time`

Whether `pause` or `concurrent.phase.time` are captured depends on the concurrency of the garbage collector with which the JVM was started.
For both metrics an `action` and a `cause` tag is added. The `action` specifies what was was done, e.g. a minor or a major collection.
The `cause` tag provides information on the circumstances which triggered the collection.

The following additional garbage collection metrics are also available:

|Metric|Description|Unit|OpenCensus Metric Name
|---|---|---|---|
|`live.data.size`|The size of the old generation memory pool captured directly after a full GC.|bytes|`jvm/gc/live/data/size`
|`max.data.size`|The maximum allowed size of the old generation memory pool captured directly after a full GC.|bytes|`jvm/gc/max/data/size`
|`memory.allocated`|Increase in the size of the young generation memory pool after one GC to before the next|bytes|`jvm/gc/memory/allocation`
|`memory.promoted`|Increase in the size of the old generation memory pool from before a GC to after the GC|bytes|`jvm/gc/memory/allocation`

## Class Loading Metrics

Class loading metrics are recorded by the `inspectit.metrics.classloader` recorder.
This recorder polls the captured data from the system with a frequency specified by `inspectit.metrics.classloader.frequency` which defaults to `inspectit.metrics.frequency`.
The available metrics are explained in the table below.

|Metric|Description|Unit|OpenCensus Metric Name
|---|---|---|---|
|`loaded`|The total number of currently loaded classes in the JVM|classes|`jvm/classes/loaded`
|`unloaded`|The total number of unloaded classes since the start of the JVM|classes|`jvm/classes/unloaded`

## JMX Metrics

Metrics exposed by MBean objects are recorded by the `inspectit.metrics.jmx` recorder which can be enabled by setting the `inspectit.metrics.jmx.enabled` property to `true`.

This recorder polls all registered MBean servers with a frequency specified by `inspectit.metrics.jmx.frequency` which defaults to `inspectit.metrics.frequency`.
The recorder exposes JMX attributes containing values that are non-negative numbers or booleans.
All values are exposed as double metric representing the last value of the JMX MBean.
Booleans are converted to `0.0` or `1.0` and non-double numbers to double representations.

Format of the metric name that's being exposed follows the pattern:
```text
jvm/jmx/domain/bean_property_1_value/attrbute_key_1/../attribute_key_N/attribute_name
```

**Example Metric Name**: `jvm/jmx/java/lang/runtime/uptime`

The exposed metrics also contain additional bean properties as labels.
All values that are used in constructing the metric's name are lowercase by default, but this can be adapted in the settings.

By default, the JMX metrics recorder will force the creation of the default platform MBean server when enabled.
You can change this behavior by setting the value of the property `inspectit.metrics.jmx.force-platform-server` to `false`. 

You can precisely control which MBean should be scraped and exposed as metrics using the `inspectit.metrics.jmx.object-names` property.
This property defines a map of object names to be whitelisted or blacklisted.
In this map the key represents an object name pattern, while the value is a boolean representing whether a MBean is white- (`true`) or blacklisted (`false`).
The behavior is as follows:
1. If the map is empty everything is collected.
2. If the map contains only whitelisted object name entries, only these are collected.
3. If the map contains only blacklisted object name entries, then everything is collected except the blacklisted ones.
4. If the map contains both the whitelisted and blacklisted entries, then only the whitelisted ones that are not blacklisted are collected.

All configuration properties related to the JMX recorder are located under the `inspectit.metrics.jmx` property.
The available JMX configuration properties are:

<!--- spaces to force a wider property column to prevent a lot of line breaks in the long property names -->

|Property&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|Description|Default
|---|---|---|
|`enabled`|The switch for enabling/disabling JMX recorder.|`false`
|`frequency`|Specifies the frequency used by the JMX recorder to poll and record metrics.|`${inspectit.metrics.frequency}`
|`force-platform-server`|The switch to enable or disable the creation of the platform MBean server before scraping starts.|`true`
|`lower-case-metric-name`|If `true` records JMX metrics with the lowercase name format.|`true`
|`object-names`|Map for whitelisting and blacklisting object names to be scraped. The key should be an object name pattern and value should be `true` (whitelisting) or `false` (blacklisting). More info about the object name patterns can be found in [Java SE API docs](https://docs.oracle.com/javase/7/docs/api/javax/management/ObjectName.html). |see above