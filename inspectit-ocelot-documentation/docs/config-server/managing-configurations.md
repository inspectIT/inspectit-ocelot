---
id: managing-configurations
title: Managing Configuration Files
---

The configuration server internally uses Git to manage its working directory. This allows a versioning of configurations, so that it is possible to track when and how a configuration was created, changed or deleted. Additionally, it allows unwanted changes to be reverted and configuration files to be restored that would otherwise be lost.

Furthermore, a staging of configurations is possible. By default, the configuration server has two branches (`WORKSPACE` and `LIVE`) which contain the configuration files and which can be used as the basis for the configuration to be delivered. All changes made by users to the configuration files affect the `WORKSPACE` branch. The `LIVE` branch cannot be changed directly by users. A change to the `LIVE` branch is only possible by transferring an already done change to the `WORKSPACE` branch to the `LIVE` branch - in this case called "promotion".

:::tip
It is possible for agents to obtain the configurations exclusively from the `LIVE` branch. As a result, users can now edit configuration files without having to deliver these - possibly incomplete changes - directly, thus preventing warnings due to invalid configurations.
:::

In order to deliver specific configurations to agents, so-called "agent mappings" can be used. These can be used to define precisely which files and from which branch an agent should receive.

![Configuration Server Workspace Architecture](assets/configuration-server-branches.png)

## Promoting Configuration Files

Changes to configuration files by users only affect the `WORKSPACE` branch. If a user wants to make a change to a configuration file, but the version of the `LIVE` branch is delivered to the agent, the user must do the following:

* First, the user must perform the change, which allows the change to be tracked on the workspace branch.
* Afterwards the change must be approved and promoted by a user who has promotion rights. Only through promotion the changes to a file will be transferred to the `LIVE` branch.

:::note
It is important to note that only whole files and not individual changes can be promoted. This means that if two different users have edited a single file it is only possible to promote the whole file and not just the changes of a specific user.
:::

In the following screenshot, the configuration server's promotion user interface is shown. It can be used to review, approve and promote configurations. Only users who have promotion rights can approve and promote configuration files.

![Configuration Server's promotion UI](assets/configuration-server-promotion-ui.png)

1. The promotion UI can be access via the navigation sidebar.
2. The UI shows a list of all files which have been changed on the `WORKSPACE` branch, thus, differ from the `LIVE` branch. The icons show whether a file has been newly created, edited or deleted. Approved files that will be promoted to the `LIVE` branch are highlighted in green with a check mark.
3. The current version of the file on the `LIVE` branch.
4. The current version of the file on the `WORKSPACE` branch.
5. Button to promote the approved files.
6. The selected file can be approved with this button.

## Four-Eyes Promoting Restriction

By default, any user with promotion rights can promote any changes.
In some setups it can be beneficial to enforce peer-reviews before promoting configuration changes.
The configuration server offers a mechanism to enforce a four eyes principle for configuration changes which can be enabled using the following setting:

```YAML
inspectit-config-server:
  security:
    four-eyes-promotion: true
```

When this setting is enabled, users with promotion rights will no longer be able to promote their own configuration changes.

:::note
This restriction is only applied to non-admin users! Users with admin rights will still be able to promote their own changes.
:::

## Git-Authors
Every change has an author consisting of the login and an e-mail address of the user who made the change. For 
ldap-users the login and e-mail address of the ldap account is used. 
<br>
For internal users however, an e-mail address is generated. This address consists of the user's login and an e-mail
suffix. The default suffix is `@inspectit.rocks`.
<br>
You can provide a custom mail suffix in the following settings: 
```YAML
inspectit-config-server:
  mail-suffix: @my_mail.com
```

## External Changes

While it is not recommended, it is possible to directly change the configuration files in the filesystem instead of via the
configuration server's UI or REST-API.

In order for your changes in the file-system to become active, you need to let the configuration server know about the external changes.
This can be done by sending an HTTP GET request to the `/api/v1/configuration/reload` endpoint. This request needs to include your credentials via basic auth.
A request to this endpoint causes all external changes to be committed to the `WORKSPACE` branch and the server to be updated accordingly.

Alternatively, you can also manually commit to the `WORKSPACE` branch in the working directory of the configuration server.
However, you need to make sure that the server is either shut down or you need to have the guarantee that no other users are currently editing files via the UI,
otherwise your repository might get corrupted.