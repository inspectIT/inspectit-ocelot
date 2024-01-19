---
id: version-2.6.2-status-table-view
title: Status Table View Guide
sidebar_label: Agent Status Table View
original_id: status-table-view
---

The Status Table in the Agent Status page offers, in addition to displaying the most important agent information in the table, the possibility to retrieve additional information via different buttons.
Here is a short guide to help you navigate around the status table.

![Status Table View](assets/status-table-view-ui.png)

## Explanation of Highlighted Buttons

| Button                                                                          | Function                                                                                                                                                                                                                                                    |
|---------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![Status Table View](assets/service-states-btn.png)<br /> **Service States**    | Displays the services (e.g. Prometheus, Jaeger, Influx, Log-Preloading, Agent Command Service, ...) and their current state (`enabled`/`disabled`).<br />In the future, we plan to implement the functionality to enable/disable the services in this view. |
| ![Status Table View](assets/logs-btn.png)<br /> **Agent Logs**                  | Displays the logs of the service if agent commands and log preloading are enabled.                                                                                                                                                                          |
| ![Status Table View](assets/config-btn.png)<br /> **Current Config**            | Displays the current config in `yaml` format when the agent commands are enabled.                                                                                                                                                                           |
| ![Status Table View](assets/agent-health-icon.png)<br /> **Agent State**        | Displays the current agent state and the latest agent health incidents.                                                                                                                                                                                     |
| ![Status Table View](assets/download-archive-btn.png)<br /> **Support Archive** | Downloads a support archive as a `.zip` file if the agent commands are enabled. The support archive contains logs (if log preloading is enabled), the current config, and environment details.                                                              |
