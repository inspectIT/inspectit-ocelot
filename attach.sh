#!/bin/bash

AGENT_VERSION=$(curl -s --head https://github.com/inspectit/inspectit-ocelot/releases/latest \
   | grep -P '(?<=https:\/\/github\.com\/inspectit\/inspectit-ocelot\/releases\/tag\/)(\w|\.)+' -o)

display_usage() {
  echo "This script requires a single attribute specifying the process ID of the JVM the agent should be attached to."
  echo -e "\nUsage: attach-ocelot-agent.sh [JVM-PID] \n"
}

if [ "$#" -ne 1 ]; then
  display_usage
  exit 1;
fi

echo "> Downloading inspectIT Ocelot agent (v$AGENT_VERSION) to /tmp/ocelot-agent.jar"
wget -q -O /tmp/ocelot-agent.jar "https://github.com/inspectIT/inspectit-ocelot/releases/download/$AGENT_VERSION/inspectit-ocelot-agent-$AGENT_VERSION.jar"

echo "> Downloading jattach utility to /tmp/jattach"
wget -q -O /tmp/jattach https://github.com/apangin/jattach/releases/latest/download/jattach
chmod +x /tmp/jattach

echo "> Attaching the inspectIT Ocelot agent to JVM process $1"
/tmp/jattach $1 load instrument false /tmp/ocelot-agent.jar
