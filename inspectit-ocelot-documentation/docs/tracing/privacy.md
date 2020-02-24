---
id: privacy
title: Data Privacy
---

InspectIT Ocelot provides you a mechanism to protect the data privacy when collecting traces. 

## Data Obfuscation

The Ocelot agent configuration allows specification of the data obfuscation rules which will be applied when:

1. Collecting span attributes 

This way you can globally protect against collecting and storing user sensitive information.
This adheres to the strict GDPR rules introduced by the EU starting from 25th May 2018.

You can define one or more obfuscation patterns as show in the following configuration:

```yaml
inspectit:
  privacy:
    obfuscation:
      enabled: true
      patterns:
        - pattern: username
        - pattern: [a-z]+
```

> Note that obfuscation is enabled by default. However, setting `inspectit.privacy.obfuscation.enabled` to `false` will deactivate obfuscation completely, no matter if any patterns are defined or not.

Each pattern entry can be customized by following properties: 

|Property |Default| Description
|---|---|---|
|`pattern`|-|The regular expression used to match data for obfuscation.
|`case-insensitive`|`true`|The boolean denoting if this pattern should be complied as case insensitive.
|`check-key`|`true`|The boolean denoting if this pattern should be tested against the key of the collected attribute.
|`check-data`|`false`|The boolean denoting if this pattern should be tested against the actual value of the collected data.

### Examples

Let's check few examples in order to clarify how pattern obfuscation works in Ocelot.

#### Example 1

```yaml
inspectit:
  privacy:
    obfuscation:
      patterns:
        - pattern: address
```

Here are the results of collected tracing attributes in case of the configuration used above:

|Collected attribute key -> value|Stored attribute key -> value
|---|---|
|`companyAddress` -> `Sunny Road 13B`|`companyAddress` -> `***` (case insensitive by default)
|`address` -> `Milkey Road 17`|`address` -> `***`
|`action` -> `address update`|`action` -> `address update`

#### Example 2

```yaml
inspectit:
  privacy:
    obfuscation:
      patterns:
        - pattern: address
          case-insensitive: false
          check-key: true
          check-data: true
```

Here are the results of collected tracing attributes in case of the configuration used above:

|Collected attribute key -> value|Stored attribute key -> value
|---|---|
|`companyAddress` -> `Sunny Road 13B`|`companyAddress` -> `Sunny Road 13B` (case insensitive explicitly set to `false`)
|`address` -> `Milkey Road 17`|`address` -> `***`
|`action` -> `address update`|`action` -> `***` (`check-data` explicitly set to `true` meaning that patterns also checks attribute values)

> For now, all obfuscated values are replaced with three stars `***`. This could be changed in future Ocelot releases in favor of a replace regex.