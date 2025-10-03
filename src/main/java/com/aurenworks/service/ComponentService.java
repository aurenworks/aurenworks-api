package com.aurenworks.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aurenworks.api.dto.ComponentResponse;
import com.aurenworks.api.dto.UpdateComponentRequest;
import com.aurenworks.model.Component;
import com.aurenworks.model.Role;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComponentService {

  // In-memory storage for demo purposes
  // In production, this would be replaced with database persistence
  private final Map<String, ComponentData> components = new ConcurrentHashMap<>();

  public ComponentService() {
    // Initialize with some sample components for testing
    initializeSampleComponents();
  }

  public ComponentResponse getComponent(String id, Role userRole) {
    ComponentData componentData = components.get(id);
    if (componentData == null) {
      throw new IllegalArgumentException("Component not found: " + id);
    }

    // Check read permissions
    if (userRole == null || userRole == Role.VIEWER) {
      // VIEWER can read all components
      return toComponentResponse(componentData);
    }

    return toComponentResponse(componentData);
  }

  public ComponentResponse updateComponent(String id, UpdateComponentRequest request, String ifMatch, Role userRole) {
    ComponentData existingData = components.get(id);
    if (existingData == null) {
      throw new IllegalArgumentException("Component not found: " + id);
    }

    // Check write permissions
    if (userRole == null || userRole == Role.VIEWER) {
      throw new IllegalArgumentException("Insufficient permissions: VIEWER role cannot modify components");
    }

    // Check optimistic concurrency
    if (ifMatch != null && !ifMatch.equals(existingData.etag())) {
      throw new IllegalArgumentException("ETag mismatch: component was modified by another user");
    }

    // Validate component schema
    validateComponentSchema(request.fields());

    // Update component
    Instant now = Instant.now();
    Component updatedComponent = new Component(id, request.name(), request.description(), request.fields(),
        request.metadata());

    String newEtag = generateETag(updatedComponent, now);
    ComponentData updatedData = new ComponentData(updatedComponent, now, now, "system", newEtag);

    components.put(id, updatedData);

    // Log audit trail
    logAuditEvent("COMPONENT_UPDATED", id);

    return toComponentResponse(updatedData);
  }

  private void validateComponentSchema(java.util.List<Component.ComponentField> fields) {
    if (fields == null || fields.isEmpty()) {
      throw new IllegalArgumentException("Component must have at least one field");
    }

    for (Component.ComponentField field : fields) {
      if (field.name() == null || field.name().trim().isEmpty()) {
        throw new IllegalArgumentException("Field name cannot be empty");
      }
      if (field.type() == null || field.type().trim().isEmpty()) {
        throw new IllegalArgumentException("Field type cannot be empty for field: " + field.name());
      }
    }
  }

  private ComponentResponse toComponentResponse(ComponentData componentData) {
    Component component = componentData.component();
    return new ComponentResponse(component.id(), component.name(), component.description(), component.fields(),
        component.metadata(), componentData.createdAt(), componentData.updatedAt(), componentData.createdBy(),
        componentData.etag());
  }

  private String generateETag(Component component, Instant timestamp) {
    try {
      String content = component.id() + component.name() + component.description() + component.fields().toString()
          + component.metadata().toString() + timestamp.toString();
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hash = md.digest(content.getBytes());
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      // Fallback to simple hash
      return String.valueOf((component.id() + timestamp.toString()).hashCode());
    }
  }

  private void logAuditEvent(String action, String componentId) {
    // Simple audit logging - in production this would go to a proper audit system
    System.out.println(String.format("AUDIT: %s - componentId=%s, timestamp=%s", action, componentId, Instant.now()));
  }

  private void initializeSampleComponents() {
    // Sample component for testing
    Component.ComponentField nameField = new Component.ComponentField("name", "string", true, Map.of("maxLength", 100));
    Component.ComponentField ageField = new Component.ComponentField("age", "number", false,
        Map.of("min", 0, "max", 150));
    Component.ComponentField activeField = new Component.ComponentField("active", "boolean", false, Map.of());

    Component userComponent = new Component("user", "User", "User information component",
        java.util.List.of(nameField, ageField, activeField), Map.of("version", "1.0"));

    Instant now = Instant.now();
    String etag = generateETag(userComponent, now);
    ComponentData componentData = new ComponentData(userComponent, now, now, "system", etag);
    components.put("user", componentData);
  }

  private record ComponentData(Component component, Instant createdAt, Instant updatedAt, String createdBy,
      String etag) {
  }
}
