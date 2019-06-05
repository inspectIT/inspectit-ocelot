---
id: version-0.2-self-monitoring
title: Self-Monitoring
original_id: self-monitoring
---

inspectIT Ocelot is able to monitor itself and report the time spent in components responsible for recording metrics and creating traces.
This way a user can have a clear view on what is the overhead introduced by running the inspectIT Ocelot agent.
When self monitoring is enabled, the agent will expose several metrics regarding its internal state and processes.
For example, the `inspectit/self/duration` metric gives the total execution time spent in the agent in microseconds.
All metrics include the configured [common tags](metrics/common-tags.md).
The metric is split by the tag containing the component name and also includes all common tags.

|Metric Name |Unit| Description
|---|---|---|
|```inspectit/self/duration```|us|The total time spent by inspectIT doing internal tasks, such as configuration loading, instrumenting, etc.The metric contains the tag ```component_name```, specifying in which component the time was spent
|```inspectit/self/instrumentation-queue-size```|`classes`|InspectIT applies the configured instrumentation by working through a queue of classes it has to analyze and potentially instrument. This metric exposes the current size of this queue. By comparing it against the [total number of loaded classes](metrics/metric-recorders.md#class-loading-metrics), the instrumentation progress can be estimated.
|```inspectit/self/instrumented-classes```|`classes`|Exposes the total number of classes which are currently instrumented by inspectIT.

Self monitoring is enabled by default and can be disabled by setting the `inspectit.self-monitoring.enabled` property to `false`.

> Not all components responsible for internal management of inspectIT Ocelot are at the moment reporting the time used for internal tasks. Please take the provided numbers only for a basic reference on overhead and don't assume they are 100% correct. In addition the overhead introduced in application classes through instrumentation is currently also not captured.
