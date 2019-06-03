---
id: version-0.2-instrumentation
title: Instrumentation
original_id: instrumentation
---
This section describes how the inspectIT Ocelot Java agent can be used to inject predefined or custom monitoring code into the target application.

InspectIT Ocelot gives you a very large degree of freedom when it comes to defining instrumentation.
The first step is usually defining your [scopes](instrumentation/scopes.md). A scope acts as a selector for finding the methods you want to instrument.

Scopes are then used by [rules](instrumentation/rules.md). While scopes define which methods you instrument, rules define the actual monitoring actions which will be performed. Examples for such actions would be recording the response time or extracting the HTTP url for further processing.

For the definition of rules, [actions](instrumentation/rules.md#actions) are a key concept.
Long story short: actions allow you to specify _Java snippets in your configuration_ which will be executed to extract any data you want. This can be performance data such as the response time or any kind of business data, e.g. the shopping cart size.

> All instrumentation settings can be changed without restarting the application! They can even be changed while a previous instrumentation is still in progress. In this case the inspectIT Ocelot agent will automatically switch to the new instrumentation as soon as the configuration is loaded.