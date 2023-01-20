---
id: tracing-self-monitoring
title: Self-Monitoring
---

The inspectIT Ocelot Agent allows to trace itself to get detailed information about what the agent is doing (e.g. execution of method hooks and actions).

## Action Tracing

:::warning
Please note that this feature is intended **only for debug purposes** or to help you create configuration.
Depending on the size of the configuration used, this can result in a large amount of data being collected, so this feature **should not be used in production environments or during performance testing**.
:::

Using the *action tracing* feature, the agent will record method hooks and action calls including detailed information about the execution context where they are executed in.
These data are, for example, the arguments of an action, the return value of an action and the existing context data/variables. 

![Trace with enabled action tracing](assets/action-tracing.png)

Action tracing can be configured using the `inspectit.self-monitoring.action-tracing` property.

```YAML
inspectit:
  self-monitoring:
    action-tracing: ONLY_ENABLED 
```

The default value of this setting is `ONLY_ENABLED` and accepts the following options:

- `OFF` - Action tracing is disabled and no spans are created.
- `ONLY_ENABLED` - Only actions and method hooks of rules for which action tracing has been explicitly enabled are traced.
- `ALL_WITHOUT_DEFAULT` - Only actions and method hooks of rules which are not fagged as default rules are traced.
- `ALL_WITH_DEFAULT` - All method hooks and actions are traced.

### Enabling Action Tracing for Specific Rules

By default, action tracing is not enabled for any rule.
To trace the method hooks and actions of a specific rule, action tracing can be enabled for a rule by using their `enable-action-tracing` attribute.

```YAML
inspectit:
  instrumentation:
    rules:
      'r_example_rule':
        enable-action-tracing: true
```

Note that this setting **only has an effect** if the action tracing mode is set to `ONLY_ENABLED`.!