#!/bin/bash
source "$(dirname "$0")/../utils/load-env.sh"

echo "Setting up $INVENTORY_SERVICE_NAME..."

curl -s -X POST $KONG_URL/services \
  --data name=$INVENTORY_SERVICE_NAME \
  --data url=$INVENTORY_SERVICE_URL

curl -s -X POST $KONG_URL/services/$INVENTORY_SERVICE_NAME/routes \
  --data "paths[]=$INVENTORY_SERVICE_ROUTE"

curl -s -X POST $KONG_URL/services/$INVENTORY_SERVICE_NAME/plugins \
  --data "name=jwt"

curl -s -X POST $KONG_URL/services/$INVENTORY_SERVICE_NAME/plugins \
  --data "name=rate-limiting" \
  --data "config.minute=10"

curl -s -X POST $KONG_URL/services/$INVENTORY_SERVICE_NAME/plugins \
  --data "name=cors" \
  --data "config.origins=*" \
  --data "config.methods=GET,POST,PUT,DELETE" \
  --data "config.credentials=true"
