package com.aurenworks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aurenworks.api.dto.CreateRecordRequest;
import com.aurenworks.api.dto.RecordResponse;
import com.aurenworks.api.dto.RecordsListResponse;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class RecordServiceTest {

  @Inject
  RecordService recordService;

  @BeforeEach
  void setUp() {
    // Service is already initialized with sample components
  }

  @Test
  void testCreateRecord() {
    CreateRecordRequest request = new CreateRecordRequest("user", Map.of("name", "John Doe", "age", 30, "active", true),
        Map.of("source", "test"));

    RecordResponse response = recordService.createRecord(request);

    assertNotNull(response.id());
    assertEquals("user", response.componentId());
    assertEquals("John Doe", response.values().get("name"));
    assertEquals(30, response.values().get("age"));
    assertEquals(true, response.values().get("active"));
    assertEquals("system", response.createdBy());
    assertNotNull(response.createdAt());
    assertNotNull(response.updatedAt());
  }

  @Test
  void testCreateRecordWithInvalidComponent() {
    CreateRecordRequest request = new CreateRecordRequest("nonexistent", Map.of("name", "Test"), Map.of());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      recordService.createRecord(request);
    });

    assertEquals("Component not found: nonexistent", exception.getMessage());
  }

  @Test
  void testCreateRecordWithMissingRequiredField() {
    CreateRecordRequest request = new CreateRecordRequest("user", Map.of("age", 30), // Missing required 'name' field
        Map.of());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      recordService.createRecord(request);
    });

    assertEquals("Required field missing: name", exception.getMessage());
  }

  @Test
  void testCreateRecordWithInvalidFieldType() {
    CreateRecordRequest request = new CreateRecordRequest("user", Map.of("name", "John Doe", "age", "not-a-number" // Should
                                                                                                                   // be
                                                                                                                   // number
    ), Map.of());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      recordService.createRecord(request);
    });

    assertEquals("Field age must be a number", exception.getMessage());
  }

  @Test
  void testGetRecords() {
    // Create a test record first
    CreateRecordRequest request = new CreateRecordRequest("user", Map.of("name", "Test User", "age", 25), Map.of());
    recordService.createRecord(request);

    RecordsListResponse response = recordService.getRecords(null, 0, 10, null, "asc");

    assertNotNull(response.records());
    assertTrue(response.records().size() >= 1);
    assertNotNull(response.pagination());
    assertEquals(0, response.pagination().page());
    assertEquals(10, response.pagination().size());
  }

  @Test
  void testGetRecordsWithComponentIdFilter() {
    // Create a test record
    CreateRecordRequest request = new CreateRecordRequest("user", Map.of("name", "Test User", "age", 25), Map.of());
    recordService.createRecord(request);

    RecordsListResponse response = recordService.getRecords("user", 0, 10, null, "asc");

    assertNotNull(response.records());
    assertTrue(response.records().size() >= 1);

    // All records should have the correct componentId
    for (RecordResponse record : response.records()) {
      assertEquals("user", record.componentId());
    }
  }

  @Test
  void testGetRecordById() {
    // Create a test record
    CreateRecordRequest request = new CreateRecordRequest("user", Map.of("name", "Test User", "age", 25), Map.of());
    RecordResponse createdRecord = recordService.createRecord(request);

    RecordResponse retrievedRecord = recordService.getRecord(createdRecord.id());

    assertEquals(createdRecord.id(), retrievedRecord.id());
    assertEquals("user", retrievedRecord.componentId());
    assertEquals("Test User", retrievedRecord.values().get("name"));
    assertEquals(25, retrievedRecord.values().get("age"));
  }

  @Test
  void testGetRecordByIdNotFound() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      recordService.getRecord("nonexistent-id");
    });

    assertEquals("Record not found: nonexistent-id", exception.getMessage());
  }
}
