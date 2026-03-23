#!/bin/sh

while true; do
    curl -sf http://localhost:8008/health && break

    echo "Failed healthcheck, retrying in 1 second"
    docker compose ps
    sleep 1
done

docker compose exec synapse register_new_matrix_user -a -u admin -p admin -c /data/homeserver.yaml
