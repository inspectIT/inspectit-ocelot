---
id: jakarta
title: Upgrade to Jakarta Namespace
---

The release of Jakarta EE 9 in December 2020 did replace the previous **javax** namespace with the **jakarta** namespace.
Because of this transition, several technologies had to **rename their interfaces**. 
For example the _javax.servlet.Servlet_ was moved to _jakarta.servlet.Servlet_.

Since the default instrumentation of inspectIT Ocelot only addresses the javax namespace, 
you have to include an additional configuration to instrument the jakarta namespace for traces and metrics.

You can find the configuration in GitHub: [inspectit-ocelot-jakarta-configuration](https://github.com/inspectIT/inspectit-ocelot-configurations/blob/master/extensions/jakarta/servlet-api.yml)

---
Some technologies, which use the jakarta namespace:

- Tomcat 10+
- Jetty 11+
- Spring 6+
- Spring Boot 3+

You can find more information about the namespace transition on [https://jakarta.ee](https://jakarta.ee/blogs/javax-jakartaee-namespace-ecosystem-progress/)
