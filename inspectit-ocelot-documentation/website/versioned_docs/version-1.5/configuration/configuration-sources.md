---
id: version-1.5-configuration-sources
title: Configuration Sources
original_id: configuration-sources
---

inspectIT Ocelot tries to implement the zero-configuration approach but lets you externalize the configuration and influence every bit of its configuration if this is required.
Internally it uses the Spring-based `PropertySource` order to allow overriding of configuration values.
Configuration properties are considered in inspectIT Ocelot in the following order:

1. [Java Agent Arguments](#java-agent-arguments)
1. [Java System Properties](#java-system-properties)
1. [OS environment Variables](#os-environment-variables)
1. External Configuration Sources:
    * [File-based Configuration](configuration/external-configuration-sources.md#file-based-configuration)
1. inspectIT Ocelot Defaults

When an invalid configuration is given to inspectIT on startup, the agent will use a fallback configuration.
In this fallback configuration, the agent is inactive with the exception of listening for configuration updates.

When giving an invalid configuration through a runtime update to the agent, the agent will simply retain the previous configuration.

## Available Configuration Sources

### Java Agent Arguments

You can pass a JSON object as string to the agent via its command line arguments.
For example, to override the service name used to identify your application reporting the performance data,
you can change the `inspectit.service-name` property as follows:

```bash
$ java -javaagent:/path/to/inspectit-ocelot-agent-1.5.jar="{ \"inspectit\": { \"service-name\": \"My Custom Service\" }}" -jar my-java-program.jar
```

Note that you have to escape the quotes within your JSON string. On linux you can just use the more readable single quotes notation:

```bash
$ java -javaagent:/path/to/inspectit-ocelot-agent-1.5.jar='{ "inspectit": { "service-name": "My Custom Service" }}' -jar my-java-program.jar
```

### Java System Properties

You can pass any configuration property as the Java System property to the Java command that you use to start your Java application.
Using this approach you can change the `inspectit.service-name` property as follows:

```bash
$ java -Dinspectit.service-name="My Custom Service" -javaagent:/path/to/inspectit-ocelot-agent-1.5.jar -jar my-java-program.jar
```

### OS Environment Variables

Similar to the Java System properties, inspectIT Ocelot will also consider all the available operating system environment variables.
Due to the relaxed bindings, you can use upper case format, which is recommended when using system environment variables.

```bash
$ INSPECTIT_SERVICE_NAME="My Custom Service" java -javaagent:/path/to/inspectit-ocelot-agent-1.5.jar -jar my-java-program.jar
```

## Relaxed Bindings

Note that due to the Spring-powered configuration approach, the inspectIT Ocelot agent uses Spring support for relaxed bindings.
This means that a property can be specified in different formats depending on the property source.
As suggested by Spring, the allowed formats are:

| Property | Note |
| --- | --- |
| `inspectit.service-name` | Kebab-case, which is recommended for use in `.properties` and `.yml` files. |
| `inspectit.serviceName` | Standard camelCase syntax. |
| `inspectit.service_name` | Underscore notation (snake_case), which is an alternative format for use in `.properties` and `.yml` files. |
| `INSPECTIT_SERVICE_NAME` | UPPER_CASE format, which is recommended when using system environment variables. |

The formats should be used in the following way, based on the type of property source:

| Property Source | Format |
| --- | --- |
| System properties | Camel case, kebab case, or underscore notation. |
| Environment Variables | Upper case format with the underscore as the delimiter. |
| Property files (`.properties`) | Camel case, kebab case, or underscore notation. |
| YAML files (`.yaml`, `.yml`) | Camel case, kebab case, or underscore notation. |

## Environment Information

Each agent stores the following information about its runtime environment: 

| Property | Note |
| --- | --- |
| `inspectit.env.agent-dir` | Resolves to the path where the agent-jar is stored. |
| `inspectit.env.hostname` | The hostname where the agent is running. |
| `inspectit.env.pid` | The process id of the JVM process. |

They are used to define the default behavior when writing the configuration persistence file and will be sent
as attributes to the configuration server when fetching the configuration.

You can reference these properties within the configuration using e.g. `${inspectit.env.agent-dir}` 
as shown in the default configuration for 
[HTTP-based configuration](configuration/external-configuration-sources.md#http-based-configuration).
Referencing every other property follows the same schema.