package com.practicefintech.pgmock.pg.dto;

import java.time.Instant;

public record PgRefundStatusResponse(String refundId, String status, Long amount, Instant processedAt) {
}
