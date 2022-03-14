---
id: installation
title: Installation
---

This section describes the installation details for the inspectIT Ocelot agent.

## Supported Java Runtime Environments

The inspectIT Ocelot supports Java Runtime Environments in version 1.8.0 and above. You will not be able to use the agent with the lower Java versions.
The agent works with different JRE distributions including Oracle, openJDK, Azul, etc.

## Adding the Agent to a JVM

The best option for using the inspectIT Ocelot is to include it to the start of the JVM by using the `-javaagent` command-line option.
This way the agent will be initialized before your application starts.

```bash
$ java -javaagent:"/path/to/inspectit-ocelot-agent-{inspectit-ocelot-version}.jar" -jar my-java-program.jar
```

> Some application servers have dedicated scripts that are used to launch the actual JVM that runs the application. In such cases, you must alter the start-up scripts in order to instrument the correct JVM.

## Attaching the Agent to a Running JVM

inspectIT Ocelot also supports attaching the agent to an already running JVM.
In such a scenario the collection of metrics and traces will start from the point of the attachment.

The attaching can easily be done using the agent itself and executing the following command:

```bash
$ java -jar inspectit-ocelot-agent-{inspectit-ocelot-version}.jar <PID> [<AGENT_ARGUMENTS>]
```

In the following example, we are attaching the agent to the JVM process `1337` and passing some [additional arguments](configuration/configuration-sources.md#java-agent-arguments) to it:
```bash
$ java -jar inspectit-ocelot-agent-{inspectit-ocelot-version}.jar 1337 '{inspectit:{service-name:"my-agent"}}'
```

> The agent is internally using the utility [jattach](https://github.com/apangin/jattach) for attaching itself to a running JVM.

In order to find the process ID of a running JVM, you can use the `jcmd` to list all the running Java processes on your machine:

```bash
$ jcmd -l
```

### Attaching Using jattach

Another way of attaching the agent to a running JVM is to use the utility [jattach](https://github.com/apangin/jattach):

```bash
$ ./jattach.sh <PID> load instrument false /path/to/inspectit-ocelot-agent-{inspectit-ocelot-version}.jar='{"inspectit.service-name" : "MyService"}'
```
In this example we're also passing [JSON arguments](configuration/configuration-sources.md#java-agent-arguments) to the agent in order to configure its service name.

> Using the attach options has some limitations with respect to using the OpenCensus instrumentation library in combination with the inspectIT Ocelot agent. Please refer to [OpenCensus Configuration](configuration/open-census-configuration.md) section to understand these limitations.

### Kubernetes

If you want to use the agent in a K8s environment, you can do so by using the [OpenTelemetry K8s Operator](https://github.com/open-telemetry/opentelemetry-operator).  
It is still in development, so it is not feature-complete, but depending on your needs the current version could already provide everything necessary.  
This also means that the installation steps described in the following could be out-of-date, but they might still help with navigating their documentation on installation by showing you which parts you need.

#### Installation
Install OpenTelemetry Operator as described in [the readme](https://github.com/open-telemetry/opentelemetry-operator#getting-started).  
As of creating this section this means doing the following:

   1. Installing [cert-manager](https://cert-manager.io/docs/installation/) in your cluster if you have not yet. 
   2. Running the following command with your desired version:
   ```shell
   kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/download/v{version}/opentelemetry-operator.yaml
   ```

#### Usage
1. Annotate the namespaces or containers as described in [the readme](https://github.com/open-telemetry/opentelemetry-operator#getting-started).  
As of creating this section the annotation for java agent injection is:
```yaml
instrumentation.opentelemetry.io/inject-java: "true"
```

2. (Optional) Add environment variables for configuration to the containers.  
For example to connect the agent to a config-server for further configuration and set a service-name you could set INSPECTIT_CONFIG_HTTP_URL and INSPECTIT_SERVICE_NAME like in the following:
```yaml
containers:
  - image: my-app-image
    name: my-app
    env:
      - name: INSPECTIT_CONFIG_HTTP_URL
        value: http://my-ocelot-config-server:8090/api/v1/agent/configuration
      - name: INSPECTIT_SERVICE_NAME
        value: my-service-name
```
For further info see [Configuration Sources](../configuration/configuration-sources.md).

3. Create an Instrumentation object like the following with spec.java.image set to the inspectIT Ocelot agent image you would like to use (only works with version 1.15.2 and up):

```yaml
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: inspectit-instrumentation
spec:
  java:
    image: inspectit/inspectit-ocelot-agent:1.15.2
```

4. Restart the containers to trigger the insertion.  
As of now changes in the Instrumentation object are only applied on a restart of a container, however there are plans to make automatic restarting configurable, see [issue #553](https://github.com/open-telemetry/opentelemetry-operator/issues/553).
