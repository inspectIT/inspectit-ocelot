---
id: scope-wizard 
title: Scope Wizard 
sidebar_label: Scope Wizard
---

Since writing configurations for inspectIT Ocelot can be a tedious task - especially if one is neither familiar with the
YAML format nor the configuration itself - the inspectIT Configuration Server offers a Scope Wizard for creating basic
configurations using a graphical user interface.

## Accessing the Scope Wizard

One can easily access the Scope Wizard by clicking the Dropdown-Button next to the `New File`-button in the top left
corner of the server UI and selecting the entry `Method Configuration`.

![Creating a file for Method Configuration](assets/scope-wizard-method-configuration.png)

Next, a prompt asks you to enter a name for your new configuration file.

After entering a name for your config, the file is displayed in the file browser on the left side of the window. Note
how a cog symbol marks the file. This symbol indicates that the file was created using the Scope Wizard.

![Method Configuration Files are displayed with a cog icon](assets/scope-wizard-method-configuration-2.png)

## Creating a Configuration

After creating your empty file with the Scope Wizard, you can see a blank screen with an `Add`-button in the lower-left corner. 
Clicking this button, the dialogue for defining a scope opens up. 
The dialogue is split into two parts: the Type Matcher and the Method Matcher.

![Defining a Scope using the Scope Wizard](assets/scope-wizard-method-configuration-3.png)

### Type Matcher

The Type Matcher can be used to define parameters to select classes from your application.  
First, you need to set if you want to match a class's own name with `Class`, the name of a superclass it is extending with `Superclass`, or the name of interfaces it is implementing with `Interface`, against the defined pattern.   
Then you need to select a matching rule (e.g. `equals` or `starts with`) and define the matching pattern.

:::tip
If you are not familiar with the system you are monitoring, you can use the [Class Browser](class-browser) to browse the classes loaded by your inspectIT agents.
:::

### Method Matcher

Like how you can select classes by matching them against parameters using the Type Matcher, you can also select specific methods using the Method Matcher. 
For this, the following settings are used.

#### Visibility

You can define the visibility of your target method. 
The four options correspond to the visibility specifiers in Java `public`, `protected`, `package`, and `private`.

#### Name Matcher

You need to decide whether your target method should be selected by matching its name against a pattern or if it simply is a constructor.  
If you set it to `Constructor` the name matching will be deactivated, since constructors do not have a name to match and instead can be found automatically.  
Otherwise, you can define a name matcher by choosing a matcher type, e.g. `starts with`, and the pattern to match against.

#### Method Argument Matcher

Finally, the last setting enables you to select methods based on their arguments. 
This helps if there are multiple methods with different arguments that all match the previous settings (e.g. if there is an overloaded method), but you only want to instrument specific ones.  
Activate `Only with specified arguments` by clicking the respective checkbox. 
With the `+`-button you can add as many arguments as you need for the method. 
Then you can add the fully qualified name of each argument's type.

![Method Matcher](assets/method-matcher-example.png)

The matcher in the screenshot above, for instance, would match all of 
- `public myTestMethod(String someParam1, Object someParam2)`, 
- `protected myTestMethod(String someParam1, Object someParam2)`, 
- `myTestMethod(String someParam1, Object someParam2)`, 
- and `private myTestMethod(String someParam1, Object someParam2)`.


### Enabling and Disbaling Tracing and Time Measurement

After creating and applying the Scope, it appears in your main window as shown in the screenshot below.  
Here you can toggle two settings: `Tracing` and `Measure`.  
`Tracing` enables stack tracing for the respective Scope.  
`Measure` enables measuring the execution time for all methods in the respective Scope.

![Trace and Measure via UI](assets/scope-wizard.png)

## Convert back to YAML

Sometimes you want to extend the configuration you have created with functions that are not supported by the Scope Wizard. 
In this case you can either click the `Show as YAML`-button in the top-right corner of the UI or convert the file directly to YAML. 
To convert the file, just click the arrow next to the `Show as YAML`-button and select `Convert to YAML`.

:::warning
Converting the file back to YAML is *not* reversible! Only use this feature if you are 100% certain that you won't use the UI-based configuration anymore!
:::

![Convert to YAML](assets/scope-wizard-2.png)

