---
id: quick-start
title: Quick Start
---

You can find and download all released versions of inspectIT Ocelot in our [GitHub](https://github.com/inspectIT/inspectit-ocelot/releases) repository.
You can get the current version on the following link:

```bash
$ wget https://github.com/inspectIT/inspectit-oce/releases/download/{inspectit-ocelot-version}/inspectit-ocelot-agent-{inspectit-ocelot-version}.jar
```

The best way to start using inspectIT Ocelot is to attach the Java agent when starting your Java program.
Use the `-javaagent` command-line option to reference the inspectIT Ocelot jar:

```bash
$ java -javaagent:"/path/to/inspectit-ocelot-agent-{inspectit-ocelot-version}.jar" -jar my-java-program.jar
```

The [Installation](installation.md) section further describes what options are available for installing the agent, as well as how you can attach the agent to an already started JVM.
In the [Configuration](configuration/configuration-sources.md) section you can find more details on how to configure the inspectIT Ocelot agent.