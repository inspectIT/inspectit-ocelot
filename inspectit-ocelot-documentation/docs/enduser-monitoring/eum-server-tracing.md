---
id: eum-server-tracing
title: EUM Server Traces
sidebar_label: Collecting Traces
---

The EUM-server is capable of recording traces without any additional configuration. However, you need to enable
at least one trace exporter.

:::note
Note that if you use Boomerang as your EUM agent, it will not capture traces by default.
To capture traces with Boomerang, a special tracing plugin must be used.
More information can be found in the chapter on [installing the EUM agent](enduser-monitoring/install-eum-agent.md#traces).
:::

## Trace Exporter

The EUM server comes with the same OTLP trace exporter as the inspectIT Ocelot Java agent.
The exporter's configurations options are the same as for the [agent](tracing/trace-exporters.md).
However, they are located under the `inspectit-eum-server.exporters.tracing` configuration path.

### OTLP (Tracing)

By default, the OTLP exporter is enabled, but is not active as the `endpoint`-property is not set.
The property can be set via `inspectit-eum-server.exporters.tracing.otlp.endpoint`.

The following configuration snippet makes the OTLP exporter send traces to an OTLP receiver available under `localhost:4317`.

```yaml
inspectit-eum-server:
  exporters:
    tracing:
      otlp:
        # If OTLP exporter for the OT received spans is enabled.
        enabled: ENABLED
        # the URL endpoint, e.g., http://127.0.0.1:4317
        endpoint: localhost:4317
        # the transport protocol, e.g., 'http/thrift' or 'grpc'
        protocol: grpc
```

## Additional Span Attributes

The EUM server is able to enrich a received span with additional attributes.
Currently, the following attributes are added to **each** span.

| Attribute   | Description                                                                                                                                                                                     |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `client.ip` | The sender IP address of the request that sent the spans. This address can be anonymised if required by data protection rules. See [Masking Client IP Addresses](#masking-client-ip-addresses). |

### Masking Client IP Addresses

The attribute `client.ip` is added to each span, which contains the sender IP address of the request with which the span was sent.
This IP address can be anonymised with the following configuration:

```YAML
inspectit-eum-server:
  exporters:
    tracing:
      # Specifies whether client IP addresses which are added to spans should be masked.
      mask-span-ip-addresses: true
```

If the `mask-span-ip-addresses` setting is set to `true`, the last 8 bits are set to `0` for IPv4 addresses.
For IPv6 addresses, the last 48 bits are set to `0`.
