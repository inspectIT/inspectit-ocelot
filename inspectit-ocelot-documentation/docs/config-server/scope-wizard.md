---
id: scope-wizard 
title: Scope Wizard 
sidebar_label: Scope Wizard
---

Since writing configurations for inspectIT Ocelot can be a tedious task - especially if one is neither familiar with the
YAML format nor the configuration itself - the inspectIT Configuration Server offers a Scope Wizard for creating basic
configs using a User Interface.

## Entering the Scope Wizard

One can easily access the Scope Wizard by clicking the Dropdown-Button next to the "New File" button in the top left
corner of the server UI and selecting the entry _Method Configuration_.

![Creating a file for Method Configuration](assets/scope-wizard-method-configuration.png)

Next, a prompt asks you to enter a name for your new configuration file.

After entering a name for your config, the file is displayed in the file browser on the left side of the window. Note
how a cog symbol marks the file. This symbol indicates that the file was created using the Scope Wizard.

![Method Configuration Files are displayed with a cog icon](assets/scope-wizard-method-configuration-2.png)

## Creating a Configuration

After creating your empty file with the Scope Wizard, you can see a blank screen with an "Add"-button on the lower-left
corner. By clicking this button, the dialogue for defining a scope opens up. This dialogue is split into two sections: the Type Matcher
and the Method Matcher.

![Defining a Scope using the Scope Wizard](assets/scope-wizard-method-configuration-3.png)

### Type Matcher

The Type Matcher can be used to define a pattern for Classes, Superclasses, or Interfaces. It consists of three
different parts: The selection of the entity the Type Matcher should be matched on (Class, Superclass, or Interface), a
matching rule (i.E. equals, or starts with), and a matching pattern.

:::tip
If you are not familar to the system you are monitoring, you can use the [Class Browser](config-server/class-browser) to browse the classes loaded by your inspectIT agents.
:::

### Method Matcher

Like how you can match Classes or Interfaces with the Type Matcher, you can also match specific Methods using the Method
Matcher. However, instead of one matching parameter, four are to be defined.

#### Visibility

In the first section, you can define the visibility of your target method. The four options correspond to the visibility
specifiers in Java `public`, `protected`, `package`, and `private`.

#### Method type

In the second section, you can define whether your targeted method is a constructor or not. Note how the definition name
is disabled upon choosing the constructor options. The method name's definition is not needed since the system can
derive constructors from the settings described above.

#### Name Matcher

This third section is only available if the chosen method is not a constructor. You can define a Name Matcher by
choosing a matcher type, e.g., `starts with`, and then defining the respective method name.

#### Method Argument Matcher

Lastly, you can specify the specific arguments of the method you want to instrument. Activate _Only with specified
arguments_ by clicking the respective checkbox to do so. With the (+)-button you can add as many arguments as you need
for the method. Then you can add the fully qualified name of each argument type. The matcher in the screenshot below
would for instance match on the method `public myTestMethod(String someParam1, Object someParam2)`, `protected myTestMethod(String someParam1, Object someParam2)`, `private myTestMethod(String someParam1, Object someParam2)` and `myTestMethod(String someParam1, Object someParam2)`.

![Method Matcher](assets/method-matcher-example.png)

### Enabling and Disbaling Tracing and Time Measurement

After creating and applying the Scope, it appears in your main window, as shown in the screenshot below. Here you can toggle two methods: `Tracing` and `Measure`. `Tracing` enables stack tracing for the respective Scope. With `Measure` you can allow inspectIT to measure the execution time for all methods that apply to your Scope.

![Trace and Measure via UI](assets/scope-wizard.png)

## Convert back to YAML

Sometimes you want to extend the configuration you have created with functions that are not supported by the Scope Wizard. In this case you can either click the _Show as YAML_-button in the top-right corner of the UI or by converting the file directly to YAML. To convert the file, just click the arrow next to the _Show as YAML_-button and select `Convert to YAML`.

:::warning
Converting the file back to YAML is *not* reversible! Only use this feature if you are 100% certain that you won't use the UI-based configuration anymore!
:::

![Convert to YAML](assets/scope-wizard-2.png)

