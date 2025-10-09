---
id: tag-guard
title: Tag-Guard
---

Since version `2.6.0` it is possible to limit the amount of attribute/tag values of metrics.
This can be useful for controlling the amount of attribute values, which will be written to your time series database
(e.g. InfluxDB or Prometheus). A high amount of unique attribute values for a metric will result in a high cardinality, 
which in turn might lead to performance or memory issues in your time series database.

The recorded attribute values for each metric of an agent will be stored inside a local JSON file. This file serves
as the tag-guard-database and helps to check, if attribute values exceeded their limit.

## Configuration

You can set the Tag-Guard configuration in `inspectit.metrics.tag-guard`.

| Property                              | Default                                                                        | Description                                                                                                                  |
|---------------------------------------|--------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `.enabled`                            | `true`                                                                         | Only when the tag-guard is enabled, the attribute value limits will be checked.                                              |
| `.database-file`                      | `${inspectit.env.agent-dir}/${inspectit.service-name}/tag-guard-database.json` | The location of the JSON file with all recorded attribute values.                                                            |
| `.schedule-delay`                     | `30s`                                                                          | The delay for the scheduler, which will regularly compare the tag-guard-database with the configured attribute value limits. |
| `.overflow-replacement`               | `TAG_LIMIT_EXCEEDED`                                                           | After exceeding it's configured attribute value limit, every attribute will use this overflow replacement as value.          |
| `.max-values-per-attribute`           | `1000`                                                                         | The global attribute value limit for every metric.                                                                           |
| `.max-values-per-attribute-by-metric` | `{}`                                                                           | A map with metric names as key and their specific attribute value limit as value.                                            |

There are three ways to define are attribute value limit for metrics. They are prioritized in the following order:

1. Inside a metric definition for a particular metric
2. Globally for specific metrics via `may-values-per-attribute-by-metric`
3. Globally for all metrics via `max-values-per-attribute`

> **Note**: Since version `3.0.0` there is also a `cardinality-limit` option within metric [views](metrics/custom-metrics.md#views).
If a view exceeds this `cardinality-limit`, OpenTelemetry will drop new incoming attributes.
In future releases we might integrate this property into Tag-Guard.

This means that an attribute value limit inside a metric definition will overwrite all other attribute value limits 
for the particular metric. A configured attribute value limit in `max-values-per-attribute-by-metric` will only overwrite the
globally set attribute value limit in `max-values-per-attribute` for the particular metric, but not a configured attribute value limit
inside the metric definition. Let's look at an example:

```yaml
inspectit:
  metrics:
    tag-guard:
      max-values-per-attribute: 1000
      max-values-per-attribute-by-metric:
        my_metric: 200
```

In this configuration the global attribute value limit is set to 1000, which means that every metric can only record 1000 unique
attribute values for each attribute. However, this does not apply to the metric `my_metric`, because the global attribute value limit is 
overwritten by `max-values-per-attribute-by-metric` with 200. Thus, the metric `my_metric` can only record a maximum of 200 unique
attribute values for each attribute.

Now, let's add another configuration:

```yaml
inspectit:
  metrics:
    definitions:
      'my_metric':
        tag-guard: 100
```

This metric definition will overwrite the attribute value limit specified in `max-values-per-attribute-by-metric` for the metric `my_metric`,
resulting in an attribute value limit of 100. Every other metric still uses the globally configured attribute value
limit of 1000.


## Agent Health Monitoring

If the attribute value for a specific agent is exceeded, the Tag-Guard scheduler will detect an overflow and change
the agent health to `ERROR`.
Additionally, an agent health incident will be created, mentioning which attribute-key has exceeded its attribute value limit.
In the [Agent Status Table View](../config-server/status-table-view.md) of the Configuration-Server you can click on the
health state icon of a particular agent to view its last agent health incidents. You can set the amount of buffered incidents
with `inspectit.self-monitoring.agent-health.incident-buffer-size`. A list of incidents could look like this:

![List of agent health incidents](assets/agent-health-incidents.png)


### How To Fix A Value Overflow

If an attribute value limit was exceeded, there are two options to resolve the agent health `ERROR`. 

The **first option** would be to increase the attribute value limit. Probably the limit has been estimated too low and thus has 
to be increased. After increasing the attribute value limit, the tag-guard-database scheduler will resolve the `ERROR`.

The **second option** would be to adjust your configuration or application so the attribute value limit should not be exceeded anymore.
After the adjustment, you will have to "reset" the recorded attribute values in the tag-guard-database to resolve the `ERROR`. 
One way to reset the tag-guard-database is to delete the local JSON file. However, this will delete all recorded attribute values 
and might not be the preferred solution. <br>
A more preferable solution would be to only reset the attribute values for a specific attribute of a metric, 
which has exceeded its attribute value limit.
To do this, you could use the _**jq command-line JSON processor**_, which has to be installed on your system manually. 
For example, you could use the following command, if you would like to delete all recorded attribute values for the attribute _my_attr_ inside the metric _my_metric_:

- Unix: `jq '.my_metric.my_attr = []' tag-guard-database.json > temp.json && mv temp.json tag-guard-database.json`
- Windows: `jq ".my_metric.my_attr = []" tag-guard-database.json > temp.json && move temp.json tag-guard-database.json`

In future versions of inspectIT there might be an option to reset specific attribute values directly in the Configuration-Server UI.
