package com.practicefintech.pgmock.admin.dto;

import com.practicefintech.pgmock.pg.OutcomeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScriptRequest(@NotBlank String refundId, @NotEmpty List<OutcomeType> steps) {
}
