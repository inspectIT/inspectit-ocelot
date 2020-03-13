---
id: eum-server-setup
title: Setup the EUM Server
sidebar_label: Server Setup
---

Before starting the server, download the [latest release](https://github.com/inspectIT/inspectit-ocelot/releases) or build it by cloning the repository and executing the following command :

```bash
$ ./gradlew build
```

Start the EUM server with the following command:

```bash
$ java -jar inspectit-ocelot-eum-{version}.jar
```

By default, the server is starting on port `8080`. 
You can simply configure the port by using the Java property `-Dserver.port=[PORT]`:

```bash
$ java -Dserver.port=[port] -jar inspectit-ocelot-eum-0.0.1-SNAPSHOT.jar
```

Our server is delivered with a default configuration 
supporting the metrics `t_page`, `t_done`, `rt.tstart`, `rt.nstart` and `rt.end` of the Boomerang plugin [RT](https://developer.akamai.com/tools/boomerang/docs/BOOMR.plugins.RT.html).

In order to provide a custom configuration, please set the Java property `-Dspring.config.location=file:[PATH-TO-CONFIG]`:

```bash
$ java -Dserver.port=[port] -Dspring.config.location=file:[path-to-config] -jar inspectit-ocelot-eum-0.0.1-SNAPSHOT.jar
```
