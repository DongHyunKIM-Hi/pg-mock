package com.practicefintech.pgmock.admin.dto;

import java.time.Instant;

public record RequestLogView(int seq, String refundId, String orderId, long amount,
                              Instant receivedAt, Instant respondedAt, Integer httpStatus, String result) {
}
