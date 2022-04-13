---
id: version-1.16.0-code-style
title: Configuration Code Style
original_id: code-style
---

Below you can see a pseudo example configuration file with comments regarding style conventions that inspectIT Ocelot's default configuration follows.
They might help you with making your configurations easy to read and understand.

If you are using the inspectIT Ocelot configuration server to write and/or manage your configurations, also consider the guidelines for its [Syntax Highlighter](config-server/syntax-highlighting.md#guidelines).  

Similarly, for writing documentation into your configurations see the section [Configuration Docs](../config-server/config-docs.md).

```YAML
inspectit:
  instrumentation:
    actions:

      # Any keys that are not predetermined by the underlying Java objects, 
      # for example names of instrumentation objects, should be in single quotes.
      # The keys itself should be written in snake case.
      # Furthermore, action names should always start with `a_`!
      'a_example_action':
        docs:
          # General description
          description: |
            This is only an example showing the code style for an action.
            
            The map can be defined as following:
            ...
              constant-input:
                target:
                  key_a: value_a
                  key_b: value_b
          # Descriptions for each input parameter
          inputs:
            # If an input's description would go over multiple lines, 
            # consider putting parts of it into the main description instead.
            'target': 'A map. See the description above.'
            'key': 'Key for attachment that should be read.'
            # Special input parameters like '_attachment' do not need any explicit
            # description, theirs is set automatically.
          # Description for return-value
          return-value: 'The value of the given attachment for the given target object.'
          # Optionally provide a version tag.
          since: '1.x'
        input:
          # Again, names for input parameters are not predetermined, 
          # so they should be in single quotes.
          'target': 'Map'
          'key': 'String'
          # Special input parameters have an underscore to help differentiate them.
          '_attachments': 'ObjectAttachments'
        # Prefer value-body for the action logic.
        value-body: 'return _attachments.getAttachment(target, key);'

    rules:

      # Rule names should always start with `r_`!
      'r_example_rule':
        docs:
          # General description
          description: |
            Conditionally captures the execution time of the current method into method_duration.
            The capturing will only happen it capture_time_condition is defined as true.
        scopes:
          's_example_scope': true
        entry:
          # Context variable names should always start with `c_`!
          'c_variable_a':
            only-if-true: 'c_variable_b'
            action: 'a_some_action'
            data-input:
              'my_input': 'c_variable_c'
    scopes:

      # Scope names should always start with `s_`!
      's_example_scope':
        docs:
          # General description
          description: |
            Covers any methods that execute JDBC statements, i.e. execute(), executeQuery() and executeUpdate().
        interfaces:
          - name: 'java.sql.Statement'
        methods:
          - name: 'execute'
          - name: 'executeQuery'
          - name: 'executeUpdate'
        advanced:
          instrument-only-inherited-methods: true
```