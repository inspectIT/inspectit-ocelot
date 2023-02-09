---
id: version-2.4.1-installation
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
$ java -javaagent:"/path/to/inspectit-ocelot-agent-2.4.1.jar" -jar my-java-program.jar
```

> Some application servers have dedicated scripts that are used to launch the actual JVM that runs the application. In such cases, you must alter the start-up scripts in order to instrument the correct JVM.

## Attaching the Agent to a Running JVM

inspectIT Ocelot also supports attaching the agent to an already running JVM.
In such a scenario the collection of metrics and traces will start from the point of the attachment.

The attaching can easily be done using the agent itself and executing the following command:

```bash
$ java -jar inspectit-ocelot-agent-2.4.1.jar <PID> [<AGENT_ARGUMENTS>]
```

In the following example, we are attaching the agent to the JVM process `1337` and passing some [additional arguments](configuration/configuration-sources.md#java-agent-arguments) to it:
```bash
$ java -jar inspectit-ocelot-agent-2.4.1.jar 1337 '{"inspectit":{"service-name":"my-agent"}}'
```

> The agent is internally using the utility [jattach](https://github.com/apangin/jattach) for attaching itself to a running JVM.

In order to find the process ID of a running JVM, you can use the `jcmd` to list all the running Java processes on your machine:

```bash
$ jcmd -l
```

### Attaching Using jattach

Another way of attaching the agent to a running JVM is to use the utility [jattach](https://github.com/apangin/jattach):

```bash
$ ./jattach.sh <PID> load instrument false /path/to/inspectit-ocelot-agent-2.4.1.jar='{"inspectit.service-name" : "MyService"}'
```
In this example we're also passing [JSON arguments](configuration/configuration-sources.md#java-agent-arguments) to the agent in order to configure its service name.

> Using the attach options has some limitations with respect to using the OpenCensus instrumentation library in combination with the inspectIT Ocelot agent. Please refer to [OpenCensus Configuration](configuration/open-census-configuration.md) section to understand these limitations.

## Using the Agent With a Security Manager

If a Java Security Manager is enabled, the agent needs to be granted additional permissions to work. 
For this, add the following to your policy file:

```
grant codeBase "file:<absolute_path_to_inspectit-ocelot-agent.jar>" {
    permission java.security.AllPermission;
};
```

The correct policy file location depends on different factors.
See the [official Java documentation](https://docs.oracle.com/en/java/javase/17/security/permissions-jdk1.html#GUID-789089CA-8557-4017-B8B0-6899AD3BA18D) for further information.

## Using the Agent with Kubernetes

There are several ways to use the agent in a Kubernetes cluster.
For example, you could integrate the agent directly into the application container images, but this requires customizing all images.

Another possibility is that the agent is automatically injected into the application containers using an **operator** and attached to the JVM processes.
For this purpose, the [OpenTelemetry K8s Operator](https://github.com/open-telemetry/opentelemetry-operator) can be used, with which it is possible to automatically roll out the inspectIT Ocelot Java Agent.
It is still under development, so it is not feature-complete, but depending on your needs the current version could already provide everything needed.

:::warning Up-to-dateness of the Documentation
Since the OpenTelemetry K8s operator is currently under heavy development, the installation steps described below **may be outdated**.
They may nevertheless be helpful in navigating the OpenTelemetry Operator installation documentation by showing you which parts you need.
:::

### Installing the Operator

Install the OpenTelemetry Operator as described in its [official readme file](https://github.com/open-telemetry/opentelemetry-operator#getting-started). This includes the following steps:

1. Install the [cert-manager](https://cert-manager.io/docs/installation/) in your cluster if you have not done it already.
2. Install the operator using the following command. Please note that this will install the latest version of it:

    ```shell
    kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
    ```

    By adjusting the URL to a different GitHub release, a specific version of the operator can be used:

    ```shell
    kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/download/v{version}/opentelemetry-operator.yaml
    ```

### Using the Operator

1. Create an `Instrumentation` object as shown below. Set the `spec.java.image` to the inspectIT Ocelot agent container image you would like to use:

    :::note
    Please note that only container images of the inspectIT Ocelot Agent starting from version `1.15.2` are compatible and work with the OpenTelemetry K8s Operator.
    :::

    ```yaml
    apiVersion: opentelemetry.io/v1alpha1
    kind: Instrumentation
    metadata:
      name: my-instrumentation
    spec:
      java:
        image: inspectit/inspectit-ocelot-agent:1.15.2
    ```

2. Annotate namespaces or containers that should receive the agent as described in the [official readme file](https://github.com/open-telemetry/opentelemetry-operator#getting-started). The possible values for the annotation can be:

    - `true` - inject the `Instrumentation` resource from the namespace.
    - `my-instrumentation` - name of Instrumentation CR instance.
    - `false` - do not inject

    The following annotation can be used for this:
    ```yaml
    instrumentation.opentelemetry.io/inject-java: "true"
    ```

    :::warning Ensure Correct Referencing
    If the operator cannot find the instrumentation object, e.g. because none was created or the name was written incorrectly in the annotation, the containers will not be started!
    :::

3. (Optional) Add environment variables to the containers to configure the agent. See the following section for using [environment variables to configure](configuration/configuration-sources.md#os-environment-variables) the inspectIT Ocelot agent.

    For example, to set a service-name for the agent and connect it to a specific configuration-server, you could set the `INSPECTIT_CONFIG_HTTP_URL` and `INSPECTIT_SERVICE_NAME` environment variable like in the following:

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

    You can also take a look at the [deployment file](https://github.com/inspectIT/trading-demo-application/blob/main/k8s/deployment.yaml) of the [trading demo application](https://github.com/inspectIT/trading-demo-application) where exactly this is set up.

4. Start or restart the containers to trigger the injection and attachment of the agent.

    Currently, the operator **will not automatically restart running containers** in case changes are made to the `Instrumentation` objects. However, there are plans to provide the ability to restart containers in order to roll out changes of the configurable `Instrumentation` objects automatically (see [issue #553](https://github.com/open-telemetry/opentelemetry-operator/issues/553)).
