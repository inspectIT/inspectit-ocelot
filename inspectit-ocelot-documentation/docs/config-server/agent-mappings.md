---
id: agent-mappings
title: Agent Mappings
---

Agent mappings are used to determine which agent receives which configuration. Here, individual files or specific folders can be defined, which serve as the basis for the resulting configuration. Furthermore, you can specify which branch (`WORKSPACE` or `LIVE`) the mapping should use to obtain the configuration files.

It's important to note that the first matching agent mapping will be used to determine which configuration is shipped to an agent.
Additional agent mappings which may also match the attributes list sent by an agent will be ignored.
See section [HTTP-based Configuration](configuration/external-configuration-sources.md#http-based-configuration) for information on how to specify which attributes will be sent by an agent.

An agent mapping consists of the following properties:

| Property | Default | Note |
| --- | --- | --- |
| `name` | - | The name of the agent mapping. |
| `sources` | `[]` | A list of paths. All configuration files matching this list (Directories are resolved recursively) will be merged together and returned to matching agents. |
| `attributes` | `{}` | A map of attributes. This map is used to determine whether an agent will match this agent mapping, thus, get its configuration. |
| `sourceBranch` | `WORKSPACE` | Defines which branch is used as source for the configuration files. Supported values are: `WORKSPACE`, `LIVE` |


The configuration server is shipped with a default agent mapping.
This default agent mapping maps each agent to each configuration file of the `workspace` branch.

```YAML
- name: "Default Mapping"
  sources:
  - "/"
  attributes:
    service: ".*"
  sourceBranch: "WORKSPACE"
```

## Example Agent Mappings

### Example 1

The following agent mapping will deliver all promoted configuration files located in the directory `/common` and `/europe/west` to all agents whose `region` attributes is equal to `eu-west`.

```YAML
- name: "Example Mapping 1"
  sources:
  - "/common"
  - "/europe/west"
  attributes:
    service: ".*"
    region: "eu-west"
  sourceBranch: "LIVE"
```

### Example 2

The following agent mapping will deliver all configuration files located in the directory `/common` and the specific file `/special/customer-service.yml` of the `workspace` branch to all agents whose `service` attributes is equal to `customer-service`.

```YAML
- name: "Example Mapping 2"
  sources:
  - "/common"
  - "/special/customer-service.yml"
  attributes:
    service: "customer-service"
  sourceBranch: "WORKSPACE"
```