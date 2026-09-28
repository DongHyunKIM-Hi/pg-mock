package com.practicefintech.pgmock.pg;

import com.practicefintech.pgmock.pg.dto.PgRefundRequest;
import com.practicefintech.pgmock.pg.dto.PgRefundStatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 API. 계약: assignments/01-카카오뱅크/project/contract/pg-mock.yaml */
@RestController
@RequestMapping("/pg")
public class PgController {

    private final PgMockService service;

    public PgController(PgMockService service) {
        this.service = service;
    }

    @PostMapping("/refunds")
    public ResponseEntity<?> requestRefund(@Valid @RequestBody PgRefundRequest request) {
        return service.handleRefund(request);
    }

    @GetMapping("/refunds/{refundId}")
    public PgRefundStatusResponse getStatus(@PathVariable String refundId) {
        return service.getStatus(refundId);
    }
}
