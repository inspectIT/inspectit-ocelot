---
id: open-telemetry-configuration
title: Using OpenTelemetry Library with inspectIT Ocelot
sidebar_label: OpenTelemetry Configuration
---

><mark> TODO: finish the configuration documentation when the migration to OTEL (with the OTEL bridge) is finished, i.e., when all exporters (including OTLP exporters) are supported
</mark>
If you plan to use the OpenTelemetry library in an application which will be instrumented later on with inspectIT Ocelot, some special rules do apply.
Following these rules will make sure that there are no run-time problems in your application.
Furthermore, a correct configuration will make it possible to combine metrics and traces that you manually collect using the OpenTelemetry instrumentation library with the ones collected by the inspectIT Ocelot agent.

1. Make sure you are using the same version of OpenTelemetry as inspectIT Ocelot.

   The inspectIT Ocelot agent in version {inspectit-ocelot-version} internally uses OpenTelemetry in version {opentelemetry-version}. Please adapt any OpenTelemetry dependency in your application to this version to avoid run-time conflicts.
   ```XML
   <dependency>
       <groupId>io.opentelemetry</groupId>
       <artifactId>opentelemetry-api</artifactId>
       <version>{opentelemetry-version}</version>
   </dependency>
   ```

2. Set the JVM property `inspectit.publishOpenTelemetryToBootstrap` to `true`.

   ```
   -Dinspectit.publishOpenTelemetryToBootstrap=true
   ```

   Setting the above property to `true` tells inspectIT Ocelot that you plan to use the OpenTelemetry library in combination with the agent. Note that this property must be specified with this exact name. The flexibility offered for all other config options does not apply here. The inspectIT Ocelot agent will make sure that all OpenTelemetry classes are then loaded by the bootstrap class loader. This ensures that OpenTelemetry implementation is shared between your manual instrumentation and the agent instrumentation, making the combination of data possible.

3. Add the agent to the start of a JVM

   In this scenario, it is required that you add the agent via [the `javaagent` JVM argument](getting-started/installation.md#adding-the-agent-to-a-jvm). If the agent is successfully added to the JVM, it will log that the OpenTelemetry classes pushed to the bootstrap classloader will be used.

   It is important to state that the agent will *not* publish the OpenTelemetry classes to the bootstrap classloader if it is attached during runtime – even if the previously mentioned JVM argument is set! In this case, metrics and traces of *manual OpenTelemetry instrumentations* will *not* be collected by the inspectIT Ocelot agent.