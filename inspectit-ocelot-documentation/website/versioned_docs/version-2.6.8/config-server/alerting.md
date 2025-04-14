---
id: version-2.6.8-alerting
title: Alerting
sidebar_label: Alerting
original_id: alerting
---

The *inspectIT Configuration Server* allows to create alerting rules for the [InfluxDB Kapacitor](https://www.influxdata.com/time-series-platform/kapacitor/).
Thus, the alerting can only be used, when using InfluxDB as datasource for metrics.
By default, alerting rules are disabled and the page is not visible. To enable alerting, you have to provide
the following properties to the server's configuration:

```yaml
inspectit-config-server:
  kapacitor:
      url: "localhost:8086"
      username: "user"     
      password: "password"  
```

## Overview

At start up the configuration server will try to ping the configured url.
Every communication between the configuration server and the Kapacitor utilizes the [Kapacitor HTTP API](https://docs.influxdata.com/kapacitor/v1/working/api/).
The alerting system of Kapacitor consists of templates, tasks, topics and handlers.

Templates and topics have to be written in the Kapacitor itself, before using them with the configuration server.
The official Kapacitor documentation describes how to create
[templates](https://docs.influxdata.com/kapacitor/v1/working/api/#manage-templates) and 
[topics](https://docs.influxdata.com/kapacitor/v1/working/api/#manage-alerts).
The configuration server allows you to create [tasks](https://docs.influxdata.com/kapacitor/v1/working/api/#manage-tasks) 
out of templates and assign [handlers](https://docs.influxdata.com/kapacitor/v1/working/api/#list-topic-handlers)
to specific topics.

In summary, templates are scripts with dynamic placeholders, which should recognize anomalies in metrics to fire alerts. 
They are used as basis for several tasks. Kapacitor tasks resemble templates with filled out parameter values.
Tasks will send alerts to topics, when anomalies are detected. 
Every topic contains handlers, which define how the alerts should be processed, for example by sending an e-mail.

## Alerting rules

Alerting rules are responsible to detect anomalies in your metrics and fire alerts.
To create alerting rules, you have to prepare at least one template within your Kapacitor instance. 

When creating an alerting rule, you have to specify a Kapacitor template, the rule name as well as an optional description.
The provided templates will contain variables, which can be set within the alerting rule.
When saving the rule, a task will be sent to Kapacitor. 
Note, when enabling or disabling the rule, you also have to save it to update the Kapacitor task.
Every alerting rule will be linked to one notification channel, which resemble the Kapacitor topics.

![Alerting rules view](assets/alerting-page.png)

## Notification Channels

The notification channels of the configuration server can be used to send notifications when Kapacitor tasks have
created alerts. To use channels, you have to prepare at least one topic within your Kapacitor instance.

Every channel should contain at least one handler. Handler define how detected alerts should be processed.
When creating a handler, you have to specify type, channel and name.
The configuration server currently provides two handler types: 
- _smtp_ allows you to link an e-mail address to the notification channel. 
- _publish_ will link other channels with the current one.

![Alerting channels view](assets/alerting-channels.png)