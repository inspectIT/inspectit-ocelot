---
id: version-1.4-install-eum-agent
title: Installing the EUM Agent
sidebar_label: Install EUM Agent
original_id: install-eum-agent
---

The Ocelot EUM server offers a backend for Javascript monitoring with [Boomerang](https://developer.akamai.com/tools/boomerang/docs/index.html).
Boomerang is a Javascript metrics agent, which is able to capture arbitrary customizable metrics. 

In order to use Boomerang, you can inject the following snipped in your webpage.
In this example, it will use the Boomerang version which is bundled in the Ocelot EUM server.
This snippet loads the boomerang agent and sends all measured metrics to the specified beacon URL, in this case it is the Ocelot EUM server:

```javascript
 <script src="http://[EUM-SERVER-HOST]:[EUM-SERVER-PORT]/boomerang/boomerang.js"></script>
 <script src="http://[EUM-SERVER-HOST]:[EUM-SERVER-PORT]/boomerang/plugins/rt.js"></script>
 <!-- any other plugins you want to include -->
 <script>
   BOOMR.init({
     beacon_url: "http://[EUM-SERVER-HOST]:[EUM-SERVER-PORT]/beacon/"
   });
 </script>
```

All boomerang-specific scripts are provided by the EUM server under the following URL `http://[EUM-SERVER-HOST]:[EUM-SERVER-PORT]/boomerang/**`.

You can access the boomerang main script under: `http://[EUM-SERVER-HOST]:[EUM-SERVER-PORT]/boomerang/boomerang.js`.
All boomerang plugins can be downloaded under `http://[EUM-SERVER-HOST]:[EUM-SERVER-PORT]/boomerang/plugins/[PLUGIN_NAME].js`.
The list of available plugins can be found [here](http://akamai.github.io/boomerang/BOOMR.plugins.html).

:::warning
Note that this approach is not recommended for production use as the scripts are not minified and are loaded synchronously.
:::

Boomerang recommends that you use a [minified JS containing all your plugins](https://developer.akamai.com/tools/boomerang/docs/tutorial-building.html#asynchronously). 
In addition you should use asynchronous injection as described in the [Boomerang documentation](https://developer.akamai.com/tools/boomerang/docs/index.html).

:::tip
For automatic collection of the EUM traces have a look on the [Boomerang OpenTelemetry plugin](https://github.com/NovatecConsulting/boomerang-opentelemetry-plugin).
:::