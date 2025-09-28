package com.aurenworks.api.dto;

import java.util.Map;

public record CreateRecordRequest(String componentId, Map<String, Object> values, Map<String, Object> metadata) {
}
