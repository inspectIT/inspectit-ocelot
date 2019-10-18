---
id: external-configuration-sources
title: External Configuration Sources
sidebar_label: External Configuration Sources
---

In order to support the externalized configuration inspectIT Ocelot provides several methods.
These methods represent a bridge between certain configuration providers and the inspectIT Ocelot agent.
This way, a configuration can be provided from different sources, for example, file systems, the internet, external services, configuration services, vaults, etc.

The configuration method is considered dynamic if it supports run-time updates.
For example, if the configuration file changes on disk, the file-based configuration method will report the updated properties to the inspectIT Ocelot agent.
The agent will then update the internal configuration, effectively changing the behavior of the agent during the run-time.

> Note that not all inspectIT Ocelot configuration properties support dynamic updates. In addition, the Java System properties and OS environment variables always have higher priority, thus dynamically changing a property which is also defined in those sources will have no effect.

All externalized configuration methods must be enabled using Java System properties or OS environment variables, as they are disabled by default.
You can have more than one method active at the same time, thus pulling the configuration from different sources.

| Method | Dynamic updates | Enabled by default |
| --- | --- | --- |
| [File-based Configuration](configuration/external-configuration-sources.md#file-based-configuration) | Yes | No |
| [HTTP-based Configuration](configuration/external-configuration-sources.md#http-based-configuration) | Yes | No |

## File-based Configuration

The file-based configuration loads all `.yaml`/`.yml`, `.json`  and `.properties` files in alphabetical order from the given directory path in non-recursive mode.
The alphabetical order of files defines their priority, thus if the same property exists in `my.properties` and `yours.properties` files, then the one from the `my.properties` file will be used.

The file-based configuration is activated by specifying the path to an existing directory in the `inspectit.config.file-based.path` property.
It automatically watches for changes in the given directory and reports updates to inspectIT Ocelot agent.
If you would like to disable the dynamic updates you can set the `inspectit.config.file-based.watch` property to `false`.
In addition it is possible to configure the frequency at which the agent checks the directory for changes.
This is done by specifying the duration `inspectit.config.file-based.frequency`.

| Property | Default | Description |
| --- | --- | --- |
|`inspectit.config.file-based.path`|-|The path to the directory to scan for configuration files. An existing directory must be specified for the file-based configuration to be enabled.|
|`inspectit.config.file-based.enabled`|`true`|Can be used to disable the file-based configuration while the path is still specified.|
|`inspectit.config.file-based.watch`|`true`|If `true` the directory will be watched for changes. When changes occur, the configuration is dynamically updated.|
|`inspectit.config.file-based.frequency`|`5s`|Specifies the frequency at which the configuration directory will be checked for changes. When setting the frequency to zero, no polling is performed. Instead the agent listens for filesystem events published by the operating system to detect changes.|

> You must make sure that the access to your configuration directory is restricted in the same way you would restrict access to your application code or startup script. Otherwise the concept of [actions](instrumentation/rules#actions) of inspectIT Ocelot could make your application prone to code injection!

## HTTP-based Configuration

The HTTP-based configuration periodically fetches a specified HTTP endpoint in order to receive the agent configuration.
The HTTP endpoint is expected to either provide a JSON or YAML file.
This configuration source can be used when using the [inspectIT Ocelot Configuration Server](config-server/overview.md).

| Property | Default | Description |
| --- | --- | --- |
|`inspectit.config.http.url`|-| The url of the http endpoint to query the configuration.|
|`inspectit.config.http.enabled`|`true`| Whether the http property source should be used.|
|`inspectit.config.http.frequency`|`30s`| The frequency of polling the http endpoint to check for configuration changes. |
|`inspectit.config.http.attributes`|`service: ${inspectit.service-name}`| The following attributes will be sent as http query parameters when fetching the configuration. These are used to map agents to certain configurations. See the section on [Agent Mappings](config-server/agent-mappings.md). |
|`inspectit.config.http.persistence-file`|`${inspectit.env.jar-dir}/${inspectit.service-name}/last-http-config.yml`| The agent while save the last fetched configuration in this file. |

> Due to security reasons, the HTTP-based configuration has the lowest priority, thus, cannot override configuration properties set by different configuration sources.

To use the [inspectIT Ocelot Configuration Server](config-server/overview.md), you can simply set the `url` property to `http://{ocelot-config-server-host:port}/api/v1/agent/configuration`

The Ocelot agent will poll the given HTTP URL with the given frequency and reload the configuration if required.
This polling uses HTTP ETags and last-modified headers to ensure that the configuration is only refetched in case it actually changed.
If the HTTP request does not succeed, the last successfully fetched configuration will be kept loaded.

Every time the configuration is successfully fetched, it is also persisted in the file specified via `persistence-file`.
By default, the agent will create a folder next to the agent's JAR with the name of the service and create a file within this folder.
This file is used in case of a JVM restart: If the initial HTTP request fails, the agent will attempt to load the contents of this file as HTTP configuration.
This mechanism ensures that the last loaded configuration is kept in case the HTTP provider is unavailable, even in case of a restart.

You can disable the persisting of the configuration by setting the `persistence-file` property to an empty string or `null`.
