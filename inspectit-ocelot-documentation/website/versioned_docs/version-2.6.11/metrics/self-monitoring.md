---
id: version-2.6.11-self-monitoring
title: Self-Monitoring
original_id: self-monitoring
---

inspectIT Ocelot is able to monitor itself and report the time spent in components responsible for recording metrics and creating traces.
This way a user can have a clear view on what is the overhead introduced by running the inspectIT Ocelot agent.
When self monitoring is enabled, the agent will expose several metrics regarding its internal state and processes.
For example, the `inspectit/self/duration` metric gives the total execution time spent in the agent in microseconds.
All metrics include the configured [common tags](metrics/common-tags.md).
The metric is split by the tag containing the component name and also includes all common tags.

| Metric Name                                     | Unit                  | Description                                                                                                                                                                                                                                                                                                                                              |
|-------------------------------------------------|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ```inspectit/self/duration```                   | us                    | The total time spent by inspectIT doing internal tasks, such as configuration loading, instrumenting, etc.The metric contains the tag ```component_name```, specifying in which component the time was spent                                                                                                                                             |
| ```inspectit/self/instrumentation-queue-size``` | `classes`             | InspectIT applies the configured instrumentation by working through a queue of classes it has to analyze and potentially instrument. This metric exposes the current size of this queue. By comparing it against the [total number of loaded classes](metrics/metric-recorders.md#class-loading-metrics), the instrumentation progress can be estimated. |
| ```inspectit/self/instrumented-classes```       | `classes`             | Exposes the total number of classes which are currently instrumented by inspectIT.                                                                                                                                                                                                                                                                       |
| ```inspectit/self/action/execution-time```      | us                    | The execution time of individual actions. The metric contains the tag `action_name`, specifying the name of the instrumented action.                                                                                                                                                                                                                     |
| ```inspectit/self/action/count```               | `action executions`   | The number of executions per action. The metric contains the tag `action_name`, specifying the name of the instrumented action.                                                                                                                                                                                                                          |
| ```inspectit/self/health```                     | health in `{0, 1, 2}` | The current health status, which can be `OK` (= 0), `WARNING` (= 1), or `ERROR` (= 2)                                                                                                                                                                                                                                                                    |

Self monitoring is enabled by default (except action metrics) and can be disabled by setting the `inspectit.self-monitoring.enabled` property to `false`.

> Not all components responsible for internal management of inspectIT Ocelot are at the moment reporting the time used for internal tasks. Please take the provided numbers only for a basic reference on overhead and don't assume they are 100% correct. In addition the overhead introduced in application classes through instrumentation is currently also not captured.

### Action Execution Monitoring

Since version `1.14.0`, the inspectIT Ocelot agent is able to record execution metrics of its actions.
In order to reduce the overhead the agent is producing on the target system, the action execution metrics recording of actions is disabled by default.

The recording of these metrics can be enabled using the following configuration:

```yaml
inspectit:
  self-monitoring:
    action-metrics:
      enabled: true
```

Note: the action execution metrics are only recorded in case the self-monitoring metrics are enabled. 

### Agent Health

Since version 1.16.0, the agent determines its health by observing its own log messages.
Whenever `WARN` messages occur, it changes the health to `WARNING`; when `ERROR` messages occur, to `ERROR`.

It takes into account that some log messages become outdated, e.g., a `WARN` message about an invalid configuration
becomes outdated when the configuration changes. Given there was one such `WARN` message, the health was `WARNING` and
jumps back to `OK` when the new, correct configuration is received.

Log messages that not become obsolete due to specific events time out after a defined period.
This period is controlled via the following configuration:

```yaml
inspectit:
  self-monitoring:
    agent-health:
      validity-period: 1h
```

Note: the health metric is only recorded in case the self-monitoring metrics are enabled.

> Besides the metric, the agent also reports its health via the `X-OCELOT-HEALTH` header when fetching HTTP configurations.
> This information may be used by configuration providers, such as the configuration server, to display the agent health.
