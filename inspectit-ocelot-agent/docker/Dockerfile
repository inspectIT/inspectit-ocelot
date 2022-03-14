FROM busybox

ADD https://github.com/inspectIT/inspectit-ocelot/releases/download/1.15.1/inspectit-ocelot-agent-1.15.1.jar /inspectit-ocelot-agent.jar
RUN ln /inspectit-ocelot-agent.jar /javaagent.jar &&\
    chmod -R go+r /javaagent.jar

COPY entrypoint.sh  /
ENTRYPOINT ["sh", "/entrypoint.sh"]