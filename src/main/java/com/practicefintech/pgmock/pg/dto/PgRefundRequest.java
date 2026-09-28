package com.practicefintech.pgmock.pg.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PgRefundRequest(
        @NotBlank @Size(max = 50) String refundId,
        @NotBlank @Size(max = 50) String orderId,
        @Min(1) long amount
) {
}
