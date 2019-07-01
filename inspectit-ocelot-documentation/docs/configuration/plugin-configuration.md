---
id: plugin-configuration
title: Plugin Configuration
---

There are way more OpenCensus exporters available than we can include with the inspectIT Ocelot Agent.
For this reason, we introduced a plugin system allowing you to add and configure additional exporters.

For details on existing plugins as well as to find out how to add your own exporter as a plugin 
checkout [this repository](https://github.com/inspectIT/inspectit-ocelot-plugins).

Plugins in general are jar files which are loaded by the inspectIT Ocelot agent.
In order to enable the plugin system, you need to tell the agent where it should look for plugin jars:

```yaml
inspectit:
  plugins:
    path: /path/to/directory
```

The directory will be scanned on startup of the agent and all found plugins will be loaded.

For configuring the loaded plugins, the same [configuration sources](#configuration-sources) are used as for the agent's configuration.
The settings for each plugin can be defined under the plugins section based on its name:


```yaml
inspectit:
  plugins:
    path: /path/to/directory
    
    some-plugin:
      some-config-option: ...
      
    some-other-plugin:
      soemthing: ...
```