package com.aurenworks.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.aurenworks.api.dto.ComponentResponse;
import com.aurenworks.api.dto.ComponentsListResponse;
import com.aurenworks.api.dto.CreateComponentRequest;
import com.aurenworks.api.dto.UpdateComponentRequest;
import com.aurenworks.model.Component;
import com.aurenworks.model.Role;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComponentService {

  // In-memory storage for demo purposes
  // In production, this would be replaced with database persistence
  // Key format: projectId:componentId
  private final Map<String, ComponentData> components = new ConcurrentHashMap<>();

  public ComponentService() {
    // Initialize with some sample components for testing
    initializeSampleComponents();
  }

  public ComponentsListResponse listComponents(String projectId, int page, int size, String sortBy, String sortOrder,
      Role userRole) {
    // Filter by projectId
    List<ComponentData> filteredComponents = components.values().stream()
        .filter(data -> data.projectId().equals(projectId)).collect(Collectors.toList());

    // Apply sorting
    if (sortBy != null && !sortBy.isEmpty()) {
      filteredComponents.sort((c1, c2) -> {
        Component comp1 = c1.component();
        Component comp2 = c2.component();
        Object v1 = getSortValue(comp1, sortBy);
        Object v2 = getSortValue(comp2, sortBy);

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
      filteredComponents.sort((c1, c2) -> c2.createdAt().compareTo(c1.createdAt()));
    }

    // Apply pagination
    int total = filteredComponents.size();
    int totalPages = (int) Math.ceil((double) total / size);

    // Validate page is within bounds
    // If totalPages is 0, only page 0 is valid (empty result)
    // If totalPages > 0, page must be < totalPages
    if ((totalPages == 0 && page > 0) || (totalPages > 0 && page >= totalPages)) {
      throw new IllegalArgumentException(
          String.format("Page %d is out of bounds. Total pages: %d (0-based indexing)", page, totalPages));
    }

    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, total);

    List<ComponentData> paginatedComponents = filteredComponents.subList(startIndex, endIndex);

    List<ComponentResponse> componentResponses = paginatedComponents.stream().map(this::toComponentResponse)
        .collect(Collectors.toList());

    ComponentsListResponse.PaginationInfo pagination = new ComponentsListResponse.PaginationInfo(page, size, total,
        totalPages, page < totalPages - 1, page > 0);

    return new ComponentsListResponse(componentResponses, pagination);
  }

  public ComponentResponse createComponent(String projectId, CreateComponentRequest request, Role userRole) {
    // Check write permissions
    if (userRole == null || userRole == Role.VIEWER) {
      throw new IllegalArgumentException("Insufficient permissions: VIEWER role cannot create components");
    }

    // Validate component schema
    validateComponentSchema(request.fields());

    // Generate component ID
    String componentId = UUID.randomUUID().toString();
    String key = projectId + ":" + componentId;

    // Create component
    Instant now = Instant.now();
    Component component = new Component(componentId, request.name(), request.description(), request.fields(),
        request.metadata());

    String etag = generateETag(component, now);
    ComponentData componentData = new ComponentData(projectId, component, now, now, "system", etag);

    components.put(key, componentData);

    // Log audit trail
    logAuditEvent("COMPONENT_CREATED", componentId, projectId);

    return toComponentResponse(componentData);
  }

  public ComponentResponse getComponent(String projectId, String componentId, Role userRole) {
    String key = projectId + ":" + componentId;
    ComponentData componentData = components.get(key);
    if (componentData == null) {
      throw new IllegalArgumentException("Component not found: " + componentId);
    }

    // Check read permissions
    if (userRole == null || userRole == Role.VIEWER) {
      // VIEWER can read all components
      return toComponentResponse(componentData);
    }

    return toComponentResponse(componentData);
  }

  public ComponentResponse updateComponent(String projectId, String componentId, UpdateComponentRequest request,
      String ifMatch, Role userRole) {
    String key = projectId + ":" + componentId;
    ComponentData existingData = components.get(key);
    if (existingData == null) {
      throw new IllegalArgumentException("Component not found: " + componentId);
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
    Component updatedComponent = new Component(componentId, request.name(), request.description(), request.fields(),
        request.metadata());

    String newEtag = generateETag(updatedComponent, now);
    ComponentData updatedData = new ComponentData(projectId, updatedComponent, existingData.createdAt(), now,
        existingData.createdBy(), newEtag);

    components.put(key, updatedData);

    // Log audit trail
    logAuditEvent("COMPONENT_UPDATED", componentId, projectId);

    return toComponentResponse(updatedData);
  }

  public void deleteComponent(String projectId, String componentId, Role userRole) {
    String key = projectId + ":" + componentId;
    ComponentData componentData = components.get(key);
    if (componentData == null) {
      throw new IllegalArgumentException("Component not found: " + componentId);
    }

    // Check write permissions
    if (userRole == null || userRole == Role.VIEWER) {
      throw new IllegalArgumentException("Insufficient permissions: VIEWER role cannot delete components");
    }

    components.remove(key);

    // Log audit trail
    logAuditEvent("COMPONENT_DELETED", componentId, projectId);
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

  private Object getSortValue(Component component, String sortBy) {
    return switch (sortBy.toLowerCase()) {
      case "name" -> component.name();
      case "id" -> component.id();
      case "description" -> component.description();
      default -> null;
    };
  }

  private void logAuditEvent(String action, String componentId, String projectId) {
    // Simple audit logging - in production this would go to a proper audit system
    System.out.println(String.format("AUDIT: %s - componentId=%s, projectId=%s, timestamp=%s", action, componentId,
        projectId, Instant.now()));
  }

  private void initializeSampleComponents() {
    // Sample component for testing
    String projectId = "default-project";
    Component.ComponentField nameField = new Component.ComponentField("name", "string", true, Map.of("maxLength", 100));
    Component.ComponentField ageField = new Component.ComponentField("age", "number", false,
        Map.of("min", 0, "max", 150));
    Component.ComponentField activeField = new Component.ComponentField("active", "boolean", false, Map.of());

    Component userComponent = new Component("user", "User", "User information component",
        java.util.List.of(nameField, ageField, activeField), Map.of("version", "1.0"));

    Instant now = Instant.now();
    String etag = generateETag(userComponent, now);
    ComponentData componentData = new ComponentData(projectId, userComponent, now, now, "system", etag);
    components.put(projectId + ":user", componentData);
  }

  private record ComponentData(String projectId, Component component, Instant createdAt, Instant updatedAt,
      String createdBy, String etag) {
  }
}
