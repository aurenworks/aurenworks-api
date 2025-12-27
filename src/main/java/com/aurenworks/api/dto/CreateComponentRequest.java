package com.aurenworks.api.dto;

import java.util.List;
import java.util.Map;

import com.aurenworks.model.Component;

public record CreateComponentRequest(String name, String description, List<Component.ComponentField> fields,
    Map<String, Object> metadata) {
}
