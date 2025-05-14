#!/bin/bash
source "$(dirname "$0")/../utils/load-env.sh"

USERNAME=$1

if [ -z "$USERNAME" ]; then
  echo "Usage: $0 <username>"
  exit 1
fi

echo "Creating consumer: $USERNAME"

curl -s -X POST $KONG_URL/consumers \
  --data "username=$USERNAME"

echo "Creating JWT credentials for $USERNAME"

curl -s -X POST $KONG_URL/consumers/$USERNAME/jwt

echo "Adding user $USERNAME to ACL Group Admin"

curl -i -X POST $KONG_URL/consumers/$USERNAME/acls \
  --data "group=admin"