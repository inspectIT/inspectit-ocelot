---
id: privacy
title: Data Privacy
---

InspectIT Ocelot offers you a mechanism to protect your privacy when collecting traces and to prevent the collection of 
sensitive data. This ensures that no sensitive data will leave your application.

## Data Obfuscation

The Ocelot agent configuration allows specification of the data obfuscation rules which will be applied when:

1. Collecting span attributes

This way you can globally protect against collecting and storing user sensitive information.
This can help to implement requirements regarding the strict GDPR rules.

You can define one or more obfuscation patterns as show in the following configuration:

```yaml
inspectit:
  privacy:
    obfuscation:
      enabled: true
      patterns:
        - pattern: 'username'
        - pattern: '[a-z]+'
```

:::warning Obfuscation Deactivation
Note that obfuscation is enabled by default. However, setting `inspectit.privacy.obfuscation.enabled` to `false` will **deactivate obfuscation completely**, no matter if any patterns are defined or not.
:::

Each pattern entry can be customized by following properties: 

| Property           | Default | Description                                                                                    |
|--------------------|---------|------------------------------------------------------------------------------------------------|
| `pattern`          | -       | The regular expression used to match data for obfuscation.                                     |
| `case-insensitive` | `true`  | Denoting whether this pattern should be complied as case insensitive.                          |
| `check-key`        | `true`  | Denoting whether this pattern should be tested against the key of the collected attribute.     |
| `check-data`       | `false` | Denoting whether this pattern should be tested against the actual value of the collected data. |
| `replace-regex`    | -       | The regular expression used to obfuscate specific parts of the data value                      |

## Examples

Let's check few examples in order to clarify how pattern obfuscation works in Ocelot.

### Example 1

This configuration masks all attribute values where the corresponding attribute key contains `address`, ignoring capitalization.

```yaml
inspectit:
  privacy:
    obfuscation:
      patterns:
        - pattern: '.*address.*'  # contains 'address' 
```
The following table shows the effect of the previous obfuscation configuration on collected span attributes:

| Collected Attributes                 | Resulting Attributes          |
|--------------------------------------|-------------------------------|
| `"companyAddress": "Sunny Road 13B"` | `"companyAddress": "***"`     |
| `"address": "Milkey Road 17"`        | `"address": "***"`            |
| `"action0": "address update"`        | `"action1": "address update"` |

### Example 2

This configuration masks all attribute values where the corresponding attribute key or the value itself contain `address`. In this example, the pattern is case-sensitive.

```yaml
inspectit:
  privacy:
    obfuscation:
      patterns:
        - pattern: '.*address.*'    
          case-insensitive: false   # ignore capitalization
          check-key: true           # this is true by default
          check-data: true          # also check the attributes value
```

The following table shows the effect of the previous obfuscation configuration on collected span attributes:

| Collected Attributes                 | Resulting Attributes                 |
|--------------------------------------|--------------------------------------|
| `"companyAddress": "Sunny Road 13B"` | `"companyAddress": "Sunny Road 13B"` |
| `"address": "Milkey Road 17"`        | `"address": "***"`                   |
| `"action0": "address update"`        | `"action1": "***"`                   |

### Example 3

This configuration masks all attribute values where the corresponding attribute key or the value itself contain `address`. In this example, the pattern is case-sensitive.

```yaml
inspectit:
  privacy:
    obfuscation:
      patterns:
        - pattern: '.*address.*'
          case-insensitive: true  
          check-key: true           
          check-data: true          
          replace-regex: '[0-9]+'   # replace any numbers
```

The following table shows the effect of the previous obfuscation configuration on collected span attributes:

| Collected Attributes                 | Resulting Attributes                  |
|--------------------------------------|---------------------------------------|
| `"companyAddress": "Sunny Road 13B"` | `"companyAddress": "Sunny Road ***B"` |
| `"address": "Milkey Road 17"`        | `"address": "Milkey Road ***"`        |
| `"action0": "address update"`        | `"action1": "address update"`         |
