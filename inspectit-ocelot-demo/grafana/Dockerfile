FROM grafana/grafana:6.7.3

USER root

RUN apk add curl
RUN apk add unzip
RUN curl -LO https://github.com/NovatecConsulting/novatec-service-dependency-graph-panel/releases/download/v2.1/novatec-service-dependency-graph-panel.zip && \
	unzip novatec-service-dependency-graph-panel.zip -d "$GF_PATHS_PLUGINS/novatec-servicegraph-panel"

USER grafana