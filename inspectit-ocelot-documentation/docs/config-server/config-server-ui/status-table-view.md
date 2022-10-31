---
id: status-table-view
title: Status Table View Guide
sidebar_label: Status Table View
---

Here is a short guide to help you navigate around the status table.

![Status Table View](assets/../../../assets/status-table-view-ui.png)

#### Marked Buttons explained
- ![Status Table View](assets/../../../assets/service-states-btn.png) Here the current services (e.g. Prometheus, Jaeger, Influx, Log-Preloading, Agent Command Service, ...) and their current state enabled/disabled are being displayed. <br>
In the future it should be as easy as pressing a button to enable/disable a service.

- ![Status Table View](assets/../../../assets/logs-btn.png) When the agent commands and the log preloading are activated, the logs of the service are being shown here.

- ![Status Table View](assets/../../../assets/config-btn.png) When the agent commands are activated, the current config in yaml format is being shown here.

- ![Status Table View](assets/../../../assets/download-archive-btn.png) When the agent commands and the log preloading are activated, you can download the support archive as a .zip file here, containing logs, config and environment details.