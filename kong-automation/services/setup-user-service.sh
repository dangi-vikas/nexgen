#!/bin/bash
source "$(dirname "$0")/../utils/load-env.sh"

echo "Setting up $USER_SERVICE_NAME..."

curl -s -X POST $KONG_URL/services \
  --data name=$USER_SERVICE_NAME \
  --data url=$USER_SERVICE_URL

curl -s -X POST $KONG_URL/services/$USER_SERVICE_NAME/routes \
  --data "paths[]=$USER_SERVICE_ROUTE"

curl -s -X POST $KONG_URL/services/$USER_SERVICE_NAME/plugins \
  --data "name=jwt"

curl -s -X POST $KONG_URL/services/$USER_SERVICE_NAME/plugins \
  --data "name=rate-limiting" \
  --data "config.minute=10"

curl -s -X POST $KONG_URL/services/$USER_SERVICE_NAME/plugins \
  --data "name=cors" \
  --data "config.origins=*" \
  --data "config.methods=GET,POST,PUT,DELETE" \
  --data "config.credentials=true"
