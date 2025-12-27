package com.aurenworks.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aurenworks.api.dto.ComponentResponse;
import com.aurenworks.api.dto.ComponentsListResponse;
import com.aurenworks.api.dto.CreateComponentRequest;
import com.aurenworks.api.dto.UpdateComponentRequest;
import com.aurenworks.model.Component;
import com.aurenworks.model.Role;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ComponentServiceTest {

  @Inject
  ComponentService componentService;

  private static final String PROJECT_ID = "default-project";

  @Test
  void testListComponents_Success() {
    // Given
    Role userRole = Role.VIEWER;

    // When
    ComponentsListResponse response = componentService.listComponents(PROJECT_ID, 0, 20, null, "asc", userRole);

    // Then
    assertNotNull(response);
    assertNotNull(response.components());
    assertNotNull(response.pagination());
    assertTrue(response.pagination().total() >= 0);
    assertEquals(0, response.pagination().page());
    assertEquals(20, response.pagination().size());
  }

  @Test
  void testListComponents_PageOutOfBounds() {
    // Given
    Role userRole = Role.VIEWER;

    // When & Then - requesting a page that's out of bounds should throw IllegalArgumentException
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> componentService.listComponents(PROJECT_ID, 999, 10, null, "asc", userRole));
    assertTrue(exception.getMessage().contains("out of bounds"));
  }

  @Test
  void testCreateComponent_Success() {
    // Given
    Role userRole = Role.BUILDER;
    CreateComponentRequest request = new CreateComponentRequest("Test Component", "Test description",
        List.of(new Component.ComponentField("testField", "string", true, Map.of("maxLength", 100))),
        Map.of("version", "1.0"));

    // When
    ComponentResponse response = componentService.createComponent(PROJECT_ID, request, userRole);

    // Then
    assertNotNull(response);
    assertNotNull(response.id());
    assertEquals("Test Component", response.name());
    assertEquals("Test description", response.description());
    assertEquals(1, response.fields().size());
    assertEquals("1.0", response.metadata().get("version"));
    assertNotNull(response.etag());
  }

  @Test
  void testGetComponent_Success() {
    // Given
    String componentId = "user";
    Role userRole = Role.VIEWER;

    // When
    ComponentResponse response = componentService.getComponent(PROJECT_ID, componentId, userRole);

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
        () -> componentService.getComponent(PROJECT_ID, componentId, userRole));
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
    ComponentResponse currentComponent = componentService.getComponent(PROJECT_ID, componentId, Role.VIEWER);
    String currentETag = currentComponent.etag();

    // When
    ComponentResponse response = componentService.updateComponent(PROJECT_ID, componentId, request, currentETag,
        userRole);

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
        () -> componentService.updateComponent(PROJECT_ID, componentId, request, wrongETag, userRole));
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
        () -> componentService.updateComponent(PROJECT_ID, componentId, request, null, userRole));
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
        () -> componentService.updateComponent(PROJECT_ID, componentId, request, null, userRole));
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
        () -> componentService.updateComponent(PROJECT_ID, componentId, request, null, userRole));
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
        () -> componentService.updateComponent(PROJECT_ID, componentId, request, null, userRole));
    assertEquals("Field type cannot be empty for field: name", exception.getMessage());
  }

  @Test
  void testDeleteComponent_Success() {
    // Given
    Role userRole = Role.BUILDER;
    CreateComponentRequest createRequest = new CreateComponentRequest("To Delete", "Component to be deleted",
        List.of(new Component.ComponentField("testField", "string", true, Map.of())), Map.of());

    // Create a component first
    ComponentResponse created = componentService.createComponent(PROJECT_ID, createRequest, userRole);
    String componentId = created.id();

    // When
    componentService.deleteComponent(PROJECT_ID, componentId, userRole);

    // Then - verify it's deleted
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> componentService.getComponent(PROJECT_ID, componentId, Role.VIEWER));
    assertEquals("Component not found: " + componentId, exception.getMessage());
  }

  @Test
  void testDeleteComponent_NotFound() {
    // Given
    String componentId = "nonexistent";
    Role userRole = Role.BUILDER;

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> componentService.deleteComponent(PROJECT_ID, componentId, userRole));
    assertEquals("Component not found: nonexistent", exception.getMessage());
  }

  @Test
  void testDeleteComponent_InsufficientPermissions() {
    // Given
    String componentId = "user";
    Role userRole = Role.VIEWER; // VIEWER cannot delete

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> componentService.deleteComponent(PROJECT_ID, componentId, userRole));
    assertEquals("Insufficient permissions: VIEWER role cannot delete components", exception.getMessage());
  }
}
