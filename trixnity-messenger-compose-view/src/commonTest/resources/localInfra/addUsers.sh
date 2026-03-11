#!/bin/sh

until curl -sf http://localhost:8008/health; do sleep 1; done

cd trixnity-messenger-compose-view/src/commonTest/resources/localInfra

docker compose exec synapse register_new_matrix_user -a -u admin -p admin -c /data/homeserver.yaml
docker compose exec synapse register_new_matrix_user -u testuser -p testpassword -c /data/homeserver.yaml --no-admin
