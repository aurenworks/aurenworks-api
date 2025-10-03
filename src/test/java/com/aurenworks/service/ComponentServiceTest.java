package com.aurenworks.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aurenworks.api.dto.ComponentResponse;
import com.aurenworks.api.dto.UpdateComponentRequest;
import com.aurenworks.model.Component;
import com.aurenworks.model.Role;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ComponentServiceTest {

  @Inject
  ComponentService componentService;

  @Test
  void testGetComponent_Success() {
    // Given
    String componentId = "user";
    Role userRole = Role.VIEWER;

    // When
    ComponentResponse response = componentService.getComponent(componentId, userRole);

    // Then
    assertNotNull(response);
    assertEquals(componentId, response.id());
    // Note: Component name might be "Updated User" if modified by other tests
    assertNotNull(response.name());
    assertNotNull(response.description());
    assertNotNull(response.fields());
    assertTrue(response.fields().size() >= 2); // At least 2 fields
    assertNotNull(response.etag());
  }

  @Test
  void testGetComponent_NotFound() {
    // Given
    String componentId = "nonexistent";
    Role userRole = Role.VIEWER;

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> componentService.getComponent(componentId, userRole));
    assertEquals("Component not found: nonexistent", exception.getMessage());
  }

  @Test
  void testUpdateComponent_Success() {
    // Given
    String componentId = "user";
    Role userRole = Role.BUILDER;
    UpdateComponentRequest request = new UpdateComponentRequest("Updated User", "Updated user information component",
        List.of(new Component.ComponentField("name", "string", true, Map.of("maxLength", 200)),
            new Component.ComponentField("email", "string", true, Map.of("maxLength", 255))),
        Map.of("version", "2.0"));

    // First get the current ETag
    ComponentResponse currentComponent = componentService.getComponent(componentId, Role.VIEWER);
    String currentETag = currentComponent.etag();

    // When
    ComponentResponse response = componentService.updateComponent(componentId, request, currentETag, userRole);

    // Then
    assertNotNull(response);
    assertEquals(componentId, response.id());
    assertEquals("Updated User", response.name());
    assertEquals("Updated user information component", response.description());
    assertEquals(2, response.fields().size());
    assertEquals("2.0", response.metadata().get("version"));
    assertNotNull(response.etag());
    assertNotEquals(currentETag, response.etag()); // ETag should change after update
  }

  @Test
  void testUpdateComponent_ETagMismatch() {
    // Given
    String componentId = "user";
    Role userRole = Role.BUILDER;
    UpdateComponentRequest request = new UpdateComponentRequest("Updated User", "Updated description",
        List.of(new Component.ComponentField("name", "string", true, Map.of())), Map.of());
    String wrongETag = "wrong-etag";

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> componentService.updateComponent(componentId, request, wrongETag, userRole));
    assertEquals("ETag mismatch: component was modified by another user", exception.getMessage());
  }

  @Test
  void testUpdateComponent_InsufficientPermissions() {
    // Given
    String componentId = "user";
    Role userRole = Role.VIEWER; // VIEWER cannot modify
    UpdateComponentRequest request = new UpdateComponentRequest("Updated User", "Updated description",
        List.of(new Component.ComponentField("name", "string", true, Map.of())), Map.of());

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> componentService.updateComponent(componentId, request, null, userRole));
    assertEquals("Insufficient permissions: VIEWER role cannot modify components", exception.getMessage());
  }

  @Test
  void testUpdateComponent_ValidationError_EmptyFields() {
    // Given
    String componentId = "user";
    Role userRole = Role.BUILDER;
    UpdateComponentRequest request = new UpdateComponentRequest("Updated User", "Updated description", List.of(), // Empty
                                                                                                                  // fields
                                                                                                                  // list
        Map.of());

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> componentService.updateComponent(componentId, request, null, userRole));
    assertEquals("Component must have at least one field", exception.getMessage());
  }

  @Test
  void testUpdateComponent_ValidationError_EmptyFieldName() {
    // Given
    String componentId = "user";
    Role userRole = Role.BUILDER;
    UpdateComponentRequest request = new UpdateComponentRequest("Updated User", "Updated description",
        List.of(new Component.ComponentField("", "string", true, Map.of())), // Empty field name
        Map.of());

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> componentService.updateComponent(componentId, request, null, userRole));
    assertEquals("Field name cannot be empty", exception.getMessage());
  }

  @Test
  void testUpdateComponent_ValidationError_EmptyFieldType() {
    // Given
    String componentId = "user";
    Role userRole = Role.BUILDER;
    UpdateComponentRequest request = new UpdateComponentRequest("Updated User", "Updated description",
        List.of(new Component.ComponentField("name", "", true, Map.of())), // Empty field type
        Map.of());

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> componentService.updateComponent(componentId, request, null, userRole));
    assertEquals("Field type cannot be empty for field: name", exception.getMessage());
  }
}
