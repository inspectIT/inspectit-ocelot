FROM grafana/grafana:5.4.2

USER root
RUN apt-get update && apt-get -y install unzip
RUN cd "$GF_PATHS_PLUGINS" && \
	mkdir grafana-influxdb-flux-datasource && \
	curl -LO https://github.com/NovatecConsulting/novatec-service-dependency-graph-panel/releases/latest/download/novatec-service-dependency-graph-panel.zip && \
	unzip novatec-service-dependency-graph-panel.zip -d ./novatec-servicegraph-panel
USER grafana