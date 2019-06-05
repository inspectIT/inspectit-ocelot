---
id: version-0.2-logging-configuration
title: Logging Configuration
original_id: logging-configuration
---

The inspectIT Ocelot agent uses Logback library for logging.
It comes with the default logging configuration that can be manipulated using configuration properties.
The default logback configuration appends inspectIT logs to the console and writes two types of log files:

* `agent.log` - includes the complete log
* `exceptions.log` - includes only `WARN` and `ERROR` level messages

The file appenders use the rolling policy with a max file size of 20MB and a history of 30 files.

The available properties for manipulating the default logging configuration are the following:

|Property |Default| Description|
|---|---|---|
|`inspectit.logging.trace`|`false`|Sets the inspectIT Ocelot log level to `TRACE`.|
|`inspectit.logging.debug`|`false`|Sets the inspectIT Ocelot log level to `DEBUG` only if it's not already set to `TRACE`.|
|`inspectit.logging.console.enabled`|`true`|Defines if the console output is enabled.|
|`inspectit.logging.console.pattern`|-|Sets a custom logback pattern for console output.|
|`inspectit.logging.file.enabled`|`true`|Defines if the file appenders are enabled.|
|`inspectit.logging.file.pattern`|-|Sets a custom logback pattern for file output.|
|`inspectit.logging.file.path`|-|Sets a path where `agent.log` and `exceptions.log` files are created. By default, the path is resolved to `$java.io.tmp/inspectit-ocelot` directory, falling back to `/tmp/inspectit-ocelot` if `$java.io.tmp` System property is not defined.|
|`inspectit.logging.file.include-service-name`|`true`|When `true` the service name defined in the `inspectit.service-name` is included in the log messages appended to files.|

If you are not satisfied with default logback configuration options you can supply your own logback config file in the property `inspectit.logging.config-file`.
This way the properties specified in the table above are not taken into account.