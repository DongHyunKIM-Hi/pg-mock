package com.practicefintech.pgmock.admin.dto;

import java.util.List;
import java.util.Map;

public record ProcessedListResponse(List<ProcessedItem> items, Map<String, Integer> countByOrder) {
}
