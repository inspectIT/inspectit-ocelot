---
id: agent-command-configuration
title: Agent Command Configuration
sidebar_label: Agent Command Configuration
---

Since version `1.10.0`, the inspectIT Ocelot agent provides the _Agent Commands_ feature, which allows to interact with a specific agent.
Using this feature is only possible if the agent is used together with the inspectIT Ocelot Configuration-Server.

With this feature it is possible to send specific commands or operations to the agent via the configuration server, which the agent should execute.
Subsequently, the result of these commands is returned.

It is now possible to interact with the agent.
Simple commands can be executed, such as a "ping" command whether an agent exists or more complex commands, such as loading the class structure of the application in which the agent is integrated.
Some new functionality of the configuration server interface is based on this feature, e.g. the _class browser_.
These functions work only if this feature is enabled.

:::warning
It is important to note that depending on the complexity of the agent commands, they may have a negative impact on the performance of the target application.
:::

## How it works

It is important to note that the mentioned agent commands **cannot** be sent directly to the agents.
For security reasons, **all communication with the agents is initiated by the agents themselves**.
This means that the agent must always initiate communication and not vice versa.

In order to use the agent commands feature, it must first be enabled, as it is disabled by default.
See the [Configuration section](configuration/agent-command-configuration.md#configuration) for more information.

Once the feature is enabled, specific agent commands can be triggered via the configuration server.
The configuration server offers a certain [set of endpoints](https://github.com/inspectIT/inspectit-ocelot/blob/f01ad602a3b188d3be0d17eca22bc4370913b6a1/components/inspectit-ocelot-configurationserver/src/main/java/rocks/inspectit/ocelot/rest/command/AgentCommandController.java) for this purpose. 

Once a command has been created, it is ready for the agent to pick up in the configuration server.
The agent then asks the configuration server at certain intervals whether new commands exist and obtains the commands assigned to it.
After that, the respective commands are executed by the agent and the result is sent back to the configuration server.

Due to the fact that the agent fetches the created agent commands in a certain interval, in the worst case it can take exactly the time of the interval until the agent receives a new command.
However, once the agent receives a command, it switches to its "live"-mode for a specific period of time where it receives commands in near real time.
If no more commands appear then, the agent will then switch back to normal mode, where it will again obtain the commands at the interval mentioned earlier.

## General Configuration

The agent command feature is disabled by default and must be enabled on the agent side.
This can be achieved with the help of the following configuration:

```YAML
inspectit:
  agent-commands:
    enabled: true
```

Now the agent has to be configured where it can reach the configuration server and retrieve its commands.
For this, the following endpoint of the configuration server has to be used: `/api/v1/agent/command`.
This is achieved by configuring the URL as follows:

```YAML
inspectit:
  agent-commands:
    url: http://<CONFIGURATION_SERVER_AND_PORT>/api/v1/agent/command
```

As soon as this is activated, the agent obtains the commands stored in the configuration server and executes them.
The complete configuration for activating the agent commands thus looks as follows.

```YAML
inspectit:
  agent-commands:
    enabled: true
    url: http://<CONFIGURATION_SERVER_AND_PORT>/api/v1/agent/command
```

### Dynamic Agent Command URL

In larger environments, where many agents exist and these reach the configuration server under different addresses, it can be cumbersome and difficult to successfully configure the agent command URL.
Especially if the configuration server HTTP address is already successfully set up, e.g. via JVM properties, you do not want to have to configure the agent command URL additionally.

For this purpose, there is the possibility that the agent derives the agent command URL based on the configured URL for retrieving the agent configuration via HTTP.
This possibility can be activated by using the following configuration:

```YAML
inspectit:
  agent-commands:
    derive-from-http-config-url: true
```

:::tip
This setting takes the URL defined under `inspectit.config.http.url` as a basis.
:::

This setting has a higher priority than manually specifying the URL.
If an agent command URL is configured and the `derive-from-http-config-url` option is enabled, the URL is ignored if it is possible to derive the agent command URL based on the HTTP configuration URL.

To generate the agent command URL, only the information regarding protocol, host and port is used from the URL of the HTTP configuration. 
By default, `/api/v1/agent/command` is used as the path of the URL.
However, this can be adjusted using the following configuration, for example in the case that the configuration server is operated behind a reverse proxy and the URLs are not accessible under their actual name:

```YAML
inspectit:
  agent-commands:
    agent-command-path: "/proxy/command"
```

### Additional Configuration Options

The agent command feature can be more precisely configured to the needs with the following optional parameters which are defined bellow the property `inspectit.agent-commands`:

| Property                           | Default Value | Description                                                                                                                                                                                                                                             |
|------------------------------------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| <nobr>`live-socket-timeout`</nobr> | `30s`         | The timeout duration used when the agent is in discovery mode. Defining how long the agent will wait for new commands.                                                                                                                                  |
| `socket-timeout`                   | `5s`          | The timeout duration used for requests when the agent is in normal mode.                                                                                                                                                                                |
| `polling-interval`                 | `15s`         | The used interval for polling agent commands.                                                                                                                                                                                                           |
| `live-mode-duration`               | `2m`          | How long the agent will staying in the live mode, before falling back to the normal mode.                                                                                                                                                               |
| `retry.max-attempts`               | `7`           | The maximum number of attempts to try to fetch the configuration. Integers must be greater or equal to 1.                                                                                                                                               |
| `retry.initial-interval`           | `30s`         | The initial interval to wait after the first failed attempt. Durations must be greater or equal to 1 ms.                                                                                                                                                |
| `retry.multiplier`                 | `2`           | For each retry the last interval to wait is multiplied with this number to calculate the next interval to wait. Decimals must be greater or equal to 1.0.                                                                                               |
| `retry.randomization-factor`       | `0.1`         | This factor introduces randomness to what the actual wait interval will be. This prevents that a lot of agents will issue requests towards the configuration server at the same time. Decimals between 0.0 (exclusive) and 1.0 (inclusive) are allowed. |

In case the specified HTTP endpoint is temporarily not available, inspectit applies by default a retry mechanism with
exponential backoff in order to save resources. If Ocelot agent cannot reload the configuration successfully after the
maximum number of attempts the reason is logged and the standard polling mechanism starts again. This may cause a new retry cycle to start. Turn retries off by removing all properties starting with `inspectit.agent-commands.retry`.

## Command-specific Configuration

Some agent commands need specific configuration to work, which is described in the following.

### Log Preloading for Logs Command

The Logs Command allows retrieving the agent logs via the configuration server.
For that, the agent preloads its own logs into a memory buffer, from which log entries are then retrieved by the command.

Log preloading is disabled by default and can be enabled as follows:

```YAML
inspectit:
  log-preloading:
    enabled: true
```

Preloading can be further configured, using the following properties below `inspectit.log-preloading`:

| Property              | Default Value | Description                                                                                                                                                                      |
|-----------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `log-level`           | `WARN`        | The minimum log level to preload. If it is `WARN`, only log messages with level `WARN` and `ERROR` are preloaded. Allowed values are: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`. |
| `buffer-size`         | `128`         | The maximum number of log messages to preload. When reaching the size, oldest messages are dropped first.                                                                        |

:::warning
Please note that any change to the log preloading configuration will cause all previously preloaded messages to be dropped.
:::
