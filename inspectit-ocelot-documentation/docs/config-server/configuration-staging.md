---
id: configuration-files-staging
title: Configuration Files Staging Using Remote Git
sidebar_label: Configuration Files Staging
---

:::tip
It is recommended to first familiarize yourself with [how the configuration server manages configuration](config-server/managing-configurations.md) files before reading this chapter.
:::

Since version `1.11.0` the Configuration Server offers the possibility that external Git repositories can be connected.
This allows configuration files to be obtained from an external Git repository and also transferred to it.
This allows us to secure configuration files (e.g. for creating backups), initialize configuration servers with a specific set of configuration files or chain several configuration servers together.
The latter can be used, for example, to cover more complex scenarios, such as synchronizing configuration files across multiple system stages.

:::important
All configuration server properties mentioned below refer to being set under the `inspectit-config-server.remote-configurations` key!
:::

Note that this feature is **disabled by default**.
It can be enabled by setting the property `inspectit-config-server.remote-configurations.enabled` to `true`.

## Synchronization Workflow

The synchronization of configuration files can be divided into two areas: pulling and pushing configuration files.
These two options can be used together or separately.
For example, a configuration server can be set to only pull files from a remote Git repository, but not push any away.

Using the following diagram, the process and the individual steps will be explained in more detail.

![Configuration Files Staging Workflow](assets/staging-workflow.png)

### Pulling Configuration Files

The configuration server can be set to pull configuration files from a specific branch from a remote Git.
Depending on the configuration, this can be done when the **server is started** (disabled by default) or [**triggered by a webhook**](#triggering-file-pulling-using-webhooks).

In order for this to work, a remote Git repository must be configured and the appropriate branch specified.
This can be configured using the `pull-repository` property.
Please refer to the [Repository Configuration section](#repository-configuration) for more information on how to configure a repository and its branch.

![Configuration Files Staging Workflow](assets/staging-pull.png)

When the synchronization process *(1)* is triggered, the configuration server obtains all files that have changed (added, modified, deleted) since the last synchronization and puts them into the local `WORKSPACE` Branch.
Please note that the **synchronized files overwrite the local files**!
Changes that were only made locally will be overwritten and lost.
Files that have been created or changed locally, but were **not included in the changes** made on the remote branch, remain unaffected.

By default, the configuration server will not synchronize on startup. However, this can be set with the `pull-at-startup` option.

#### Initial Synchronization

When the configuration server performs a synchronization for the **very first time** (for example, because the server has been upgraded and this feature is now available), **no files are pulled** by default.
Only changes that happen from now on will also be synchronized.
This is done so that configuration files are not accidentally overwritten and lost.

This behavior can be changed with the option `initial-configuration-sync`.
If this option is set to `true`, the configuration server pulls all files that are in the remote Git repository at the very first time.
Please note that only files are added or overwritten.
No files are deleted!

This option is useful if, for example, a new instance of a configuration server should be populated with a set of configuration files.

### Auto-Promotion of Configuration Files

When the configuration server pulls configuration files, they will be put into the `WORKSPACE` branch.
By default, the server **automatically performs a promotion** *(3)* of the pulled files after a successful pull, so that the `WORKSPACE` and the `LIVE` branch are up-to-date.
However, this function can be disabled with the `auto-promotion` property.
If this option is disabled, the server will not perform an automatic promotion and the `LIVE` branch will remain untouched.

### Pushing Configuration Files

The configuration server can be configured to send the `LIVE` branch respectively files that are promoted to it to a remote Git repository.
Depending on the configuration, this can be done when the **server is started** (enabled by default) or when a **configuration file promotion** is done.

In order for this to work, a remote Git repository must be configured and the appropriate branch specified.
This can be configured using the `push-repository` property.
Please refer to the [Repository Configuration section](#repository-configuration) for more information on how to configure a repository and its branch.

![Configuration Files Staging Workflow](assets/staging-push.png)

If pushing of the configuration files is triggered (3), the configuration server pushes the files to the configured remote Git repository.
It is important to note that the sending is **forced by default**, but this can be disabled with the `push-repository.use-force-push` option.
Even if this option is enabled, the server will always try an unforced push first.
Since this can cause a commit to be lost in the remote repository due to the forced push, it is recommended that only a single configuration server can push to the target branch.

This process can also be triggered by an automatic promotion due to pulling configuration files from a remote Git repository.

## Triggering File Pulling Using Webhooks

As already mentioned, pulling configuration files from a remote Git repository can be done at startup of the configuration server, or can be triggered by a webhook.
Triggering this process through a webhook can be handy if you want to trigger a synchronization whenever a change happens on the configured remote branch, for example.

For this purpose, the configuration server provides the following endpoint, which can be triggered via an HTTP GET request: `/api/v1/hook/synchronize-workspace`.

:::note
This endpoint will only work if pulling configuration files from a remote Git repository has been enabled and configured.
:::

For security reasons, this endpoint can only be accessed with a predefined token, so that not any arbitrary user can trigger this process.
This token is included in the request via the query parameter `token`.

The tokens can be set up using the following property: `inspectit-config-server.security.webhook-tokens`.
It is possible to define several tokens, which can be used if individual tokens have to be deactivated again.

```yaml
inspectit-config-server:
  security:
    webhook-tokens:
      - 'very_secure_token'
      - 'another_very_secure_token'
```

#### Example

Assuming the configuration server is started locally on the default port `8090` and a token `secret_token` has been set up for webhook access, this can be triggered with the following CURL command.

```bash
$ curl http://localhost:8090/api/v1/hook/synchronize-workspace?token=secret_token
```

## Chaining Multiple Configuration Servers

![Configuration Server Chaining using remote Git repository](assets/staging-chain.png)

## Configuration Properties

### Repository Configuration