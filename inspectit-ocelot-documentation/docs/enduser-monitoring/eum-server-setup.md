---
id: eum-server-setup
title: Setup the EUM Server
sidebar_label: Server Setup
---

You can download the [latest release](https://github.com/inspectIT/inspectit-ocelot-eum-server/releases) in this repository.
Or build the server by cloning the repository and executing the following command:

```bash
$ ./gradlew assemble
```

Start the EUM server with the following command:

```bash
$ java -jar inspectit-ocelot-eum-server-{version}.jar
```

By default, the server is starting on port `8080`. 
You can simply configure the port by using the Java property `-Dserver.port=[PORT]`:

```bash
$ java -Dserver.port=[port] -jar inspectit-ocelot-eum-server-{version}.jar
```

Our server is delivered with a default configuration supporting the metrics
`t_page`, `t_done`, `rt.tstart`, `rt.nstart`, `rt.end` and `restiming` of the Boomerang [RT](https://akamai.github.io/boomerang/akamai/BOOMR.plugins.RT.html) plugin.

In order to provide a custom configuration, please set the Java property `-Dspring.config.location=file:[path-to-config]`:

```bash
$ java -Dserver.port=[port] -Dspring.config.location=file:[path-to-config] -jar inspectit-ocelot-eum-server-{version}.jar
```
