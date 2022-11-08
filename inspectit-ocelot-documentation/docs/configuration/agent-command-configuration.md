---
id: agent-command-configuration
title: Agent Command Configuration
sidebar_label: Agent Command Configuration
---

Since version `1.10.0` the inspectIT Ocelot agent provides the _Agent Commands_ feature, which allows interacting with a specific agent.
Using this feature is only possible if the agent is used together with the inspectIT Ocelot Configuration-Server.

With this feature it is possible to send specific commands or operations to the agent via the configuration server, which the agent should execute.
Subsequently, the result of these commands is returned.

Simple commands can be executed, such as a "ping" command whether an agent exists or more complex commands, such as loading the class structure of the application in which the agent is integrated.
Some new functionality of the configuration server interface is based on this feature, e.g. the _class browser_.
These functions work only if this feature is enabled.

:::warning
It is important to note that depending on the complexity of the agent commands, they may have a negative impact on the performance of the target application.
:::

## How it works

For security reasons, **all communication with the agents is initiated by the agents themselves**.
This means that the agent must always initiate communication and not vice versa.  
However, since version `1.16/2.0? todo` agent commands can still be sent from the configuration server to the agent without any delay.
This works using a bidirectional streaming gRPC connection, that the agent opens once and that then is used for all communication regarding agent commands.
The server sends commands over that connection as soon as they come in, and then the agent executes the command and responds over the same connection.

In order to use the agent commands feature, it must first be enabled, as it is disabled by default.
See [General Configuration](#general-configuration) below for more information.

Once the feature is enabled, specific agent commands can be triggered via the configuration server.
The configuration server offers a certain [set of endpoints](https://github.com/inspectIT/inspectit-ocelot/blob/master/components/inspectit-ocelot-configurationserver/src/main/java/rocks/inspectit/ocelot/rest/command/AgentCommandController.java) for this purpose.

### Existing Commands
|  Name       | Description                                                                                                                                                                                                      | Endpoint                     | Parameters      |
|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|-----------------|
| Ping        | Simple ping command to see if an agent exists and is available.                                                                                                                                                  | /api/v1/ping                 | agent-id        |
| ListClasses | Requests a list of classes available in the application the agent is attached to. <br>The full class set is filtered using a query. <br>Also provides info about the classes' types and a list of their methods. | /api/v1/command/list/classes | agent-id, query |
| Logs        | Retrieves logs from an agent. <br> See also its command-specific [configuration section](#log-preloading-for-logs-command).                                                                                      | /api/v1/logs                 | agent-id        |


## General Configuration

The agent command feature is disabled by default and must be enabled on the agent side using the `enabled` property.  
Furthermore, the agent needs to know where it can reach the configuration server to establish the connection for receiving the commands, this is set in the `host` property.

The complete configuration for activating the agent commands thus looks as follows.

```YAML
inspectit:
  agent-commands:
    enabled: true
    host: <CONFIGURATION_HOST>
```

:::warning
By default the agent commands and responses are sent as plaintext without encryption. To use TLS, at least a certificate for the config-server needs to be provided. See [Using TLS](#using-tls) for further info.
:::

### Dynamic Agent Command URL

In larger environments, where many agents exist and these reach the configuration server under different addresses, it can be cumbersome and difficult to successfully configure the agent command host.
Especially if the configuration server HTTP address is already successfully set up, e.g. via JVM properties, you do not want to have to configure the agent command URL additionally.

For this purpose, there is the possibility that the agent derives the agent command host based on the configured URL for retrieving the agent configuration via HTTP.
This possibility can be activated by using the following configuration:

```YAML
inspectit:
  agent-commands:
    derive-from-http-config-url: true
```

:::tip
This setting takes the URL defined under `inspectit.config.http.url` as a basis.
:::

This setting has a higher priority than manually specifying the host.
If an agent command host is configured and the `derive-from-http-config-url` option is enabled, the host is ignored if it is possible to derive the agent command host based on the HTTP configuration URL.

### Using TLS

To use TLS you will at least need a valid certificate and private key for your configuration server. 
Depending on your needs you might have to change additional settings too.

:::tip
The [Trouble-Shooting section](https://yidongnan.github.io/grpc-spring-boot-starter/en/trouble-shooting.html#network-closed-for-unknown-reason) of the library the configuration server uses to set up the gRPC service can help with problems with setting up TLS.
:::

#### Enabling TLS on the configuration server

The configuration server uses [grpc-spring-boot-starter](https://github.com/yidongnan/grpc-spring-boot-starter), so the [Server Security](https://yidongnan.github.io/grpc-spring-boot-starter/en/server/security.html) section of their documentation is the base for the server-side part of this documentation.

To enable TLS on the configuration server the property `security.enabled` needs to be set to `true`.  
To make TLS work, the properties `certificate-chain` and `private-key` need to be set as well to a path to your certificate and the corresponding private key.  
So in the end a configuration for the configuration server could look like this:
```YAML
grpc:
  server:
    security:
      enabled: true
      certificate-chain: file:certificates/server.pem
      private-key: file:certificates/server.key
```

On the agent-side you need to set the property `use-tls` to `true`.  
If the issuing certificate authority of the server's certificate is not known to the agent, then you also need to set the property `trust-cert-collection-file-path` to the path to a file containing a collection of trusted certificates.  
Finally, if the server's certificate is not valid for the hostname the agent uses to connect itself, you can also provide that hostname to fix the problem under `authority-override`.  
So in the end a configuration for the agent could look like this:
```YAML
inspectit:
  agent-commands:
    use-tls: true
    trust-cert-collection-file-path: certificates/ca.pem
    authority-override: ocelot-config-server
```

#### Mutual authentication

To use [mutual authentication](https://en.wikipedia.org/wiki/Mutual_authentication) the following needs to be done additionally to the settings in the previous section.

In the configuration server's configuration you need to set the property `client-auth` to either `OPTIONAL` or rather `REQUIRED`, since only the latter enforces mutual authentication.  
Furthermore `trust-cert-collection` needs to be set to a collection of trusted certificates, so the server knows which client certificates are trusted:
```YAML
grpc:
  server:
    security:
      client-auth: REQUIRED
      trust-cert-collection: file:certificates/ca.pem
```

In the agent's configuration you need to set the properties `client-cert-chain-file-path` and `client-private-key-file-path` to a certificate and corresponding key for the agent:
```YAML
inspectit:
  agent-commands:
    client-cert-chain-file-path: certificates/client.pem
    client-private-key-file-path: certificates/client.key
```

### Additional Configuration Options

The agent command feature can be more precisely configured to your needs with some optional parameters described below.

For the agent there are the following properties which are defined below `inspectit.agent-commands`:

| Property                   | Default Value | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|----------------------------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `port`                     | `9657`        | Port the agent will use to connect to the config-server over gRPC, should correspond to `grpc.server.port` in configuration server's properties.                                                                                                                                                                                                                                                                                                                                                     |
| `max-inbound-message-size` | `4`           | Maximum size for inbound gRPC messages, i.e. commands from config-server, in MiB.                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `backoff-reset-time`       | `60s`         | Time after which the backoff between retries to re-establish the gRPC connection between agent and configuration server is reset to the lowest value.                                                                                                                                                                                                                                                                                                                                                |                                                                                  |
| `max-backoff-increases`    | `4`           | How often the backoff between retries to re-establish the gRPC connection between agent and config-server is increased for the next retry. Backoff is calculated as 2 to the power of how often the backoff has been increased plus 1, so a value of 4 means that the maximum backoff is 32 seconds. <br>This setting only sets a maximum for the backoff between retries, it does not affect the number of retries, the service will always continue to try reconnecting on errors unless disabled. |

For the configuration server there are the following properties:

| Property                                                          | Default Value | Description                                                                                                                           |
|-------------------------------------------------------------------|---------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `inspectit-config-server.agent-commands.response-timeout`         | `30s`         | How long a command will wait for a response from the agent before throwing a timeout-exception.                                       |
| `inspectit-config-server.agent-commands.max-inbound-message-size` | `4`           | Maximum size for inbound grpc messages, i.e. responses from agents, in MiB.                                                           |
| `grpc.server.port`                                                | `9657`        | The server's grpc port. Needs to be synced with the port set in the agent configuration. If set to -1 the grpc server is deactivated. |

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
