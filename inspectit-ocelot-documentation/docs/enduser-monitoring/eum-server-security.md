---
id: eum-server-security
title: EUM Server Security Configuration
sidebar_label: Security Configuration
---

In order to allow only authorized instead of public access, EUM-Server offers a simple, shared secret solution.

The following configuration snippet shows the default configuration for security.
```yaml
inspectit-eum-server:
  security:
    # Enable/Disable Security
    enabled: false
    # Change name of authentication header if required
    authorization-header: Authorization
    # White list certain urls which will not be secured
    permitted-urls:
      - "/actuator/health"
      - "/boomerang/**"
    auth-provider:
      simple:
        # Enable/Disable Provider
        enabled: false
        # The directory where token files are stored. Empty by default to force users to provide one
        token-directory: ""
        # Flag indicates if token-directory should be watched for changes and tokens reloaded
        # The name of the initial token file. If a name is provided file will be created with an initial token
        default-file-name: "default-token-file.yaml"
        watch: true
        # How often token-directory should be watched for changes
        frequency: 60s
```

## Configuration options

All properties share the common prefix `inspectit-eum-server.security`.

| Property               | Default                                | Description                                                                                                                |
|------------------------|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `enabled`              | `false`                                | If `true` access to EUM-Server is secured.                                                                                 |
| `authorization-header` | `Authorization`                        | The HTTP-Header EUM-Server uses to the fetch the authorization information from.                                           |
| `permitted-urls`       | `/actuator/health` and `/boomerang/**` | A list of whitelisted url patterns. These URLs are publicly available without Authorization.                               |
| `auth-provider`        |                                        | Allows to configure different AuthenticationProviders that EUM-Server supports. <br/>Currently only `simple` is supported. |

Please note that if security is enabled, at least one AuthenticationProvider has to be enabled. Otherwise, all endpoints of EUM-Server are not accessible except for the whitelisted ones.

#### AuthenticationProvider `simple`

The AuthenticationProvider `simple` supports to secure EUM-Server using shared secrets aka tokens.

If this AuthenticationProvider is enabled, every client request needs to contain a known token as HTTP header (see `authorization-header` above). Otherwise, the request is rejected with a `403 - Forbidden` status code.

The known tokens are stored in yaml files below a configurable directory. Each file may contain multiple shared secrets.

Such a token consists of a name and a token value. `name` allows to identify the token. `token` is the actual shared secret.

Example:
```yaml
  # Identifies a token. E.g. you can document which application, person, organization, etc. knows about this token. It has no influence on security.
- name: "Name for first token"
  # The value of the token. If an HTTP-request contains this value (without opening and closing double quotes), access is allowed.
  token: "755f3e71-e43f-4715-bd26-b6e112fd30dd"
  # You man specify as many elements as you like
- name: "Name of other token"
  token: "any token value you like"
```

If this AuthenticationProvider is enabled, a default token file containing a generated token will be created, if that file does not yet exist.

It is possible to add, change and remove tokens without a restart. All files below the token directory will be scanned regularly by default. 

The following table describes the configuration options for the AuthenticationProvider `simple`.

All properties share the common prefix `inspectit-eum-server.security.auth-provider.simple`.

| Property name       | Default                   | Description                                                                                                                                                                  |
|---------------------|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`           | `false`                   | If `true` this AuthenticationProvider is enabled.                                                                                                                            |
| `token-directory`   |                           | The directory where token files are stored. If this authentication provider is enabled this value must not be empty. Otherwise, the application will not start successfully. |
| `default-file-name` | `default-token-file.yaml` | The name of the default token file name.                                                                                                                                     |
| `watch`             | `true`                    | If `true` the token directory is scanned for changes regularly.                                                                                                              |
| `frequency`         | `60s`                     | The frequency how often the token directory is scanned for changes.                                                                                                          |

## Client configuration

You need to configure your client to use one of the known tokens during requests to EUM-Server.

You should make sure to connect via https. Otherwise, it is trivial to intercept the token.

Example configuration using boomerang.js:

```javascript
// this must be one of your known tokens configured in EUM-Server
var token = "fancy but secret token";
BOOMR.init({
  beacon_url: "http://your.target.url",
  // the following two options are necesssary to configure Authorization for sending of beacons
  beacon_type: "POST", // for other beacon_types boomerang never sends an authorization header
  beacon_auth_token: token,
  // the following configures the Authorization for sending of spans
  OpenTelemetry: {
    collectorConfiguration: {
      headers: {"Authorization": token}
    }
  }
})
```
