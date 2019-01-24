#!/usr/bin/env bash
echo "$DOCKER_HUB_PW" | docker login -u "$DOCKER_HUB_USER" --password-stdin
