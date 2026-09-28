package com.practicefintech.pgmock.pg;

import java.time.Instant;

/** 성공(또는 지연 후 성공)으로 처리 완료된 환불 건의 기록. 같은 refundId의 재요청을 판단하는 근거가 된다. */
public record ProcessedRecord(String refundId, String orderId, long amount, Instant processedAt) {
}
