rm -rf /agent/*
# Copy agent jar into shared volume
cp /inspectit-ocelot-agent.jar /agent/inspectit-ocelot-agent.jar
while true; do sleep 2; done;