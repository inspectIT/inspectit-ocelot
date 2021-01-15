---
id: version-1.6.1-installation
title: Installation
original_id: installation
---

This section describes the installation details for the inspectIT Ocelot agent.

## Supported Java Runtime Environments

The inspectIT Ocelot supports Java Runtime Environments in version 1.8.0 and above. You will not be able to use the agent with the lower Java versions.
The agent works with different JRE distributions including Oracle, openJDK, Azul, etc.

## Adding the Agent to a JVM

The best option for using the inspectIT Ocelot is to include it to the start of the JVM by using the `-javaagent` command-line option.
This way the agent will be initialized before your application starts.

```bash
$ java -javaagent:"/path/to/inspectit-ocelot-agent-1.6.1.jar" -jar my-java-program.jar
```

> Some application servers have dedicated scripts that are used to launch the actual JVM that runs the application. In such cases, you must alter the start-up scripts in order to instrument the correct JVM.

## Attaching the Agent to a Running JVM

inspectIT Ocelot also supports attaching the agent to an already running JVM.
In such a scenario the collection of metrics and traces will start from the point of the attachment.

The attaching can easily be done using the agent itself and executing the following command:

```bash
$ java -jar inspectit-ocelot-agent-1.6.1.jar <PID> [<AGENT_ARGUMENTS>]
```

In the following example, we are attaching the agent to the JVM process `1337` and passing some [additional arguments](configuration/configuration-sources.md#java-agent-arguments) to it:
```bash
$ java -jar inspectit-ocelot-agent-1.6.1.jar 1337 '{inspectit:{service-name:"my-agent"}}'
```

> The agent is internally using the utility [jattach](https://github.com/apangin/jattach) for attaching itself to a running JVM.

In order to find the process ID of a running JVM, you can use the `jcmd` to list all the running Java processes on your machine:

```bash
$ jcmd -l
```

### Attaching Using jattach

Another way of attaching the agent to a running JVM is to use the utility [jattach](https://github.com/apangin/jattach):

```bash
$ ./jattach.sh <PID> load instrument false /path/to/inspectit-ocelot-agent-1.6.1.jar='{"inspectit.service-name" : "MyService"}'
```
In this example we're also passing [JSON arguments](configuration/configuration-sources.md#java-agent-arguments) to the agent in order to configure its service name.

> Using the attach options has some limitations with respect to using the OpenCensus instrumentation library in combination with the inspectIT Ocelot agent. Please refer to [OpenCensus Configuration](configuration/open-census-configuration.md) section to understand these limitations.

