#!/bin/bash

source "$(dirname "$0")/../utils/load-env.sh"

# Enable Prometheus globally
curl -i -X POST $KONG_URL/plugins \
  --data "name=prometheus"

# Add ACL plugin to user service
curl -i -X POST $KONG_URL/services/user-service/plugins \
  --data "name=acl" \
  --data "config.allow=admin,users"

# Create a consumer and assign to ACL group
curl -i -X POST $KONG_URL/consumers \
  --data "username=vikas"

curl -i -X POST $KONG_URL/consumers/vikas/acls \
  --data "group=admin"
