# inspectIT Ocelot - Configuration Server

This project contains the Ocelot configuration server.
It's a standalone component based on Spring Boot.
It can be build by `gradle bootJar` and started by `java -jar inspectit-ocelot-configurationserver-X.X.X.jar` or directly via gradle `gradle bootRun`.

It is used to manage and store agent configurations.
The configurations can be fetched by Ocelot agents via a REST interface.
By default, the REST interface is exposed on port 9090.

The application is using Swagger for API documentation and is also providing a Swagger UI.
By default, the Swagger UI is available at: http://localhost:9090/swagger-ui.html
