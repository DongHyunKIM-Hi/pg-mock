package com.practicefintech.pgmock.admin;

import com.practicefintech.pgmock.admin.dto.ProcessedItem;
import com.practicefintech.pgmock.admin.dto.ProcessedListResponse;
import com.practicefintech.pgmock.admin.dto.RequestLogView;
import com.practicefintech.pgmock.admin.dto.ScriptRequest;
import com.practicefintech.pgmock.pg.PgMockService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 튜터가 수강생 제출물을 직접 실행해 검증할 때 쓰는 API. 수강생에게는 절대 공개하지 않는다.
 * admin.enabled=false(기본값)면 이 컨트롤러 빈 자체가 등록되지 않아 /admin/* 은 404가 된다.
 * 계약: assignments/01-카카오뱅크/project/contract/pg-mock-admin.yaml
 */
@RestController
@RequestMapping("/admin")
@ConditionalOnProperty(prefix = "admin", name = "enabled", havingValue = "true")
public class AdminController {

    private final PgMockService service;

    public AdminController(PgMockService service) {
        this.service = service;
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        service.reset();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/script")
    public ResponseEntity<Void> script(@Valid @RequestBody ScriptRequest request) {
        service.scheduleScript(request.refundId(), request.steps());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests")
    public List<RequestLogView> requests() {
        return service.requestLog().stream()
                .map(e -> new RequestLogView(e.seq(), e.refundId(), e.orderId(), e.amount(),
                        e.receivedAt(), e.respondedAt(), e.httpStatus(), e.result()))
                .toList();
    }

    @GetMapping("/processed")
    public ProcessedListResponse processed() {
        List<ProcessedItem> items = service.processedList().stream()
                .map(r -> new ProcessedItem(r.refundId(), r.orderId(), r.amount(), r.processedAt()))
                .toList();
        return new ProcessedListResponse(items, service.countByOrder());
    }
}
