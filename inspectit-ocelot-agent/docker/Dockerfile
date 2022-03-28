FROM busybox

COPY inspectit-ocelot-agent.jar /
RUN ln /inspectit-ocelot-agent.jar /javaagent.jar &&\
    chmod -R go+r /javaagent.jar

COPY entrypoint.sh  /
ENTRYPOINT ["sh", "/entrypoint.sh"]