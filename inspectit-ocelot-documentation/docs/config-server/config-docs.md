---
id: config-docs
title: Configuration Docs
---

Since version 1.15.0 the inspectIT Ocelot configuration server provides the option to look at documentation for instrumentation objects, i.e. scopes, rules and actions, for a chosen agent mapping.

## Using existing Configuration Docs

To open the sidebar for the documentation, click on the `Configuration Docs`-button to the right of the configuration editor.

![Picture of opened Configuration Docs view](assets/config-docs-button.png)

Then select an agent mapping, for which the documentation should be shown, from the Dropdown menu. 
They are alphabetically ordered, and can be filtered using the search box.

![Picture of agent mapping dropdown](assets/config-docs-dropdown.png)

You can also decide whether to include or exclude the default configuration with the checkbox below.

The documentation for this agent mapping will then be shown below.
It updates whenever you change either of the settings at the top, or save changes to a configuration file in the editor.

To help with searching documentation for specific objects, you can filter the documentation using the filter text box above.

![Picture of documentation filtered for 'attach'](assets/config-docs-filter.png)


## Writing your own Configuration Docs

Part of the documentation is automatically created based on the settings of each instrumentation object, so there will be some documentation for your own configurations without additional effort.
However, this is of course limited, so to make full use of the feature, you should extend them as described in the following.

### All instrumentation objects

With all three kinds of instrumentation objects documentation is added below the key `docs`.  
With the key `description`, you can provide a general description of what the instrumentation object is for, what it does or how it works.  
With the key `since` you can add a versioning tag as a string that will also be shown in the documentation.

For scopes and rules these two options are all, for actions there a few more.
The documentation for a rule could then for example look like the following:
```YAML
rules:
  'r_new_rule':
    docs:
      description: |
        Captures execution time of current method.
        The capturing will only happen if xy is true.
      since: '1.42.0'
```

### Special values for actions

For actions there are two more possible keys below `docs`.

One is `inputs` below which you can add any input names as keys and a description for them as values.
For special input parameters (see [Input Parameters](../instrumentation/rules#input-parameters)) the description is added automatically, so you do not have to add any yourself.  
The other is `return-value` which should contain a description of the return value, if there is any.

So all in all, an action with documentation could look like the following:
```YAML
actions:
  'a_attachment_get':
    docs:
      description: 'Reads a given attachment from a target object.'
      inputs:
        'target': 'Object from which the attachment should be read.'
        'key': 'Key for attachment that should be read.'
      return-value: 'The value of the wanted attachment for the given object.'
      since: '1.42.0'
    input:
      '_attachments': 'ObjectAttachments'
      'target': 'Object'
      'key': 'String'
    value: '_attachments.getAttachment(target, key)'
```