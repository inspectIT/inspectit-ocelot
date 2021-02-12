---
id: version-1.7-scopes
title: Scopes
original_id: scopes
---

A Scope is one of the basic instrumentation configuration components.
It is important to get familiar with it in order to understand which methods are targeted by which instrumentation rule.
A single scope can be considered as a set of methods and is used by rules to determine which instrumentation should apply to which method.

Scopes are defined under the configuration key `inspectit.instrumentation.scopes` and will be defined as a map of key-value pairs:

```yaml
inspectit:
  instrumentation:
    scopes:
      's_my_scope':
        # SCOPE_DEFINITION
      's_another_scope':
        # SCOPE_DEFINITION
```

## Scope Definition

The definition of a scope contains the following five attributes:

|Attribute|Description
|---|---
|`interfaces`| A list of matcher which defines the required interfaces of the target method's class.
|`superclass`| A matcher which defines the required superclass of the target method's class.
|`type`| A matcher which defines the required type of the target method's class.
|`methods`| A list of matchers which defines the target methods itself.
|`advanced`| Advanced settings.
|`exclude`| A map of scopes. This map is used to exclude the specified scopes methods.

In order to determine which methods should be instrumented all classes are checked against the defined `interface`, `superclass` and `type` matchers.
If and only if a class matches on *all* matchers, each of their methods is checked against the defined method matchers in order to determine the target methods.
Finally, the methods of the excluded scopes are excluded from the matcher.
Thus, the process of determine whether the methods of a class `X` should be instrumented can be represented as:

1. Check if all interface matcher matches the interface of class `X` (if any is defined)
2. Check if the superclass matcher matches any superclass of class `X` (if defined)
3. Check if the type matcher matches the type of class `X` (if defined)
4. For each method: check if the method is matching *any* of the defined method matchers (if defined)
5. For each excluded scope: exlude the corresponding methods from the method matcher (if defined)

> Keep in mind that all of the type matchers have to match whereas only one of the method matchers have to match!

Based on the previous description a scope definition looks like the following code-snippet:

```yaml
# interfaces which have to be implemented
interfaces:
  - # TYPE_MATCHER_A
  - # TYPE_MATCHER_B
# the superclass which has to be extended
superclass:
  # TYPE_MATCHER_C
# matcher describing the class' type
type:
  # TYPE_MACHER_D
# the targeted method - each method which matches at least one of the defined matchers will be instrumented
methods:
  - # METHOD_MATCHER_A
  - # METHOD_MATCHER_B
# the scopes which have to be excluded
exclude:
  # SCOPE_A: true
```

## Type Matcher

As shown in the previous code-snippet, the scope definition contains multiple type matchers which are used to match interfaces, superclasses and class types itself.

A type matcher consists of the following attributes:

|Attribute|Default|Description
|---|---|---|
|`name`| -| The name or pattern which is used to match against the fully qualified class or interface name.
|`matcher-mode`| `EQUALS_FULLY`| The matching mode. Possible values: `EQUALS_FULLY`, `MATCHES` (see [String.match](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#matches-java.lang.String)), `STARTS_WITH`, `STARTS_WITH_IGNORE_CASE`, `CONTAINS`, `CONTAINS_IGNORE_CASE`, `ENDS_WITH`, `ENDS_WITH_IGNORE_CASE`
|`annotations`|-| A list of matchers used for matching annotations. Each annotation matcher consists of a `name` and `matcher-mode` which are equivalent to the ones above.

The following example will match against a type which is exactly named `java.util.AbstractList` and is annotated with the annotation `any.Annotation`.

```yaml
name: 'java.util.AbstractList'
matcher-mode: EQUALS_FULLY
annotations:
  - name: 'any.Annotation'
    matcher-mode: EQUALS_FULLY
```

## Method Matcher

The matcher used to determine whether a method is affected by a certain scope contains the *same attributes as the previously described type matcher* but also contains additional ones.

Besides `name`, `matcher-mode` and `annotations`, the method matcher contains the following attributes:

|Attribute|Default|Description
|---|---|---|
|`visibility`| [PUBLIC, PROTECTED, PACKAGE, PRIVATE]| A list of visibility modifiers. The target method has to use one of the specified modifiers. Possible values: `PUBLIC`, `PROTECTED`, `PACKAGE`, `PRIVATE`
|`arguments`|-| A list of fully qualified class names representing the method's arguments.
|`is-synchronized`|-| Specifies whether the target method is synchronized.
|`is-constructor`|`false`| Specifies whether the target method is a constructor. If this value is `true`, the `name` and `is-synchronized` attribute will *not* be used!

The following example will match against all methods which are exactly named `contains`, use the `PUBLIC` visibility modifier, have exactly one argument which is a `java.lang.Object`, are not synchronized and are annotated by the annotation `any.Annotation`.

```yaml
name: 'contains'
matcher-mode: EQUALS_FULLY
visibility: [PUBLIC]
arguments: ["java.lang.Object"]
is-synchronized: false
annotations:
  - name: 'any.Annotation'
    matcher-mode: EQUALS_FULLY
is-constructor: false
```

## Advanced Settings

The scope definition's advanced settings contains currently the following two attributes:

|Attribute|Default|Description
|---|---|---|
|`instrument-only-inherited-methods`| false | If this value is `true`, only methods will be instrumented which are inherited of a superclass or interface which were specified in the `interfaces` or `superclass` configuration.
|`disable-safety-mechanisms`| false | By default, the agent will not allow scopes containing only "any-matcher" like `MATCHES(*)`. If required, this safety feature can be disabled by setting this value to `true`.

## Example Scope Definition

The following code-snippet contains an example of a complete scope definitions.
Note: the following configuration contains all possible attributes even though they are not all necessary!

```yaml
inspectit:
  instrumentation:
        scopes:
          # the id of the following defined scope element - this example scope targets the ArrayList's contains method
          's_example_list_scope':
            # interfaces which have to be implemented
            interfaces:
              - name: 'java.util.List'
                matcher-mode: EQUALS_FULLY
                annotations:
                  - name: 'any.Annotation'
                    matcher-mode: EQUALS_FULLY
            # the superclass which has to be extended
            superclass:
              name: 'java.util.AbstractList'
              matcher-mode: EQUALS_FULLY
              annotations:
                - name: 'any.Annotation'
                  matcher-mode: EQUALS_FULLY
            # matcher describing the class' name (full qualified)
            type:
              name: 'ArrayList'
              matcher-mode: ENDS_WITH
              annotations:
                - name: 'any.Annotation'
                  matcher-mode: EQUALS_FULLY
            # the targeted method - each method which matches at least one of the defined matchers will be instrumented
            methods:
              - name: 'contains'
                matcher-mode: EQUALS_FULLY
                visibility: [PUBLIC]
                arguments: ["java.lang.Object"]
                is-synchronized: false
                annotations:
                  - name: 'any.Annotation'
                    matcher-mode: EQUALS_FULLY
              - is-constructor: true
                visibility: [PUBLIC]
                arguments: []
                annotations:
                  - name: 'any.Annotation'
                    matcher-mode: EQUALS_FULLY
             # advances settings which can be used to specify and narrow the instrumentation
            advanced:
              instrument-only-inherited-methods: false
              disable-safety-mechanisms: false
            # exclude the methods from the specified scope
            exclude:
              's_to_be_exclude': true
```