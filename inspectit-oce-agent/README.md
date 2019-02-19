# inspectIT OCE - Agent

This project has basically two goals: on the one hand it contains the implementation of the actual Java agent which will be attached to your JVM, and on the other it includes the "system test".

## Java Agent

This is the Java agent which is attached to your JVM.
It is responsible for attaching and loading required artifacts, like the bootstrap and core component of the agent.
Furthermore, the agents ensures to load classes of the inspectIT OCE Java agent using a separate class loader in order to isolate its classes and to prevent any negative side effects.

The files of the required artifacts (Jar file of the bootstrap and core component) are also contained in the resulting agent Jar (see the appropriate README files of the individual projects for more information).

## System Test 

The goal of the "system test" is to verify that the inspectIT OCE Java agent is working and able to successfully startup with the JVM.

The "system test" is a JUnit test where the Java agent is attached to.
By doing this, the JUnit test functions as test application which is monitored by the agent.

Using the unit tests which are executed, it is verified that the agent is collecting data and, in addition, the collected data is also verified.

## Performance (JMH) Tests

This project contains performance tests, which can measure the overhead of the attached inspectIT OCE agent to an application.
Similar to the system tests, here the JMH tests are executed after the Java agent is attached to the application and complete instrumentation is performed.

General information on how to run performance tests can be found in [the separate document](../resources/internal-docs/PERFTESTS.md).