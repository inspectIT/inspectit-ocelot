---
id: tags-exporters
title: Tags Exporters
---

Tags exporters represent special exporters of InspectIT, which allow to export internal tags to external applications like browsers.
Currently, there is only one tags exporter:

| Exporter                        |Supports run-time updates| Push / Pull |Enabled by default|
|---------------------------------|---|-------------|---|
| [HTTP Exporter](#http-exporter) |Yes| Push & Pull |No|

## HTTP Exporter

The HTTP exporter exports tags via a REST-API running on an HTTP-server. The server provides two endpoints.
One GET-endpoint to expose data to external applications and one PUT-endpoint to receive data from external applications.
The server is by default started on the port `9000` and data can then be accessed or written by 
calling 'http://localhost:9000/inspectit'

Data will always be stored behind a provided session-ID to ensure data correlation with its browser. The session-ID is read from the `cookie`-header. You cannot access any data inside the HTTP-server without an existing session-ID.
A HTTP-server can only store a specific amount of sessions, which can be configured in the configuration server. Additionally, every session will be cached only for a specific amount of time.


The following properties are nested properties below the `inspectit.exporters.tags.http` property:

| Property         | Default      | Description
|------------------|--------------|---|
| `.enabled`       | `DISABLED`   |If `ENABLED` or `IF_CONFIGURED`, the inspectIT Ocelot agent will try to start the exporter and HTTP server.
| `.host`          | `0.0.0.0`    |The hostname or network address to which the HTTP server should bind.
| `.port`          | `9000`       |The port the HTTP server should use.
| `.path`          | `/inspectit` |The path on which the HTTP endpoints will be available.
| `.session-limit` | `32`         |How many sessions can be stored in the server at the same time.
| `.time-to-live`  | `300`        |How long the data should be stored in the server in seconds.


The data of the HTTP exporter is stored inside an internal data storage. Data can be written to the storage
by using [browser propagation](../instrumentation/rules.md#data-propagation)

