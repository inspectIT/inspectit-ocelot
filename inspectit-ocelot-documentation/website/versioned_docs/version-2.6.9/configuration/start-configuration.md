---
id: version-2.6.9-start-configuration
title: Start Configuration
original_id: start-configuration
---

There are some properties to configure the start procedure of the agent. These properties can only be
defined via [Java System Properties](#java-system-properties) or [Environment Variables](#os-environment-variables).

## Changing temporary directory

By default, the agent will use ``java.io.tmpdir`` (Java default) to create temporary files, such as logs or
JAR files. To change the path of the temporary directory, use the following property:

```
-Dinspectit.temp-dir=/user/dir
```

## Recycling JAR files

The agent requires three JAR files to allow [runtime instrumentation](instrumentation/instrumentation.md).
By default, these files will be created temporarily at runtime. They are called _ocelot-bootstrap.jar_, _ocelot-core.jar_ and
_ocelot-opentelemetry-fat.jar_. 
Each agent will create three new JAR files for itself inside the temporary directory.
When you are running multiple agents on the same machine, this would consume additional storage space.
Thus, you can configure the agent to recycle such JAR files:

```
-Dinspectit.recycle-jars=true
```
The agent will look inside ``${temporary-directory}/inspectit-ocelot/2.6.9`` for JAR files.
If no files have been found, the agent will create new ones, which can also be used by other agents.
These files will not be deleted after the shutdown of the agent. Thus, when you are updating your agent version,
you will have to delete the JAR files from the previous version manually.
