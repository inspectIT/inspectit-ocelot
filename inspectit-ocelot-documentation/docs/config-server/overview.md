---
id: overview
title: Configuration Server Overview
sidebar_label: Overview
---

The *inspectIT Configuration Server* is a standalone component provided by the inspectIT Ocelot project.
It can be used to centrally manage and distribute the inspectIT Ocelot agent configuration files.
For this purpose, HTTP endpoints are provided that can be queried by the agents to obtain the corresponding configuration.
This has the advantage that the manual administration of the configuration files is not required.

![Configuration Server Architecture](assets/configuration-server-architecture.png)

The server provides a web interface to manage all relevant settings and configurations. It can be accessed via `http://<server-address>:<port>/ui/`. By default, the configuration server is listening to port `8090`.

In addition, a Swagger UI is provided which contains a list including documentation of the server's REST API. By default, the Swagger UI can be accessed via `http://<server-address>:<port>/swagger-ui.html`.

## Management of Configuration Files

Since version 1.4, the configuration server uses Git and a local repository to manage the working directory. If a working directory already exists that was created by a configuration server in version 1.3 or lower, it is automatically migrated to a working directory managed using a local Git repository. No manual adjustments are necessary.

Please see [Managing Configuration Files](config-server/managing-configurations.md) for more information on managing of the configuration files.