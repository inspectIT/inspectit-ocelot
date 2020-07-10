---
id: cs-configuration
title: Server Configuration
---

The inspectIT Ocelot Configuration Server is based on the Spring Boot framework which provides a rich set on configuration settings.
There are several ways to customize the server settings according to your needs.

In the following section, we use the approach of using an `application.yml` file to specify the customized settings which is located in the same directory as the server's JAR file.

:::note
Please note that the inspectIT Ocelot configuration server uses the prefix `inspectit-config-server` for its specific inspectIT properties. Up to version 0.4, the prefix `inspectit` was used which has been changed to avoid confusion between the configurations of the different components.
:::

## Configure the HTTP(S) Port

By default, the configuration server uses port 8090 to bind its HTTP endpoints.
This can be changed by setting the `server.port` property to the desired port number.

The following code causes the server to use port `8888`.
```YAML
server:
    port: 8888
```

## Configure SSL 

In order to use SSL encryption for the server's endpoints, the `server.ssl.*` properties have to be configured.
The following properties can be used to configure SSL.

| Property | Description |
| --- | --- |
| `server.ssl.key-store` | The path to the keystore containing the certificate |
| `server.ssl.key-store-password` | Password used to access the key store |
| `server.ssl.key-store-type` | The format used for the keystore. It could be set to `JKS` or `PKCS12` |
| `server.ssl.key-password` | Password used to access the key in the key store | 
| `server.ssl.key-alias` | Alias that identifies the key in the key store (in case the store contains multiple certificates) |

:::note
Currently, it is not possible to use HTTP and HTTPS at the same time. If HTTPS is configured, the server's endpoint will not accept HTTP requests.
:::

The following code causes the server to use the certificate which is mapped to the alias `ocelot` and contained in the key-store `/opt/inspectit/ocelot.p12`.

```YAML
server:
  ssl:
    key-store-type: PKCS12
    key-store: /opt/inspectit/ocelot.p12
    key-store-password: my-keystore-secret
    key-alias: ocelot
```

You can find more information on how to setup SSL on the following sites:
* [Baeldung - HTTPS using Self-Signed Certificate in Spring Boot](https://www.baeldung.com/spring-boot-https-self-signed-certificate)
* [Spring Boot Reference Guide - Configure SSL](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl)