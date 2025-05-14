#!/bin/bash

echo "Running setup for all services..."

./bootstrap/setup-kong-plugins.sh
./services/setup-user.sh
./services/setup-cart.sh
./services/setup-order.sh
./services/setup-product.sh
./services/setup-inventory.sh

echo "All services registered."
