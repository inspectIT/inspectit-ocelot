---
id: docker-examples
title: inspectIT Ocelot Demo Application
sidebar_label: Demo Application
---

## inspectIT Ocelot Demo

If you would like to check inspectIT Ocelot in action with a demo application, you can use our docker compose example.
We use the distributed version of the [Spring PetClinic sample application](https://github.com/spring-petclinic/spring-petclinic-microservices) as the target application in our demo.
To demonstrate the flexibility of the OpenCensus bases inspectIT agent, we provide different demo scenarios covering different monitoring and APM components.

All of the demo scenarios are fully configured with predefined dashboards, *so you can get started in 5 minutes*.

### Demo #1 - InfluxDB and Zipkin

Uses InfluxData Telegraf for metrics gathering, InfluxDB for metrics storage and Grafana for Dashboards. Traces are exported to Zipkin.

* File: `docker-compose-influxdb-zipkin.yml`
* [OpenAPM Landscape](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&collector=influx-telegraf%2Czipkin-server&storage=influx-db&dashboarding=grafana)

![Demo scenario using InfluxDB and Zipkin](assets/demo-landscape-influxdb-zipkin.png)

### Demo #2 - InfluxDB and Zipkin

Uses Prometheus Server for metrics gathering and storage, Grafana for Dashboards. Traces are exported to Jaeger.

* File: `docker-compose-prometheus-jaeger.yml`
* [OpenAPM Landscape](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&collector=prometheus-server%2Cjaeger-collector&dashboarding=grafana&visualization=jaeger-query)

![Demo scenario using Prometheus and Jaeger](assets/demo-landscape-prometheus-jaeger.png)


## Launching the Demo

*Pre-requisites:* To launch the demo, [Docker](https://www.docker.com/) needs to be installed on your system.
If you are using Docker for Windows or running Docker in a virtual machine, ensure that Docker has at least 4GB main memory assigned.

Either [download](https://github.com/inspectIT/inspectit-ocelot/archive/master.zip) or [clone the inspectit-Ocelot GitHub repository](https://github.com/inspectIT/inspectit-ocelot).

Change into the ```inspectit-ocelot-demo/``` directory and execute the following command to launch the desired demo scenario (replace [SCENARIO_POSTFIX] with the postfix of the scenario you would like to launch):

```bash
$ docker-compose -f docker-compose-[SCENARIO_POSTFIX].yml up
```

This will start all the Docker containers required for the corresponding demo scenario, including the Petclinic demo application.

You can access the demo application (PetClinic) under http://localhost:8080.
Details on accessing monitoring infrastructure components are listed below, depending on the selected demo scenario.

### Starting the Demo on Windows Using WSL

If you want to execute the demo on Windows using the Windows subsystem for linux (WSL), you have to mount your hard drive to the WSL's root directory due to a problem of Docker for Windows and its volume mounting.

For example, mounting your C drive to the root file system can be achieved using the following commands:

```bash
sudo mkdir /c
sudo mount --bind /mnt/c /c
```

For more information, check out the following blog post: [Setting Up Docker for Windows and WSL to Work Flawlessly](https://nickjanetakis.com/blog/setting-up-docker-for-windows-and-wsl-to-work-flawlessly)


## Demo Scenarios

### InfluxDB and Zipkin Scenario
In this scenario the following components are preconfigured and used for monitoring:

[![Demo scenario using InfluxDB and Zipkin](assets/demo-landscape-influxdb-zipkin.png)](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&collector=influx-telegraf%2Czipkin-server&storage=influx-db&dashboarding=grafana)

- *inspectIT Ocelot agent:* Instruments all the target demo application components.
- *InfluxData Telegraf:* Gathers metrics exposed by the agent.
- *InfluxDB:* Stores metric data collected by Telegraf as time series.
- *Grafana:* Provides predefined example Dashboards visualizing the metrics collected by the inspectIT Ocelot agent. The query language [Flux](https://docs.influxdata.com/flux) is used to query the data from InfluxDB.
- *Zipkin:* Zipkin is used to store and query all recorded traces.

You can access Grafana through http://localhost:3001 using `admin` as username and `demo` as password.
The traces can be viewed in Zipkin on http://localhost:9411.

### Prometheus and Jaeger Scenario
In this scenario the following components are preconfigured and used for monitoring:

[![Demo scenario using Prometheus and Jaeger](assets/demo-landscape-prometheus-jaeger.png)](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&collector=prometheus-server%2Cjaeger-collector&dashboarding=grafana&visualization=jaeger-query)

- *inspectIT Ocelot agent:* Instruments all the target demo application components.
- *Prometheus Server:* Gathers metrics exposed by the agent.
- *Grafana:* Provides predefined example Dashboards visualizing the metrics collected by the inspectIT Ocelot agent.
- *Jaeger:* Jaeger is used to store and query all recorded traces.

You can access Grafana through http://localhost:3001 using `admin` as username and `demo` as password.
The traces can be viewed in Jaeger on http://localhost:16686.

Prometheus can be accessed through http://localhost:9090.

### Demo Grafana Dashboards
The demo scenarios include the following predefined Grafana Dashboards:

| Name + Grafana Marketplace | Description | Screenshot |
| -------------- | ------- | -------- |
| Service Graph [[InfluxDB]](https://grafana.com/dashboards/10142) [[Prometheus]](https://grafana.com/dashboards/10139) | Shows a graph of all instrumented and external services and their interaction. All flows are derived based on live metrics. | ![](assets/demo-dashboard-servicegraph_small.png) |
| HTTP Metrics [[InfluxDB]](https://grafana.com/dashboards/10141) [[Prometheus]](https://grafana.com/dashboards/10138) | Shows statistics of incoming and outgoing HTTP requests for each instrumented service. | ![](assets/demo-dashboard-http_small.png) |
| System Metrics [[InfluxDB]](https://grafana.com/dashboards/9601) [[Prometheus]](https://grafana.com/dashboards/9599) | Shows system metrics, such as system CPU utilization, load average and disk usage. | ![](assets/demo-dashboard-system_small.png) |
| JVM Metrics [[InfluxDB]](https://grafana.com/dashboards/9600) [[Prometheus]](https://grafana.com/dashboards/9598) | Shows JVM metrics related to JVM CPU usage, Memory (Heap and Non-Heap) and Garbage Collection. | ![](assets/demo-dashboard-jvm_small.png) |
| Self Monitoring [[InfluxDB]](https://grafana.com/dashboards/10143) [[Prometheus]](https://grafana.com/dashboards/10140) | Shows the instrumentation state and progress based on [self monitoring metrics](metrics/self-monitoring.md). | ![](assets/demo-dashboard-selfmonitoring_small.png) |


## Changing Agent Configurations

In all demo scenarios the inspectIT Ocelot agents already have their service names and used ports as well as a basic instrumentation set up.
However, if you want to customize any other configuration option you can provide custom configuration files.

The demo starts the following services, of which each is instrumented with an inspectIT Ocelot Agent:

- *config-server*
- *discovery-server*
- *customers-service*
- *visits-service*
- *vets-service*
- *api-gateway*

For each service you can put your own agent configuration files in the
correspondingly named subfolders in ```inspectit-ocelot-demo/agentconfig/```.
For example, if you want to change the configuration of the inspectIT Ocelot
agent attached to the *vets-service*, you can put a YAML-file into ```inspectit-ocelot-demo/agentconfig/vets-service```.

Note that it is not required to restart the demo! The agents listen for updates of the corresponding directories and reconfigure themselves when required.
