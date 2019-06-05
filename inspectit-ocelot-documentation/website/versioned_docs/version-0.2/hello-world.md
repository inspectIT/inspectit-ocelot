---
id: version-0.2-doc1
title: Hello World
original_id: doc1
---

inspectIT Ocelot is a spin-off from the [inspectIT](https://github.com/inspectIT/inspectIT) APM project.

Open source APM tools have significantly improved in the past few years and instrumentation libraries like OpenCensus, openTracing, Zipkin, Jaeger, micrometer, etc have gained a lot of popularity.
The main problem that inspectIT Ocelot tries to solve is the time needed to introduce these instrumentation libraries in already active projects.
As these libraries require manual instrumentation, this would result in code changes in hundreds of projects for many organizations.
This often hinders companies from moving away from commercial APM solutions.

The inspectIT Ocelot agent uses Java byte-code manipulation to set up the OpenCensus instrumentation library with zero-configuration and requires no source-code changes.
Furthermore, inspectIT Ocelot instruments your application in a way that most important metrics are automatically monitored and traces are collected and propagated as well.
This allows DevOps teams to start collecting performance data about their Java applications in seconds.

The decision to base the agent on the OpenCensus implementation was made because of the flexibility that OpenCensus provides with respect to where the collected data can be exported and stored.
OpenCensus offers multiple [exporters for Java](https://opencensus.io/exporters) (Prometheus, Zipkin, Jaeger, etc) and inspectIT Ocelot supports them all.
Thus, combining different open source tools you can come to the desired APM solution based on open source software only, as an example [OpenAPM.io](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&collector=jaeger-collector,zipkin-server,prometheus-server&visualization=jaeger-query,zipkin-server&dashboarding=grafana) landscape shows below:

![Possible landscape with inspectIT Ocelot and other open source tools](assets/inspectit-ocelot-landscape.png)

It's important to mention that inspectIT Ocelot seamlessly integrates with the OpenCensus library if it is used in your application.
The data collected by your manual source code instrumentation will be combined with everything that the agent collects as well.