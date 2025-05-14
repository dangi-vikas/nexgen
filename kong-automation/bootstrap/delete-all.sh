#!/bin/bash

source "$(dirname "$0")/../utils/load-env.sh"

echo "Cleaning setup for all services..."

# Delete services
for service in user-service cart-service order-service product-service inventory-service; do
  curl -s -X DELETE $KONG_URL/services/$service
done

# Delete all consumers
consumers=$(curl -s $KONG_URL/consumers | jq -r '.data[].id')
for id in $consumers; do
  curl -s -X DELETE $KONG_URL/consumers/$id
done

echo "All services cleaned."
