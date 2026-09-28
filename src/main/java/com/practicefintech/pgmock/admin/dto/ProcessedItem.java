package com.practicefintech.pgmock.admin.dto;

import java.time.Instant;

public record ProcessedItem(String refundId, String orderId, long amount, Instant processedAt) {
}
