---
id: install-eum-agent
title: Installing the EUM Agent
sidebar_label: Install EUM Agent
---

# Metrics

The inspectIT Ocelot EUM-server offers a backend for JavaScript monitoring with [Boomerang](https://akamai.github.io/boomerang/akamai/).
Boomerang is a JavaScript metrics agent, which is able to capture arbitrary customizable metrics.

In order to use Boomerang, you can inject the following snippet in your webpage.
In this example, it will use the Boomerang version which is bundled in the Ocelot EUM server.
This snippet loads the Boomerang agent and sends all measured metrics to the specified beacon URL, 
in this case it is the EUM server:

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
Boomerang recommends to use a [minified JS containing all your plugins](https://akamai.github.io/boomerang/akamai/#asynchronously).
Additionally, you should use asynchronous injection as described in the [Boomerang documentation](https://akamai.github.io/boomerang/akamai/#toc8__anchor).
:::


# Traces

Boomerang itself can only collect metrics. However, it is possible to also collect traces automatically
by using the [OpenTelemetry-Boomerang-Plugin](https://github.com/inspectIT/boomerang-opentelemetry-plugin).
This self-made plugin allows to record traces via Boomerang and send them to the EUM-server as well.
You can include the plugin as any other Boomerang plugin. 

The EUM server provides the plugin under the URL `http://[EUM-SERVER-HOST]:[EUM-SERVER-PORT]/boomerang/plugins/boomerang-opentelemetry.js`.

