FROM node:8-alpine

WORKDIR /artillery

COPY load-script.yml /artillery/load-script.yml
COPY docker-entrypoint.sh /artillery/docker-entrypoint.sh

RUN npm install -g artillery@1.6.0-26 --unsafe-perm=true --allow-root && \
    apk add --no-cache curl && \
    chmod +x /artillery/docker-entrypoint.sh

ENTRYPOINT ["/artillery/docker-entrypoint.sh"]
CMD ["run", "load-script.yml"]