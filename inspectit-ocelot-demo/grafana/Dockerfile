FROM grafana/grafana:5.4.2

USER root
## We could use latest unzip here, no need to pin version for installation
# hadolint ignore=DL3008,DL3009 
RUN apt-get update && apt-get -y install --no-install-recommends unzip
## GF_PATHS_PLUGINS is set via env
# hadolint ignore=DL3003 
RUN cd "$GF_PATHS_PLUGINS" && \
	mkdir grafana-influxdb-flux-datasource && \
	curl -LO https://github.com/NovatecConsulting/novatec-service-dependency-graph-panel/releases/latest/download/novatec-service-dependency-graph-panel.zip && \
	unzip novatec-service-dependency-graph-panel.zip -d ./novatec-servicegraph-panel
USER grafana
