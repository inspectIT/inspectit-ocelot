FROM grafana/grafana:5.4.2

USER root

# We could use latest unzip here, no need to pin version for installation
# hadolint ignore=DL3008
RUN apt-get update && apt-get -y install --no-install-recommends unzip \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir "$GF_PATHS_PLUGINS/grafana-influxdb-flux-datasource" && \
	curl -LO https://github.com/NovatecConsulting/novatec-service-dependency-graph-panel/releases/latest/download/novatec-service-dependency-graph-panel.zip && \
	unzip novatec-service-dependency-graph-panel.zip -d "$GF_PATHS_PLUGINS/novatec-servicegraph-panel"

USER grafana