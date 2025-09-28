package com.aurenworks.api.dto;

import java.time.Instant;
import java.util.Map;

public record RecordResponse(String id, String componentId, Map<String, Object> values, Instant createdAt,
    Instant updatedAt, String createdBy, Map<String, Object> metadata) {
}
