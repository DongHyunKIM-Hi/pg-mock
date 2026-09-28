package com.practicefintech.pgmock.pg.dto;

import java.time.Instant;

public record PgRefundResponse(String refundId, String result, boolean alreadyProcessed, Instant processedAt) {
}
