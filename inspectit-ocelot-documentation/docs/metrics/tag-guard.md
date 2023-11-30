---
id: tag-guard
title: Tag-Guard
---

Since version `2.6.0` it is possible to limit the amount of tag values of metrics.
This can be useful for controlling the amount of tag values, which will be written to your time series database
(e.g. InfluxDB or Prometheus). A high amount of unique tag values for a metric will result in a high cardinality, 
which in turn might lead to performance or memory issues in your time series database.

The recorded tag values for each measure of an agent will be stored inside a local JSON file. This file serves
as a tag-guard-database and helps to check, if tag values exceeded their limit.

### Configuring Tag-Guard

You can set the Tag-Guard configuration in `inspectit.metrics.tag-guard`.

| Property                         | Default                                                                        | Description                                                                                                            |
|----------------------------------|--------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `.enabled`                       | `true`                                                                         | Only when the tag-guard is enabled, the tag value limits will be checked.                                              
| `.database-file`                 | `${inspectit.env.agent-dir}/${inspectit.service-name}/tag-guard-database.json` | The location of the JSON file with all recorded tag values.                                                            |
| `.schedule-delay`                | `30s`                                                                          | The delay for the scheduler, which will regularly compare the tag-guard-database with the configured tag value limits. |
| `.overflow-replacement`          | `TAG_LIMIT_EXCEEDED`                                                           | After exceeding it's configured tag value limit, every tag will use this overflow replacement as value.                |
| `.max-values-per-tag`            | `1000`                                                                         | The global tag value limit for every measure.                                                                          |
| `.max-values-per-tag-by-measure` | `{}`                                                                           | A map with measure names as key and their specific tag value limit as value.                                           |

There are three ways to define a tag value limit for measures. They are prioritized in the following order:

1. Inside a metric definition for a particular measure
2. Globally for specific measures via `may-values-per-tag-by-measure`
3. Globally for all measures via `max-values-per-tag`

This means that a tag value limit inside a metric definition will overwrite all other tag value limits 
for the particular metric. A configured tag value limit in `max-values-per-tag-by-measure` will only overwrite the
globally set tag value limit in `max-values-per-tag` for the particular measure, but not a configured tag value limit
inside the metric definition. Let's look at an example:

```yaml
inspectit:
  metrics:
    tag-guard:
      max-values-per-tag: 1000
      max-values-per-tag-by-measure:
        my_metric: 200
```

In this configuration the global tag value limit is set to 1000, which means that every measure can only record 1000 unique
tag values for each tag. However, this does not apply to the measure `my_metric`, because the global tag value limit is 
overwritten by `max-values-per-tag-by-measure` with 200. Thus, the measure `my_metric` can only record a maximum of 200 unique 
tag values for each tag.

Now, let's add another configuration:

```yaml
inspectit:
  metrics:
    definitions:
      'my_metric':
        tag-guard: 100
```

This metric definition will overwrite the tag value limit specified in `max-values-per-tag-by-measure` for the measure `my_metric`,
resulting in a tag value limit of 100. Every other measure still uses the globally configured tag value
limit of 1000.


### Agent Health Monitoring

If the tag value for a specific agent is exceeded, the Tag-Guard scheduler will detect an overflow and change
the agent health to `ERROR`.
Additionally, an agent health incident will be created, mentioning which tag-key has exceeded its tag value limit.
In the [Agent Status Table View](../config-server/status-table-view.md) of the Configuration-Server you can click on the
health state icon of a particular agent to view its last agent health incidents. You can set the amount of buffered incidents
with `inspectit.self-monitoring.agent-health.incident-buffer-size`. A list of incidents could look like this:

![List of agent health incidents](assets/agent-health-incidents.png)


### How To Fix A Tag Value Overflow

If a tag value limit was exceeded, there are two options to resolve the agent health `ERROR`. 

The **first option** would be to increase the tag value limit. Probably the limit has been estimated too low and thus has 
to be increased. After increasing the tag value limit, the tag-guard-database scheduler will resolve the `ERROR`.

The **second option** would be to adjust your configuration or application so the tag value limit should not be exceeded anymore.
After the adjustment, you will have to "reset" the recorded tag values in the tag-guard-database to resolve the `ERROR`. 
One way to reset the tag-guard-database is to delete the local JSON file. However, this will delete all recorded tag values 
and might not be the preferred solution. <br>
A more preferable solution would be to only reset the tag values for a specific tag of a measure, 
which has exceeded its tag value limit.
To do this, you could use the _**jq command-line JSON processor**_, which has to be installed on your system manually. 
For example, you could use the following command, if you would like to delete all recorded tag values for the tag _my_tag_ inside the measure _my_metric_:

- Unix: `jq '.my_metric.my_tag = []' tag-guard-database.json > temp.json && mv temp.json tag-guard-database.json`
- Windows: `jq ".my_metric.my_tag = []" tag-guard-database.json > temp.json && move temp.json tag-guard-database.json`

In future versions of inspectIT there might be an option to reset specific tag values directly in the Configuration-Server UI.
