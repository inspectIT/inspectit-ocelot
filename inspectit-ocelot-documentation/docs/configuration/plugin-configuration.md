---
id: plugin-configuration
title: Plugin Configuration
---

There are more OpenCensus exporters available than the ones we have included with inspectIT Ocelot.
This decision was made so that we can keep the size and amount of dependencies of the agent small. 
For this reason, we introduced a plugin system allowing you to add and configure additional exporters.

For details on existing plugins and to find out how to add your own exporter as a plugin 
take a look at [this repository](https://github.com/inspectIT/inspectit-ocelot-plugins).

Plugins in general are JAR files which are loaded by the inspectIT Ocelot agent.
In order to enable the plugin system, you need to tell the agent where it should look for plugin JARs:

```yaml
inspectit:
  plugins:
    path: '/path/to/directory'
```

The directory will be scanned on startup of the agent and all found plugins will be loaded.

> The scanning for plugins only takes place on agent startup. Adding plugins at runtime is currently not supported.

For configuring the loaded plugins, the same [configuration sources](#configuration-sources) are used as for the agent's configuration.
The settings for each plugin can be defined under the plugins section based on its name:


```yaml
inspectit:
  plugins:
    path: '/path/to/directory'
    
    some-plugin:
      some-config-option: ...
      
    some-other-plugin:
      something: ...
```