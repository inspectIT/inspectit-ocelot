---
id: code-style
title: Code style
---

Below you can see a YAML with some comments regarding style conventions that inspectIT Ocelot's default configuration follows.
They might help you with making your configurations easy to read and understand.

If you are using the inspectIT Ocelot configuration server to write and/or manage your configurations, also consider the guidelines for its [Syntax Highlighter](../config-server/syntax-highlighting.md).  
Similarly, for writing documentation into your configurations see the section [Configuration Docs](../config-server/config-docs.md).
```YAML
inspectit:
  instrumentation:
    actions:

      # Any keys that are not predetermined by the underlying Java objects, 
      # for example names of instrumentation objects, should be in single quotes.
      'a_attachment_get':
        docs:
          # General description
          description: 'Reads a given attachment from a target object.'
          # Descriptions for each input parameter
          inputs:
            'target': 'Object from which the attachment should be read.'
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
          'target': 'Object'
          'key': 'String'
          # Special input parameters have an underscore to help differentiate them.
          '_attachments': 'ObjectAttachments'
        # Use value for single-line java code where a value should be returned.
        value: '_attachments.getAttachment(target, key)'

      'a_regex_replaceAll_multi':
        docs:
          description: |
            Replaces all matches in a string for all of the provided regex patterns with the given replacement strings.
          
            The input patterns_and_replacements needs to be given in the following form:
              constant-input:
                patterns_and_replacements:
                  - pattern: <regexA>
                    replacement: <replacementA>
                  - pattern: <regexB>
                    replacement: <replacementA>
              
            The replacement is executed in the order of the inputs.
          inputs:
            'string': 'Input string that should be modified.'
            # If an input's description would go over multiple lines, 
            # consider putting parts of it into the main description instead.
            'patterns_and_replacements': 'Contains patterns and replacements as described above.'
          return-value: 'Returns the modified string. If the input string is null, null is returned.'
        imports:
          - 'java.util'
        input:
          'string': 'String'
          'patterns_and_replacements': 'Map'
        # Use value-body for multi-line java code
        value-body: |
          if (string == null) {
            return null;
          }
          String result = string;
          for (int i = 0; i < patterns_and_replacements.size(); i++) {
            Map patAndRepl = (Map) patterns_and_replacements.get(String.valueOf(i));
            String regex = (String) patAndRepl.get("pattern");
            String replacement = Objects.toString(patAndRepl.get("replacement"));
            result = result.replaceAll(regex, replacement);
          }
          return result;

    rules:

      'r_capture_method_duration_conditional':
        docs:
          # General description
          description: |
            Conditionally captures the execution time of the current method into method_duration.
            The capturing will only happen it capture_time_condition is defined as true.
            For example, http instrumentation define capture_time_condition based on http_is_entry.
            The condition is there to prevent unnecessary invocations of System.nanoTime(), which can be expensive.
        include:
          'r_capture_method_entry_timestamp_conditional': true
        exit:
          'method_duration':
            only-if-true: 'capture_time_condition'
            action: 'a_timing_elapsedMillis'
            data-input:
              'since_nanos': 'method_entry_time'
    scopes:

      's_jdbc_statement_execute':
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