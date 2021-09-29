FROM openjdk:8
COPY build/libs/inspectit-ocelot-eum-server-SNAPSHOT.jar /inspectit-ocelot-eum-server.jar
COPY docker/entrypoint.sh  /
ENTRYPOINT ["sh", "/entrypoint.sh"]
