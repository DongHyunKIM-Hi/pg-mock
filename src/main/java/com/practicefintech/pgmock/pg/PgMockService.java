package com.practicefintech.pgmock.pg;

import com.practicefintech.pgmock.config.PgMockProperties;
import com.practicefintech.pgmock.pg.dto.PgRefundRequest;
import com.practicefintech.pgmock.pg.dto.PgRefundResponse;
import com.practicefintech.pgmock.pg.dto.PgRefundStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 가상 결제대행사의 핵심 동작. 계약(project/contract/pg-mock.yaml)의 "요청 ID 인식 규칙"이
 * 이 클래스의 {@link #decide}에 그대로 구현되어 있다:
 *   1) 이미 성공 처리된 refundId → 다시 처리하지 않고 그 결과를 그대로 반환한다.
 *   2) 그 외에는 예약된 시나리오(/admin/script)나 확률(pgmock.weights)로 결과를 정한다.
 *   3) 성공 계열(SUCCESS, SLOW_BUT_PROCESSED)로 정해지면 "지연 전에" 처리 기록을 남긴다.
 *      그래야 SLOW_BUT_PROCESSED가 응답을 늦게 주는 동안 같은 ID로 재시도가 들어와도
 *      곧바로 "이미 처리됨"으로 답할 수 있다 (T1 함정의 핵심 장치).
 */
@Service
public class PgMockService {

    private final PgMockProperties properties;

    private final ConcurrentHashMap<String, ProcessedRecord> processed = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<OutcomeType>> scripts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> refundLocks = new ConcurrentHashMap<>();
    private final Queue<RequestLogEntry> requestLog = new ConcurrentLinkedQueue<>();
    private final AtomicInteger seq = new AtomicInteger(0);

    public PgMockService(PgMockProperties properties) {
        this.properties = properties;
    }

    public ResponseEntity<?> handleRefund(PgRefundRequest request) {
        Instant receivedAt = Instant.now();
        RequestLogEntry logEntry = new RequestLogEntry(
                seq.incrementAndGet(), request.refundId(), request.orderId(), request.amount(), receivedAt);
        requestLog.add(logEntry);

        Decision decision = decide(request);

        // 지연은 락 밖에서 수행한다: 이 요청이 12초를 기다리는 동안에도
        // 같은 refundId의 재시도가 막히지 않아야 하기 때문이다.
        sleepFor(decision.outcome());

        ResponseEntity<?> response = toResponse(decision);
        logEntry.complete(Instant.now(), extractStatus(response), decision.resultLabel());
        return response;
    }

    public PgRefundStatusResponse getStatus(String refundId) {
        ProcessedRecord record = processed.get(refundId);
        if (record == null) {
            return new PgRefundStatusResponse(refundId, "NOT_FOUND", null, null);
        }
        return new PgRefundStatusResponse(refundId, "PROCESSED", record.amount(), record.processedAt());
    }

    // ---------- 판정 ----------

    private Decision decide(PgRefundRequest request) {
        Object lock = refundLocks.computeIfAbsent(request.refundId(), id -> new Object());
        synchronized (lock) {
            ProcessedRecord existing = processed.get(request.refundId());
            if (existing != null) {
                return new Decision(request.refundId(), OutcomeType.SUCCESS, true, existing.processedAt());
            }

            OutcomeType outcome = nextScriptedOutcome(request.refundId())
                    .orElseGet(this::weightedRandomOutcome);

            Instant processedAt = null;
            if (outcome == OutcomeType.SUCCESS || outcome == OutcomeType.SLOW_BUT_PROCESSED) {
                processedAt = Instant.now();
                processed.put(request.refundId(),
                        new ProcessedRecord(request.refundId(), request.orderId(), request.amount(), processedAt));
            }
            return new Decision(request.refundId(), outcome, false, processedAt);
        }
    }

    private Optional<OutcomeType> nextScriptedOutcome(String refundId) {
        Deque<OutcomeType> queue = scripts.get(refundId);
        if (queue == null) {
            return Optional.empty();
        }
        synchronized (queue) {
            return Optional.ofNullable(queue.pollFirst());
        }
    }

    private OutcomeType weightedRandomOutcome() {
        PgMockProperties.Weights w = properties.weights();
        int roll = ThreadLocalRandom.current().nextInt(w.total());
        int successUpper = w.success();
        int failUpper = successUpper + w.fail();
        int errorUpper = failUpper + w.error();
        int slowUpper = errorUpper + w.slow();
        if (roll < successUpper) return OutcomeType.SUCCESS;
        if (roll < failUpper) return OutcomeType.FAIL;
        if (roll < errorUpper) return OutcomeType.ERROR;
        if (roll < slowUpper) return OutcomeType.SLOW;
        return OutcomeType.SLOW_BUT_PROCESSED;
    }

    // ---------- 지연·응답 ----------

    private void sleepFor(OutcomeType outcome) {
        int millis = switch (outcome) {
            case SLOW, SLOW_BUT_PROCESSED -> properties.delay().slowMs();
            default -> randomBetween(properties.delay().normalMinMs(), properties.delay().normalMaxMs());
        };
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int randomBetween(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + ThreadLocalRandom.current().nextInt(max - min);
    }

    private ResponseEntity<?> toResponse(Decision decision) {
        if (decision.outcome() == OutcomeType.ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        boolean success = decision.outcome() == OutcomeType.SUCCESS
                || decision.outcome() == OutcomeType.SLOW_BUT_PROCESSED;
        PgRefundResponse body = new PgRefundResponse(
                decision.refundId(),
                success ? "SUCCESS" : "FAIL",
                decision.alreadyProcessed(),
                decision.processedAt());
        return ResponseEntity.ok(body);
    }

    private int extractStatus(ResponseEntity<?> response) {
        return response.getStatusCode().value();
    }

    // ---------- 평가 전용(/admin) ----------

    public void reset() {
        processed.clear();
        scripts.clear();
        refundLocks.clear();
        requestLog.clear();
        seq.set(0);
    }

    public void scheduleScript(String refundId, List<OutcomeType> steps) {
        scripts.put(refundId, new ArrayDeque<>(steps));
    }

    public List<RequestLogEntry> requestLog() {
        return List.copyOf(requestLog);
    }

    public List<ProcessedRecord> processedList() {
        return List.copyOf(processed.values());
    }

    public Map<String, Integer> countByOrder() {
        return processedList().stream()
                .collect(Collectors.groupingBy(ProcessedRecord::orderId, Collectors.summingInt(r -> 1)));
    }

    /** 판정 결과 하나를 나타낸다. refundId를 들고 있어 응답 생성 시 요청 객체를 다시 참조하지 않아도 된다. */
    private record Decision(String refundId, OutcomeType outcome, boolean alreadyProcessed, Instant processedAt) {
        String resultLabel() {
            return switch (outcome) {
                case SUCCESS, SLOW_BUT_PROCESSED -> "SUCCESS";
                case FAIL, SLOW -> "FAIL";
                case ERROR -> "ERROR";
            };
        }
    }
}
