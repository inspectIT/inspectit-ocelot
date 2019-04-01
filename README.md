![inspectIT Ocelot ](https://inspectit.github.io/inspectit-ocelot/images/inspectit-oce.png)

# inspectIT Ocelot 

[![Build Status](https://circleci.com/gh/inspectIT/inspectit-ocelot.svg?style=svg)](https://circleci.com/gh/inspectIT/inspectit-ocelot)
[![Code Coverage](https://codecov.io/gh/inspectit/inspectit-ocelot/branch/master/graph/badge.svg)](https://codecov.io/gh/inspectIT/inspectit-ocelot)

inspectIT Ocelot is a zero-configuration Java agent for dynamically collecting application performance, tracing and behavior data based on the [OpenCensus library](https://opencensus.io/).
Despite the zero-configuration capability, it provides a powerful configuration feature which enables a full and in-depth customization of it.
In order to use inspectIT Ocelot, the source code of the target application does **not** have to be touched, changed or modified - even access to the actual source code is not required.
It automatically injects all required components and executes the necessary actions by itself.

But wait - isn't there already an inspecIT existing?
Yes, it is! Please [read this](http://www.inspectit.rocks/public-announcement-inspectit-future-plans-and-road-map/) in order to understand why we are developing a new and improved version of it!
Compared to the former [inspectIT](https://inspectit.rocks/), the OC edition follows the approach of focusing on compatibility and interaction with other awesome open source tools.
For this purpose we provide interfaces and data exporters for tools and frameworks like [Prometheus](https://prometheus.io/), [Zipkin](https://zipkin.io/) or [Jaeger](https://www.jaegertracing.io/).
This allows us to use and interact with well-known and established tools like [Elasticsearch](https://www.elastic.co/products/elasticsearch), [InfluxDB](https://www.influxdata.com/) or [Grafana](https://grafana.com/), reducing the amount of components which have to be introduced into an existing infrastructure or which needs to be familiarized with.

## Collected Data

The inspectIT Ocelot Java agent collects a lot of different data. Currently among others the following data is collected:

* Metrics
  * CPU (usage and number of cores)
  * Disk Space (used, free and total)
  * Memory (used and available for various regions like heap or non-heap)
  * Threads (counts and states)
  * Garbage Collection (pause times and collection statistics)
  * Class Loading (loaded and unloaded counts)
  
 A full list of gathered data and metrics can be found in the [documentation](http://docs.inspectit.rocks/releases/latest/#_metrics).

## Demo

You want to see the inspectIT Ocelot Java agent in action?
No problem!
We've prepared a nice containerized demo to show what the agent is capable of.
The demo consists of three different scenarios, whereby we would like to emphasize the flexibility of the agent and therefore each scenario uses a different set of tools.
For example, Elasticsearch is used as data storage in one scenario and InfluxDB or Prometheus in the other.

All you have to be done is to checkout this repository, install Docker on your machine and spin one of the prepared scenarios up:

* `$ docker-compose -f inspectit-ocelot-demo/docker-compose-elastic.yml up`
* `$ docker-compose -f inspectit-ocelot-demo/docker-compose-influxdb.yml up`     
* `$ docker-compose -f inspectit-ocelot-demo/docker-compose-prometheus.yml up`

Check out the [documentation's demo section](http://docs.inspectit.rocks/releases/latest/#_demo_scenarios) for detailed information on each scenario.

## Installation

Getting started with the inspectIT Ocelot Java agent is very easy!

First of all, you have to download the Java agent.
You will find all released versions in the release section of this repository: https://github.com/inspectIT/inspectit-ocelot/releases

The best way to use the inspectIT Ocelot Java agent is to attach it to your Java application during startup.
This can be achieved using the `-javaagent` command-line option of your JVM and referencing the agent Jar:

    $ java -javaagent:"/path/to/inspectit-ocelot-agent.jar" -jar my-java-program.jar

#### Attaching the Agent to a Running JVM

We also support attaching the inspectIT Ocelot Java agent to an already running JVM.
In such a scenario the collection of metrics and traces will start from the point of the attachment.
Please read the [documentation](http://docs.inspectit.rocks/releases/latest/#_attaching_the_agent_to_an_already_started_jvm) how this can be achieved.

## Configuration

The inspectIT Ocelot Java agent offers a comprehensive configuration capability which allows you to customize practically all properties to your own needs.
In addition, the agent also supports **hot reloading** for its configuration which makes it possible to modify individual configuration settings during runtime without having to restart the application, which is usually the case.

The configuration hot reloading feature also allows you to start the agent in a kind of "standby state" with deactivated features and activate these at a later point in time.

Currently, the configuration values can be set using [environment variables, system properties](http://docs.inspectit.rocks/releases/latest/#_java_system_properties) or [configuration files](http://docs.inspectit.rocks/releases/latest/#_file_based_configuration).
This allows you to pass configuration values to the agents by, for example, using Puppet to set specific properties or using Ansible to roll out updated configuration files which then will be hot reloaded.
 
For detailed information about the configuration see the [related section of the documentation](http://docs.inspectit.rocks/releases/latest/#_configuration).

## Documentation

A detailed user documentation can be found at: http://docs.inspectit.rocks/

You need a documentation of an earlier version? No problem! All the released documentation can be found here: http://docs.inspectit.rocks/releases/

If you cannot wait for the next stable release and want to use an agent based on the current master branch, the corresponding documentation can be found here: http://docs.inspectit.rocks/master/  

## Contribution and Development

If you want to contribute anything to this awesome project, feel free to open a pull request or reach out to us!
A good starting point is the [CONRIBUTION.md](CONTRIBUTION.md).

If you need additional or in-depth information on the actual implementation of inspectIT Ocelot, check out the README files in the child projects of this repository. 

## FAQ

#### Is it pronounced inspect-"IT" or "it"?

It's like "inspect it" and not like IT in information technology. `/ɪnˈspektɪt/` :) 

#### Can I use the agent on an application using Java 6?

No. The Java agent is developed against Java 8, thus, the application which you want to attach the agent to have to be using a JVM running on Java 8 or higher.

#### Does the agent require a specific JVM in order to run properly?

No. The agent is compatible to and can be used with any kind of JVM (Oracle JVM, an OpenJDK, an IBM JVM, ...), as long as it supports the `javaagent` argument and provides Java 8 or higher.

#### My application is already using OpenCensus. Can I use the agent anyway?

Yes, you can use the inspectIT Ocelot Java agent if you already use the OpenCensus library.
However, there are a few points that need to be considered to ensure a smooth operation.
Please read the [corresponding section in the documentation](http://docs.inspectit.rocks/master/#_using_opencensus_library_with_inspectit_oce).

#### How can I start my application with the inspectIT Ocelot Java agent?

We have a [detailed section in the documentation](http://docs.inspectit.rocks/releases/latest/#_installation) about this topic.
The easiest way is to add the `javaagent` argument to your Java command like [described above](#installation).

#### My Java application does not start or it exits immediately when I use the agent.

In this case, check your Java command and ensure that you have not accidentally removed your application.
You'll most likely have a command like `java -javaagent:inspectit-ocelot-agent.jar` or `java -javaagent:inspectit-ocelot-agent.jar -version`. 
Starting Java without an application but the agent will work but does not make sense because the agent will initialize itself and then the JVM will properly shut down because it has no application to execute.
It's like executing just `java`.

## Behind the Scenes

inspectIT Ocelot is mainly driven by [NovaTec Consulting GmbH](https://www.novatec-gmbh.de/), a German consultancy firm that specializes in software performance.
