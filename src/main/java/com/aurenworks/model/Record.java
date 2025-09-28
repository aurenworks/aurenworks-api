package com.aurenworks.model;

import java.time.Instant;
import java.util.Map;

public record Record(String id, String componentId, Map<String, Object> values, Instant createdAt, Instant updatedAt,
    String createdBy, Map<String, Object> metadata) {
}
