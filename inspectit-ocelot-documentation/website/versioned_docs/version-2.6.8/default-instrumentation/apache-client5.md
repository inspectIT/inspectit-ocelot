---
id: version-2.6.8-apache-client5
title: Upgrade to Apache Client 5
original_id: apache-client5
---

The release of the Apache Client 5.0 in February 2020 introduced some breaking changes, including **namespace changes**.
For example the _org.apache.http.impl.client.CloseableHttpClient_ was moved to _org.apache.hc.client5.http.impl.classic.CloseableHttpClient_.
Thus, there are some efforts necessary to migrate from previous versions to version 5.

Since the default instrumentation of inspectIT Ocelot only addresses the Apache Client 4,
you have to include an additional configuration to instrument version 5 for traces and metrics.

You can find the configuration in GitHub: [inspectit-ocelot-apache-client5-configuration](https://github.com/inspectIT/inspectit-ocelot-configurations/tree/master/extensions/apache-client5)

---

Find more information about the migration from version 4 to version 5 on [https://hc.apache.org](https://hc.apache.org/httpcomponents-client-5.4.x/migration-guide/index.html).


