package com.nexgen.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Consumer-side view of {@code cart-checkout}. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutEvent {

    private String eventId;

    private String eventType;

    private String userId;

    private String orderNumber;

    private List<String> productIds;

    private double totalAmount;
}
