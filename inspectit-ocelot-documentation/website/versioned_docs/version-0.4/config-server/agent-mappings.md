---
id: version-0.4-agent-mappings
title: Agent Mappings
original_id: agent-mappings
---

Agent mappings are used to determine which agent receives which configuration.
It's important to note that the first matching agent mapping will be used to determine which configuration is shipped to an agent.
Additional agent mappings which may also match the attributes list sent by an agent will be ignored.
See section [HTTP-based Configuration](configuration/external-configuration-sources.md#http-based-configuration) for infromation on how to specify which attributes will be send by an agent.

An agent mapping consists of the following properties:

| Property | Note |
| --- | --- |
| `name` | The name of the agent mapping. |
| `sources` | A list of paths. All configuration files matching this list (Directories are resolved recursively) will be merged together and returned to matching agents. |
| `attributes` | A map of attributes. This map is used to determine whether an agent will match this agent mapping, thus, get its configuration. |

The configuration server is shipped with a default agent mapping.
This default agent mapping maps each agent to each configuration file.

```YAML
- name: "Default Mapping"
  sources:
  - "/"
  attributes:
    service: ".*"
```

## Example Agent Mappings

### Example 1

The following agent mapping will deliver all configuration files located in the directory `/common` and `/europe/west` to all agents whose `region` attributes is equal to `eu-west`.

```YAML
- name: "Example Mapping 1"
  sources:
  - "/common"
  - "/europe/west"
  attributes:
    service: ".*"
    region: "eu-west"
```

### Example 2

The following agent mapping will deliver all configuration files located in the directory `/common` and the specific file `/special/customer-service.yml` to all agents whose `service` attributes is equal to `customer-service`.

```YAML
- name: "Example Mapping 2"
  sources:
  - "/common"
  - "/special/customer-service.yml"
  attributes:
    service: "customer-service"
```