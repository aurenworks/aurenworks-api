package com.aurenworks.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.aurenworks.api.dto.CreateRecordRequest;
import com.aurenworks.api.dto.RecordResponse;
import com.aurenworks.api.dto.RecordsListResponse;
import com.aurenworks.model.Component;
import com.aurenworks.model.Record;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecordService {

  // In-memory storage for demo purposes
  // In production, this would be replaced with database persistence
  private final Map<String, Record> records = new ConcurrentHashMap<>();
  private final Map<String, Component> components = new ConcurrentHashMap<>();

  public RecordService() {
    // Initialize with some sample components for testing
    initializeSampleComponents();
  }

  public RecordResponse createRecord(CreateRecordRequest request) {
    // Validate component exists
    Component component = components.get(request.componentId());
    if (component == null) {
      throw new IllegalArgumentException("Component not found: " + request.componentId());
    }

    // Validate record values against component schema
    validateRecordValues(request.values(), component);

    // Create new record
    String recordId = UUID.randomUUID().toString();
    Instant now = Instant.now();

    Record record = new Record(recordId, request.componentId(), request.values(), now, now, "system", // TODO: Get from
                                                                                                      // security
                                                                                                      // context
        request.metadata());

    records.put(recordId, record);

    // Log audit trail
    logAuditEvent("RECORD_CREATED", recordId, request.componentId());

    return toRecordResponse(record);
  }

  public RecordsListResponse getRecords(String componentId, int page, int size, String sortBy, String sortOrder) {
    List<Record> filteredRecords = records.values().stream()
        .filter(record -> componentId == null || record.componentId().equals(componentId)).collect(Collectors.toList());

    // Apply sorting
    if (sortBy != null && !sortBy.isEmpty()) {
      filteredRecords.sort((r1, r2) -> {
        Object v1 = r1.values().get(sortBy);
        Object v2 = r2.values().get(sortBy);

        if (v1 == null && v2 == null)
          return 0;
        if (v1 == null)
          return "asc".equals(sortOrder) ? 1 : -1;
        if (v2 == null)
          return "asc".equals(sortOrder) ? -1 : 1;

        int comparison = v1.toString().compareTo(v2.toString());
        return "desc".equals(sortOrder) ? -comparison : comparison;
      });
    } else {
      // Default sort by createdAt desc
      filteredRecords.sort((r1, r2) -> r2.createdAt().compareTo(r1.createdAt()));
    }

    // Apply pagination
    int total = filteredRecords.size();
    int totalPages = (int) Math.ceil((double) total / size);
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, total);

    List<Record> paginatedRecords = filteredRecords.subList(startIndex, endIndex);

    List<RecordResponse> recordResponses = paginatedRecords.stream().map(this::toRecordResponse)
        .collect(Collectors.toList());

    RecordsListResponse.PaginationInfo pagination = new RecordsListResponse.PaginationInfo(page, size, total,
        totalPages, page < totalPages - 1, page > 0);

    return new RecordsListResponse(recordResponses, pagination);
  }

  public RecordResponse getRecord(String id) {
    Record record = records.get(id);
    if (record == null) {
      throw new IllegalArgumentException("Record not found: " + id);
    }
    return toRecordResponse(record);
  }

  private void validateRecordValues(Map<String, Object> values, Component component) {
    for (Component.ComponentField field : component.fields()) {
      Object value = values.get(field.name());

      if (field.required() && value == null) {
        throw new IllegalArgumentException("Required field missing: " + field.name());
      }

      if (value != null) {
        validateFieldType(value, field);
      }
    }
  }

  private void validateFieldType(Object value, Component.ComponentField field) {
    String expectedType = field.type().toLowerCase();

    switch (expectedType) {
      case "string" :
        if (!(value instanceof String)) {
          throw new IllegalArgumentException("Field " + field.name() + " must be a string");
        }
        break;
      case "number" :
        if (!(value instanceof Number)) {
          throw new IllegalArgumentException("Field " + field.name() + " must be a number");
        }
        break;
      case "boolean" :
        if (!(value instanceof Boolean)) {
          throw new IllegalArgumentException("Field " + field.name() + " must be a boolean");
        }
        break;
      case "array" :
        if (!(value instanceof List)) {
          throw new IllegalArgumentException("Field " + field.name() + " must be an array");
        }
        break;
      case "object" :
        if (!(value instanceof Map)) {
          throw new IllegalArgumentException("Field " + field.name() + " must be an object");
        }
        break;
      default :
        // For unknown types, just log a warning but don't fail
        System.out.println("Warning: Unknown field type " + expectedType + " for field " + field.name());
    }
  }

  private RecordResponse toRecordResponse(Record record) {
    return new RecordResponse(record.id(), record.componentId(), record.values(), record.createdAt(),
        record.updatedAt(), record.createdBy(), record.metadata());
  }

  private void logAuditEvent(String action, String recordId, String componentId) {
    // Simple audit logging - in production this would go to a proper audit system
    System.out.println(String.format("AUDIT: %s - recordId=%s, componentId=%s, timestamp=%s", action, recordId,
        componentId, Instant.now()));
  }

  private void initializeSampleComponents() {
    // Sample component for testing
    Component.ComponentField nameField = new Component.ComponentField("name", "string", true, Map.of("maxLength", 100));
    Component.ComponentField ageField = new Component.ComponentField("age", "number", false,
        Map.of("min", 0, "max", 150));
    Component.ComponentField activeField = new Component.ComponentField("active", "boolean", false, Map.of());

    Component userComponent = new Component("user", "User", "User information component",
        List.of(nameField, ageField, activeField), Map.of("version", "1.0"));

    components.put("user", userComponent);
  }
}
