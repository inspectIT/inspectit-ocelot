---
id: version-2.6.1-process
title: Instrumentation Process
original_id: process
---

The approach inspectIT Ocelot takes for instrumenting is fundamentally different from the approach of most other JVM instrumentation agents.
InspectIT Ocelot does *not* instrument classes when they are loaded, the instrumentation is performed purely asynchronous in the background.

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
      
      # defines whether orphan action classes are recycled or new classes should be injected instead
      recyclingOldActionClasses: true
```

In addition, the size of the instrumentation queue can be used as an indicator for the instrumentation progress.
It is accessible via the [self-monitoring](metrics/self-monitoring.md) of the agent.

InspectIT allows you to perform instrumentation by injecting custom code into your application.
If your JVM has a `SecurityManager` enabled, you might also want to control the `ProtectionDomain` of these injected classes.

By default, inspectIT will use its own `ProtectionDomain` for injected classes.
Alternatively, you can make inspectIT to use the `ProtectionDomain` for which the action is being created using the following configuration:

```yaml
inspectit:
  instrumentation:
    internal:
      use-inspectit-protection-domain: false
```

## Synchronous instrumentation (BETA!)
:::caution
Enabling synchronous instrumentation in Java 8 environments will result in significant boot time performance degradation!
See See: <a href="https://bugs.openjdk.java.net/browse/JDK-7018422">JDK-7018422</a>
:::

By default, all instrumentation is performed purely asynchronously in the background. There may be situations where this is not appropriate and a class must be instrumented directly at the first load, 
e.g. in batch processes.

InspectIT can be configured to instrumented classes on first class load by updating the following configuration:
```yaml
inspectit:
  instrumentation:
    internal:
      async: false
```

## Delayed instrumentation
Despite instrumenting asynchronously or synchronously, inspectIT always starts the instrumentation process as soon as
the agent is attached to a JVM. There are cases in which it is desirable to postpone the start of the instrumentation 
process. Although this is rarely necessary inspectIT provides the possibility to do so via system property
`inspectit.start.delay` or OS environment variable `INSPECTIT_START_DELAY`. 

You provide a value interpreted as milliseconds the agent shall wait before the instrumentation process starts. If you 
do not provide a value the instrumentation process will start immediately.

The Agent expects positive integers excluding zero. For all other values the agent will print an error message on stderr
and continue as if there was no value supplied.

If you specify both system property and OS environment variable, the system property will take precedence.

Since this option has an impact before the agent fetches any configuration from the
[Configuration Server](config-server/overview.md) you cannot specify that value like any other inspectIT configuration
property. It is only available as system property or OS environment variable.

Example using system property:
```bash
# this will delay the instrumentation process by 10 minutes
$ java -javaagent:"/path/to/inspectit-ocelot-agent-2.6.1.jar" \
   -Dinspectit.start.delay=600000 \
   -jar my-java-program.jar
```

Example using OS environment variable (using bash):
```bash
# this will delay the instrumentation process by 5 minutes
$ export INSPECTIT_START_DELAY=300000
$ java -javaagent:"/path/to/inspectit-ocelot-agent-2.6.1.jar" -jar my-java-program.jar
```
