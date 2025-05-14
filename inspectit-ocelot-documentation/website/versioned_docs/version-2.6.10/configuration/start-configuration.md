---
id: version-2.6.10-start-configuration
title: Start Configuration
original_id: start-configuration
---

There are some properties to configure the start procedure of the agent. These properties can only be
defined via [Java system properties](#java-system-properties) or [OS environment variables](#os-environment-variables).
If you specify both system property and OS environment variable, the system property will take precedence.

## Delaying agent start

Despite instrumenting asynchronously or synchronously, inspectIT always starts the instrumentation process as soon as
the agent is attached to a JVM. There are cases in which it is desirable to postpone the start of the instrumentation
process. Although this is rarely necessary, inspectIT provides the possibility to do so via system property
`inspectit.start.delay` or OS environment variable `INSPECTIT_START_DELAY`.

You provide a value interpreted as milliseconds the agent shall wait before the instrumentation process starts. If you
do not provide a value the instrumentation process will start immediately.

The Agent expects positive integers excluding zero. For all other values the agent will print an error message on stderr
and continue as if there was no value supplied.

Example using system property:
```bash
# this will delay the instrumentation process by 10 minutes
$ java -javaagent:"/path/to/inspectit-ocelot-agent-2.6.10.jar" \
   -Dinspectit.start.delay=600000 \
   -jar my-java-program.jar
```

Example using OS environment variable (using bash):
```bash
# this will delay the instrumentation process by 5 minutes
$ export INSPECTIT_START_DELAY=300000
$ java -javaagent:"/path/to/inspectit-ocelot-agent-2.6.10.jar" -jar my-java-program.jar
```

## Changing temporary directory

By default, the agent will use ``java.io.tmpdir`` (Java default) to create temporary files, such as logs or
JAR files. To change the path of the temporary directory, use the system property `inspectit.temp-dir` 
or the OS environment variable `INSPECTIT_TEMP_DIR`.

```
-Dinspectit.temp-dir=/user/dir
```

## Recycling JAR files

The agent requires multiple JAR files to allow [runtime instrumentation](instrumentation/instrumentation.md).
By default, these files will be created temporarily at runtime. 
Each agent will create these new JAR files for itself inside the temporary directory.
When you are running multiple agents on the same machine, this would consume additional storage space.
Thus, you can configure the agent to recycle such JAR files via the system property `inspectit.recycle-jars`
or the OS environment variable `INSPECTIT_RECYCLE_JARS`.

```
-Dinspectit.recycle-jars=true
```

The agent will look inside ``${temporary-directory}/inspectit-ocelot/2.6.10`` for JAR files.
If no files have been found, the agent will create new ones, which can also be used by other agents.
**These files will not be deleted after the shutdown of the agent.** Thus, when you are updating your agent version,
you will have to delete the JAR files from the previous version manually.
