---
id: class-browser
title: Class Browser
sidebar_label: Class Browser
---

Each inspectIT agent can provide the classes loaded by the respective agent. You can retrieve and browse this list in the configuration server and use it for defining scopes for your configuration.

:::note
You need to enable [Agent Commands](config-server/agent-command-configuration) for the Class Browser to work!
:::

## Accessing the Class Browser

You can access the Class Browser via the Scope Wizard. Create a new file using the Method Configuration and open the [Scope Wizard](config-server/scope-wizard). You access the Class Browser by clicking the search button in the Type Matcher of the Scope Wizard and selecting an agent.

![Class Browser Example](assets/class-browser-select.png)

Enter a search string in the search field and click `search` to retrieve the class tree. You can either leave the field blank or enter a full or partial fqn, as shown below, to narrow down the results. Note that it can take a few seconds until the class list is displayed.

:::warning
Creating a class list, especially when leaving the search-field blank, temporarily increases the resource usage of the agent. Thus we advise you to use this feature in test systems primarily.
:::

## Browsing Classes, Interfaces, and Methods
The Class Browser is designed in a tree-like structure. Each entry marked with an arrow corresponds to a part of a fqn. Each entry with an icon is either an interface (i), a class (c), or a method (m). In the screenshot below, you can see some classes of `java.net`, for instance, `URISyntaxException`. You can also see the methods this classes implements. Also, there is an interface, the `DatagramSocketImplFactory`.

![Class Browser Example](assets/class-browser.png)

## From trees to scopes
Now that you know which methods you want to monitor, you can select the radio button next to a method. Then click `select`. The Scope-Wizard will be automatically configured according to your selection.

