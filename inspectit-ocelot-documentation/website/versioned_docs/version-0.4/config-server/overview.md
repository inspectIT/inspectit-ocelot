---
id: version-0.4-overview
title: Configuration Server Overview
sidebar_label: Overview
original_id: overview
---

The *inspectIT Configuration Server* is a standalone component provided by the inspectIT Ocelot project.
It can be used to centrally manage and distribute the inspectIT Ocelot agent configuration files.
For this purpose, HTTP endpoints are provided that can be queried by the agents to obtain the corresponding configuration.
This has the advantage that the manual administration of the configuration files is not required.

![Configuration Server Architecture](assets/configuration-server-architecture.png)

The server provides a web interface to manage all relevant settings and configurations. It can be accessed via `http://<server-address>:<port>/ui/`. By default, the configuration server is listening to port `8090`.

In addition, a Swagger UI is provided which contains a list including documentation of the server's REST API. By default, the Swagger UI can be accessed via `http://<server-address>:<port>/swagger-ui.html`.