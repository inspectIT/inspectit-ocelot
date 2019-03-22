# inspectIT Ocelot - Bootstrap

This project contains a manager class which will basically start the core of the inspectIT Ocelot Java agent.
It also provides interfaces which have to be implemented by each inspectIT Ocelot Java agent implementation (e.g. `IAgent`).

The resulting Jar file of this package will be pushed to the JVM's bootstrap class loader.
This is necessary in order to create a bridge between the user (application) classes and the classes which will be loaded by the inspectIT class loader (classes of the inspectIT Ocelot Java agent). 