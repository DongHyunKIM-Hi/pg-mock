package com.practicefintech.pgmock.pg;

import java.time.Instant;

/**
 * 수신한 요청 하나의 기록. 접수 시각에 즉시 만들어 로그에 추가하고(수신 순서 보존),
 * 처리가 끝나면 {@link #complete}로 결과를 채운다. 여러 스레드가 동시에 만들 수 있으므로
 * 완료 필드는 volatile로 둔다.
 */
public final class RequestLogEntry {

    private final int seq;
    private final String refundId;
    private final String orderId;
    private final long amount;
    private final Instant receivedAt;
    private volatile Instant respondedAt;
    private volatile Integer httpStatus;
    private volatile String result;

    public RequestLogEntry(int seq, String refundId, String orderId, long amount, Instant receivedAt) {
        this.seq = seq;
        this.refundId = refundId;
        this.orderId = orderId;
        this.amount = amount;
        this.receivedAt = receivedAt;
    }

    public void complete(Instant respondedAt, int httpStatus, String result) {
        this.respondedAt = respondedAt;
        this.httpStatus = httpStatus;
        this.result = result;
    }

    public int seq() {
        return seq;
    }

    public String refundId() {
        return refundId;
    }

    public String orderId() {
        return orderId;
    }

    public long amount() {
        return amount;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    public Instant respondedAt() {
        return respondedAt;
    }

    public Integer httpStatus() {
        return httpStatus;
    }

    public String result() {
        return result;
    }
}
