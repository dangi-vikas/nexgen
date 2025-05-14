#!/bin/bash
source "$(dirname "$0")/../utils/load-env.sh"

# Function to create consumer and assign to group
create_consumer_group() {
  local USERNAME=$1
  local GROUP=$2
  echo "Creating consumer $USERNAME in group $GROUP"
  curl -s -X POST $KONG_URL/consumers \
    --data "username=$USERNAME"

  curl -s -X POST $KONG_URL/consumers/$USERNAME/acls \
    --data "group=$GROUP"

  curl -s -X POST $KONG_URL/consumers/$USERNAME/jwt
}

# Add consumers with groups
create_consumer_group admin-app admin
create_consumer_group frontend-app users
create_consumer_group internal-cart internal
create_consumer_group monitoring-app monitor
