---
id: version-0.2-process
title: Instrumentation Process
original_id: process
---

The approach inspectIT Ocelot takes for instrumenting is fundamentally different from the approach of most other JVM instrumentation agents. InspectIT Ocelot does *not* instrument classes when they are loaded, the instrumentation is performed purely asynchronous in the background.

In this background task inspectIT Ocelot essentially looks at every loaded class and performs an instrumentation if required by the active configuration. Hereby, the agent manages the classes he has to analyze in a queue. This queue is processed in batches to ensure that no CPU resources are blocked if they are required by the instrumented application. The batching is configurable using the `internal` settings:

```yaml
inspectit:
  instrumentation:
    # settings for fine-tuning the instrumentation process
    internal:
      # the time to pause between executing batches of class instrumentation updates
      inter-batch-delay: 50ms
      # defines how many classes are checked at once for updates of their configuration per batch
      class-configuration-check-batch-size: 1000
      # defines the maximum number of classes which are instrumented per batch
      class-retransform-batch-size: 10

      # defines how often the agent should check if new classes have been defined.
      new-class-discovery-interval: 10s
      # defines how often the new class discovery is performed after a new class has been loaded
      num-class-discovery-trials: 2
```

In addition, the size of the instrumentation queue can be used as an indicator for the instrumentation progress.
It is accessible via the [self-monitoring](metrics/self-monitoring.md) of the agent.