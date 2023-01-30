FROM busybox

ADD ./inspectit-ocelot-agent/docker/  /
RUN ln /inspectit-ocelot-agent.jar /javaagent.jar &&\
    chmod -R go+r /javaagent.jar

ENTRYPOINT ["sh", "/entrypoint.sh"]