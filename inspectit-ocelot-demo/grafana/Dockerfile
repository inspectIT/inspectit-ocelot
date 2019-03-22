FROM grafana/grafana:5.4.2
USER root
RUN apt-get update && apt-get -y install unzip
RUN cd /var/lib/grafana/plugins/ && mkdir grafana-influxdb-flux-datasource && curl -LO https://github.com/grafana/influxdb-flux-datasource/releases/download/5.2.6/influxdb-flux-datasource-5.2.6.zip && unzip influxdb-flux-datasource-5.2.6.zip -d ./grafana-influxdb-flux-datasource
USER grafana