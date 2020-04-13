---
id: common-tags
title: Common Tags
---

inspectIT Ocelot provides a map of common tag keys and values that are used when recording all metrics.
This feature enables to "label" metrics collected with inspectIT Ocelot and to share common information about a process or an environment where the process runs.
The map can be [extended or overwritten by the user](#user-defined-tags) when this is required.

>The common tags can also be added as attributes in tracing spans.
 This behavior can be configured in the global [tracing settings](tracing/tracing.md#common-tags-as-attributes).

Tag providers are responsible for extracting information from a specific source and provides a map of key value pairs that are then combined into the common tag context.
Each provider specifies a priority and if the same tag keys are supplied by two providers, then the tag value from the provider with higher priority will be used.

inspectIT Ocelot currently supports the following tag providers:

|Provider |Tags provided |Supports run-time updates |Enabled by default |Priority
|---|---|---|---|---|
|[User Defined Tags](#user-defined-tags)|-|Yes|No|HIGH
|[Environment Tags](#environment-tags)|`service`, `host`, `host_address`|Yes|Yes|LOW

> Run-time updates currently only support changing of a tag value, but not adding or removing tags.

## User Defined Tags

User defined tags can be added to the common tag context by defining the `inspectit.tags.extra` property.

* Setting the user defined tags using properties:
   ```properties
   inspectit.tags.extra.region=us-west-1
   inspectit.tags.extra.stage=preprod
   ```

* Setting the user defined tags using YAML file:
   ```YAML
   inspectit:
     tags:
       extra:
         region: us-west-1
         stage: preprod
   ```

## Environment Tags

The environment tag provider extends the common tag context by supplying information about the service and the network host.
The service is resolved from the `inspectit.service-name` property, while the host information is extracted using `InetAddress.getLocalHost()`.

> On machines that have multiple network interfaces the `InetAddress.getLocalHost()` method might not provide desired values for host related tags.

|Property |Default| Description
|---|---|---|
|`inspectit.tags.providers.environment.enabled`|`true`|Enables or disables the provider.
|`inspectit.tags.providers.environment.resolve-host-name`|`true`|If `false`, the tag `host` containing the resolved host name will not be provided.
|`inspectit.tags.providers.environment.resolve-host-address`|`true`|If `false`, the tag `host_address` containing the resolved host IP address will not be provided.