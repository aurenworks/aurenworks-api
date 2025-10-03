package com.aurenworks.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.aurenworks.model.Component;

public record ComponentResponse(String id, String name, String description, List<Component.ComponentField> fields,
    Map<String, Object> metadata, Instant createdAt, Instant updatedAt, String createdBy, String etag) {
}
