---
id: log-config
title: Server Logging Configuration
---

Similar to the Ocelot agent, the inspectIT Ocelot Configuration Server uses Logback as a library for logging as well.
Unlike the logging configuration of the agent, which can be configured using every option listed in
[Configuration Sources](configuration/configuration-sources.md),
the default logging configuration of the inspectIT Ocelot Configuration Server can *only* be manipulated through JVM system properties or environment variables.

The configuration server will log into the console and create three files:

* `full.log` - includes the complete log
* `access.log` - includes only agent request messages
* `audit.log` - includes only user interaction messages

The fille appenders use the rolling policy with a max file size of 20MB and a history of 30 files.

The following variables can be set to manipulate the default configuration:

|Variable | Description|
|---|---|
|`LOG_DIR`|Defines where the file output will be stored.|
|`LOG_CONSOLE_PATTERN`|Sets a custom logback pattern for the console output.|
|`LOG_FILE_PATTERN`|Sets a custom logback pattern for the file output.|

Additionally, you can change the default configuration by changing logback.xml of the configuration server before building. The file can be found in:
`components\inspectit-ocelot-configurationserver\src\main\resources\logback.xml`.