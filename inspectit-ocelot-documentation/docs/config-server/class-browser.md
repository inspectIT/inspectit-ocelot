---
id: class-browser
title: Class Browser
sidebar_label: Class Browser
---

Each inspectIT agent can provide the classes loaded by the respective agent. You can retrieve and browse this list in the configuration server and use it for defining scopes for your configuration.

:::note
You need to enable [Agent Commands](../configuration/agent-command-configuration) for the Class Browser to work!
:::

## Accessing the Class Browser

You can access the Class Browser via the [Scope Wizard](scope-wizard). 
Create a new file using the Method Configuration and open the Scope Wizard. 
Click the search button in the Type Matcher and then select an agent.

![Class Browser Example](assets/class-browser-select.png)

Enter a search string in the search field and click `search` to retrieve the class tree. 
You can either leave the field blank or enter a full or partial fully qualified name, as shown in the second screenshot further below, to narrow down the results. 
Note that it can take a few seconds until the class list is displayed.

:::warning
Creating a class list, especially when leaving the search-field blank, temporarily increases the resource usage of the agent. Thus we advise you to use this feature in test systems primarily.
:::

## Browsing Classes, Interfaces, and Methods
The Class Browser is designed in a tree-like structure. 
Each entry with a folder icon corresponds to a part of a fully qualified name. 
Each entry with a circle icon is either an interface (i), a class (c), or a method (m).  
In the screenshot below, you can see some classes of `java.net`, for instance. 
As an example the methods of the class `URISyntaxException` are shown as well. 
There is also an interface, the `DatagramSocketImplFactory`.

![Class Browser Example](assets/class-browser.png)

## From trees to scopes
Now that you know which methods you want to monitor, you can select the radio button next to a method. 
Then click `Select`. 
The Scope-Wizard will be automatically configured according to your selection.

