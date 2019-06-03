---
id: version-0.2-open-census-configuration
title: Using OpenCensus Library with inspectIT Ocelot
sidebar_label: OpenCensus Configuration
original_id: open-census-configuration
---

If you plan to use the OpenCensus library in an application which will be instrumented later on with inspectIT Ocelot, some special rules do apply.
Following these rules will make sure that there are no run-time problems in your application.
Furthermore, a correct configuration will make it possible to combine metrics and traces that you manually collect using the OpenCensus instrumentation library with the ones collected by the inspectIT Ocelot agent.

1. Make sure you are using the same version of OpenCensus as inspectIT Ocelot.
   
   The inspectIT Ocelot agent in version 0.2 internally uses OpenCensus in version 0.20.0. Please adapt any OpenCensus dependency in your application to this version to avoid run-time conflicts.
   ```XML
   <dependency>
       <groupId>io.opencensus</groupId>
       <artifactId>opencensus-api</artifactId>
       <version>0.20.0</version>
   </dependency>
   ```

2. Set the JVM property `inspectit.publishOpenCensusToBootstrap` to `true`.

   ```
   -Dinspectit.publishOpenCensusToBootstrap=true
   ```

   Setting the above property to `true` tells inspectIT Ocelot that you plan to use the OpenCensus library in combination with the agent. Note that this property must be specified with this exact name. The flexibility offered for all other config options does not apply here. The inspectIT Ocelot agent will make sure that all OpenCensus classes are then loaded by the bootstrap class loader. This ensures that OpenCensus implementation is shared between your manual instrumentation and the agent instrumentation, making the combing of data possible.

3. Add the agent to the start of a JVM

   In this scenario, it is required that you add the agent via [the `javaagent` JVM argument](getting-started/installation.md#adding-the-agent-to-a-jvm). If the agent is successfully added to the JVM, it will log that the OpenCensus classes pushed to the bootstrap classloader will be used.

   It is important to state that the agent will *not* publish the OpenCensus classes to the bootstrap classloader if it is attached during runtime – even if the previously mentioned JVM argument is set! In this case, metrics and traces of *manual OpenCensus instrumentations* will *not* be collected by the inspectIT Ocelot agent.