---
id: docker-examples
title: inspectIT Ocelot Demo Application
sidebar_label: Demo Application
---

## inspectIT Ocelot Demo

If you would like to see inspectIT Ocelot in action with a demo application, you can use our docker compose example.
We use the distributed version of the [Spring PetClinic sample application](https://github.com/spring-petclinic/spring-petclinic-microservices) as the target application in our demo.
To demonstrate the flexibility of the OpenCensus based inspectIT agent, we provide different demo scenarios covering different monitoring and APM components.

All the demo scenarios are fully configured with predefined dashboards, *so you can get started in 5 minutes*.

### Demo #1 - Prometheus, Grafana and Jaeger

Uses Prometheus Server for metrics gathering and storage, Grafana for Dashboards.
Traces are exported to Jaeger.
[See section below for detailed information](#prometheus-and-jaeger-scenario).

* File: `docker-compose-prometheus-jaeger.yml`
* [OpenAPM Landscape](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&collector=prometheus-server,jaeger-collector,inspectit-oce-eum-server&visualization=jaeger-query&dashboarding=grafana&usedges=inspectit-ocelot-agent:prometheus-server,inspectit-ocelot-agent:jaeger-collector,jaeger-query:grafana,inspectit-oce-eum-server:prometheus-server,inspectit-oce-eum-server:jaeger-collector&showCommercial=false&showFormats=false)

![Demo scenario using Prometheus and Jaeger](assets/demo-landscape-prometheus-jaeger.png)

### Demo #2 - InfluxDB and Jaeger

Uses InfluxDB for metrics storage and Grafana for Dashboards.
Traces are exported to Jaeger.
[See section below for detailed information](#influxdb-and-jaeger-scenario).

* File: `docker-compose-influxdb-jaeger.yml`
* [OpenAPM Landscape](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&collector=jaeger-collector,inspectit-oce-eum-server&storage=influx-db&visualization=jaeger-query&dashboarding=grafana&alerting=grafana&usedges=jaeger-query:grafana,inspectit-ocelot-agent:influx-db,inspectit-ocelot-agent:jaeger-collector,inspectit-ocelot-agent:influxdata-influxdb,inspectit-oce-eum-server:influxdata-influxdb,inspectit-oce-eum-server:jaeger-collector&showCommercial=false&showFormats=false)

![Demo scenario using InfluxDB and Zipkin](assets/demo-landscape-influxdb-jaeger.png)

### Demo #3 - InfluxDB and Zipkin

Uses InfluxDB for metrics storage and Grafana for Dashboards.
Traces are exported to Zipkin.
[See section below for detailed information](#influxdb-and-zipkin-scenario).

* File: `docker-compose-influxdb-zipkin.yml`
* [OpenAPM Landscape](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&collector=zipkin-server,inspectit-oce-eum-server&storage=influx-db&visualization=zipkin-server&dashboarding=grafana&alerting=grafana&usedges=inspectit-ocelot-agent:influx-db,inspectit-ocelot-agent:zipkin-server,inspectit-ocelot-agent:influxdata-influxdb,inspectit-oce-eum-server:influxdata-influxdb,inspectit-oce-eum-server:zipkin-server&showCommercial=false&showFormats=false)

![Demo scenario using InfluxDB and Zipkin](assets/demo-landscape-influxdb-zipkin.png)

### Demo #4 - Wavefront and Zipkin

Sends Zipkin traces to Wavefront through a Wavefront proxy running as a Docker container. Collects metrics via Telegraf and sends them to Wavefront.
[See section below for detailed information](#wavefront-demo-scenario).

* File: `docker-compose-wavefront-zipkin.yml`

:::note 
Before running the demo, you must add the Wavefront host and API token to the ```.env``` file in the ```inspectit-ocelot-demo```. Please add the following lines and change the cluster and token according to your Wavefront instance.
```
WAVEFRONT_URL=https://<your cluster>.wavefront.com/api
WAVEFRONT_TOKEN=<API token obtained from Wavefront>
```
:::

## Launching the Demo

:::warning
Currently, the demo is only available as Docker images for the AMD64 architecture.
On ARM platforms there may be problems with the execution.
:::

*Pre-requisites:* To launch the demo, [Docker](https://www.docker.com/) needs to be installed on your system.
If you are using Docker Desktop or running Docker in a virtual machine, ensure that Docker has at least 4GB main memory assigned.

Either [download](https://github.com/inspectIT/inspectit-ocelot-demo/archive/refs/heads/main.zip) or clone the [inspectIT Ocelot-Demo GitHub repository](https://github.com/inspectIT/inspectit-ocelot-demo).

Change into the `spring-petclinic-docker-demo/` directory and execute the following command to launch the desired demo scenario (replace [SCENARIO_POSTFIX] with the postfix of the scenario you would like to launch):

```bash
$ docker-compose -f docker-compose-[SCENARIO_POSTFIX].yml up
```

This will start all the Docker containers required for the corresponding demo scenario, including the PetClinic demo application.

You can access the demo application (PetClinic) under http://localhost:8080.
Details on accessing monitoring infrastructure components are listed below, depending on the selected demo scenario.


## Demo Scenarios

> In all scenarios you can use `admin` as username and `demo` as password for accessing Grafana and the inspectIT Ocelot Configuration Server.

### Prometheus and Jaeger Scenario
In this scenario the following components are preconfigured and used for monitoring:

[![Demo scenario using Prometheus and Jaeger](assets/demo-landscape-prometheus-jaeger.png)](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&collector=prometheus-server%2Cjaeger-collector&dashboarding=grafana&visualization=jaeger-query)

- *inspectIT Ocelot agent:* Instruments all the target demo application components.
- *inspectIT Ocelot EUM server:* Records the user's behaviour or actions while using the demo application.
- *Prometheus Server:* Gathers metrics exposed by the agent.
- *Grafana:* Provides predefined example Dashboards visualizing the metrics collected by the inspectIT Ocelot agent.
- *Jaeger:* Jaeger is used to store and query all recorded traces.
You can access Grafana through http://localhost:3000 and the configuration server via http://localhost:8090.
The traces can be viewed in Jaeger on http://localhost:16686.

Prometheus can be accessed through http://localhost:9090.

:::note
Currently the EUM-Server dashboards are only supported for the InfluxDB demos. You may use the `Explore` view in Grafana to view the EUM server metrics.
:::

### InfluxDB and Jaeger Scenario
In this scenario the following components are preconfigured and used for monitoring:

[![Demo scenario using InfluxDB and Jaeger](assets/demo-landscape-influxdb-jaeger.png)](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&collector=jaeger-collector&storage=influx-db&visualization=jaeger-query&dashboarding=grafana&alerting=grafana&showCommercial=false&showFormats=false)

- *inspectIT Ocelot agent:* Instruments all the target demo application components.
- *inspectIT Ocelot EUM server:* Records the user's behaviour or actions while using the demo application.
- *InfluxDB:* Stores metric data exported by OpenCensus as time series.
- *Grafana:* Provides predefined example Dashboards visualizing the metrics collected by the inspectIT Ocelot agent. The query language [InfluxQL](https://docs.influxdata.com/influxdb/v1.8/query_language/) is used to query the data from InfluxDB.
- *Jaeger:* Jaeger is used to store and query all recorded traces.

You can access Grafana through http://localhost:3000 and the configuration server via http://localhost:8090.
The traces can be viewed in Jaeger on http://localhost:16686.


### InfluxDB and Zipkin Scenario
In this scenario the following components are preconfigured and used for monitoring:

[![Demo scenario using InfluxDB and Zipkin](assets/demo-landscape-influxdb-zipkin.png)](https://openapm.io/landscape?agent=inspectit-ocelot-agent&instrumentation-lib=opencensus&storage=influx-db&dashboarding=grafana&alerting=grafana&collector=zipkin-server&visualization=zipkin-server)

- *inspectIT Ocelot agent:* Instruments all the target demo application components.
- *inspectIT Ocelot EUM server:* Records the user's behaviour or actions while using the demo application.
- *InfluxDB:* Stores metric data exported by OpenCensus as time series.
- *Grafana:* Provides predefined example Dashboards visualizing the metrics collected by the inspectIT Ocelot agent. The query language [InfluxQL](https://docs.influxdata.com/influxdb/v1.8/query_language/) is used to query the data from InfluxDB.
- *Zipkin:* Zipkin is used to store and query all recorded traces.

You can access Grafana through http://localhost:3000 and the configuration server via http://localhost:8090.
The traces can be viewed in Zipkin on http://localhost:9411.

:::note
Currently, only the Beacons Dashboard is working for the InfluxDB and Zipkin scenario.
:::

### Wavefront Demo Scenario

Wavefront is a SaaS-based monitoring and tracing solution. In this demo, we are running a local proxy as a Docker container that is responsible for receiving Zipkin traces and sending them to Wavefront. We're also using Telegraf to poll the Prometheus endpoints on the monitored services and send that data to Wavefront through the proxy.

- *inspectIT Ocelot agent:* Instruments all the target demo application components.
- *Telegraf:* Polls the Prometheus metric endpoints on the services.
- *Wavefront Proxy:* Receives Zipkin traces and metrics (via Telegraf), aggregates, secures and compresses them before sending them to Wavefront.

A sandbox instance of Wavefront can be obtained at https://www.wavefront.com/sign-up/.

The inspectIT Ocelot Configuration Server can be accessed via http://localhost:8090.

## Demo Grafana Dashboards
The InfluxDB and Prometheus demo scenarios include the following predefined Grafana Dashboards:

### Agent
| Name + Grafana Marketplace | Description | Screenshot|
| -------------- | ------- | -------- |
| Service Graph [[InfluxDB]](https://grafana.com/dashboards/10142) [[Prometheus]](https://grafana.com/dashboards/10139) | Shows a graph of all instrumented and external services and their interaction. All flows are derived based on live metrics. |[![](assets/demo-dashboard-servicegraph_small.png)](assets/demo-dashboard-servicegraph.png)|
| HTTP Metrics [[InfluxDB]](https://grafana.com/dashboards/10141) [[Prometheus]](https://grafana.com/dashboards/10138) | Shows statistics of incoming and outgoing HTTP requests for each instrumented service. | [![](assets/demo-dashboard-http_small.png)](assets/demo-dashboard-http.png)|
| System Metrics [[InfluxDB]](https://grafana.com/dashboards/9601) [[Prometheus]](https://grafana.com/dashboards/9599) | Shows system metrics, such as system CPU utilization, load average and disk usage. | [![](assets/demo-dashboard-system_small.png)](assets/demo-dashboard-system.png)|
| JVM Metrics [[InfluxDB]](https://grafana.com/dashboards/9600) [[Prometheus]](https://grafana.com/dashboards/9598) | Shows JVM metrics related to JVM CPU usage, Memory (Heap and Non-Heap) and Garbage Collection. | [![](assets/demo-dashboard-jvm_small.png)](assets/demo-dashboard-jvm.png)|
| Self Monitoring [[InfluxDB]](https://grafana.com/dashboards/10143) [[Prometheus]](https://grafana.com/dashboards/10140) | Shows the instrumentation state and progress based on [self monitoring metrics](metrics/self-monitoring.md). | [![](assets/demo-dashboard-selfmonitoring_small.png)](assets/demo-dashboard-selfmonitoring.png)|

### End User Monitoring
| Name | Description | Screenshot|
| ---------- | ------- | -------- |
| Beacons  | Shows metrics for the number of processed beacons and their average processing times | [![](assets/demo-dashboard-beacons_small.png)](assets/demo-dashboard-beacons.png)|
| Trace Controller | Shows metrics for the number of processed traces, the number of spans and the average processing times for the traces| [![](assets/demo-dashboard-trace-controller_small.png)](assets/demo-dashboard-trace-controller.png)|

## Changing Agent Configurations

In all demo scenarios the inspectIT Ocelot agents already have their service names and used ports as well as a basic instrumentation set up.
Each scenario uses the [inspectIT Ocelot Configuration Server](../config-server/overview.md) for managing and providing the configuration files to the agents.
The web UI of the configuration server can be accessed via [localhost:8090](http://localhost:8090).

The demo starts the following services, of which each is instrumented with an inspectIT Ocelot Agent:

- *config-server*
- *discovery-server*
- *customers-service*
- *visits-service*
- *vets-service*
- *api-gateway*


## Load Test

All demo scenarios launch with a load test written in [artillery](https://artillery.io/) that simulates user behavior. For 10 minutes approximately every 3 seconds a new virtual user is created which either looks up a random owner from the database or edits the pet type of an existing animal. Therefore no real user interaction with the PetClinic is needed.
