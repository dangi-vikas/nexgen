package com.nexgen.cart_service.client;

import com.nexgen.cart_service.dto.CheckoutRequest;
import com.nexgen.cart_service.dto.CheckoutResponse;

public interface OrderServiceClient {
    CheckoutResponse checkout(CheckoutRequest request, String authorizationHeader);
}
