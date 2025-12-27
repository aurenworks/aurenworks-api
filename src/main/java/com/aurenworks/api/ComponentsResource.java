package com.aurenworks.api;

import java.util.UUID;

import com.aurenworks.api.dto.ComponentResponse;
import com.aurenworks.api.dto.ComponentsListResponse;
import com.aurenworks.api.dto.CreateComponentRequest;
import com.aurenworks.api.dto.UpdateComponentRequest;
import com.aurenworks.model.Role;
import com.aurenworks.service.ComponentService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/projects/{projectId}/components")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Components", description = "Operations for managing components")
public class ComponentsResource {

  @Inject
  ComponentService componentService;

  @GET
  @Operation(summary = "List components", description = "Retrieves a paginated list of components for a project")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Components retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ComponentsListResponse.class))),
      @APIResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class)))})
  public Response listComponents(
      @Parameter(description = "Project ID", in = ParameterIn.PATH) @PathParam("projectId") String projectId,
      @Parameter(description = "Page number (0-based)") @QueryParam("page") @jakarta.ws.rs.DefaultValue("0") int page,
      @Parameter(description = "Page size (1-100)") @QueryParam("size") @jakarta.ws.rs.DefaultValue("20") int size,
      @Parameter(description = "Field to sort by") @QueryParam("sortBy") String sortBy,
      @Parameter(description = "Sort order (asc/desc)") @QueryParam("sortOrder") @jakarta.ws.rs.DefaultValue("asc") String sortOrder) {
    // Validate pagination parameters
    if (page < 0) {
      return Response.status(400).entity(ErrorEnvelope.of("VALIDATION_ERROR", "Page must be non-negative",
          java.util.Map.of("field", "page"), UUID.randomUUID().toString())).build();
    }

    if (size < 1 || size > 100) {
      return Response.status(400).entity(ErrorEnvelope.of("VALIDATION_ERROR", "Size must be between 1 and 100",
          java.util.Map.of("field", "size"), UUID.randomUUID().toString())).build();
    }

    try {
      // TODO: Get user role from security context
      Role userRole = Role.VIEWER; // Placeholder - should come from security context

      ComponentsListResponse response = componentService.listComponents(projectId, page, size, sortBy, sortOrder,
          userRole);
      return Response.ok(response).build();
    } catch (IllegalArgumentException e) {
      return Response.status(400).entity(ErrorEnvelope.of("VALIDATION_ERROR", e.getMessage(),
          java.util.Map.of("projectId", projectId), UUID.randomUUID().toString())).build();
    }
  }

  @POST
  @Operation(summary = "Create component", description = "Creates a new component in a project")
  @RequestBody(description = "Component creation request", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateComponentRequest.class)))
  @APIResponses({
      @APIResponse(responseCode = "201", description = "Component created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ComponentResponse.class)), headers = @org.eclipse.microprofile.openapi.annotations.headers.Header(name = "ETag", description = "Component version for optimistic concurrency")),
      @APIResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class))),
      @APIResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class)))})
  public Response createComponent(
      @Parameter(description = "Project ID", in = ParameterIn.PATH) @PathParam("projectId") String projectId,
      @RequestBody CreateComponentRequest request) {
    try {
      // TODO: Get user role from security context
      Role userRole = Role.BUILDER; // Placeholder - should come from security context

      ComponentResponse component = componentService.createComponent(projectId, request, userRole);
      return Response.status(201).entity(component).header("ETag", component.etag()).build();
    } catch (IllegalArgumentException e) {
      String errorCode = "VALIDATION_ERROR";
      int statusCode = 400;

      if (e.getMessage().contains("Insufficient permissions")) {
        errorCode = "FORBIDDEN";
        statusCode = 403;
      } else if (e.getMessage().contains("Component must have") || e.getMessage().contains("Field name cannot be")
          || e.getMessage().contains("Field type cannot be")) {
        errorCode = "VALIDATION_ERROR";
        statusCode = 400;
      }

      return Response.status(statusCode).entity(ErrorEnvelope.of(errorCode, e.getMessage(),
          java.util.Map.of("projectId", projectId), UUID.randomUUID().toString())).build();
    }
  }

  @GET
  @Path("/{componentId}")
  @Operation(summary = "Get component by ID", description = "Retrieves a specific component by its unique identifier with ETag for optimistic concurrency")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Component retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ComponentResponse.class)), headers = @org.eclipse.microprofile.openapi.annotations.headers.Header(name = "ETag", description = "Component version for optimistic concurrency")),
      @APIResponse(responseCode = "404", description = "Component not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class)))})
  public Response getComponent(
      @Parameter(description = "Project ID", in = ParameterIn.PATH) @PathParam("projectId") String projectId,
      @Parameter(description = "Component ID", in = ParameterIn.PATH) @PathParam("componentId") String componentId) {
    try {
      // TODO: Get user role from security context
      Role userRole = Role.VIEWER; // Placeholder - should come from security context

      ComponentResponse component = componentService.getComponent(projectId, componentId, userRole);
      return Response.ok(component).header("ETag", component.etag()).build();
    } catch (IllegalArgumentException e) {
      return Response.status(404)
          .entity(ErrorEnvelope.of("NOT_FOUND", e.getMessage(),
              java.util.Map.of("projectId", projectId, "componentId", componentId), UUID.randomUUID().toString()))
          .build();
    }
  }

  @PUT
  @Path("/{componentId}")
  @Operation(summary = "Update component", description = "Updates an existing component with optimistic concurrency control")
  @RequestBody(description = "Component update request", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateComponentRequest.class)))
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Component updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ComponentResponse.class)), headers = @org.eclipse.microprofile.openapi.annotations.headers.Header(name = "ETag", description = "Updated component version")),
      @APIResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class))),
      @APIResponse(responseCode = "404", description = "Component not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class))),
      @APIResponse(responseCode = "409", description = "ETag mismatch - component was modified by another user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class))),
      @APIResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class)))})
  public Response updateComponent(
      @Parameter(description = "Project ID", in = ParameterIn.PATH) @PathParam("projectId") String projectId,
      @Parameter(description = "Component ID", in = ParameterIn.PATH) @PathParam("componentId") String componentId,
      @RequestBody UpdateComponentRequest request,
      @Parameter(description = "ETag for optimistic concurrency", in = ParameterIn.HEADER) @HeaderParam("If-Match") String ifMatch) {
    try {
      // TODO: Get user role from security context
      Role userRole = Role.BUILDER; // Placeholder - should come from security context

      ComponentResponse component = componentService.updateComponent(projectId, componentId, request, ifMatch,
          userRole);
      return Response.ok(component).header("ETag", component.etag()).build();
    } catch (IllegalArgumentException e) {
      String errorCode = "NOT_FOUND";
      int statusCode = 404;

      if (e.getMessage().contains("ETag mismatch")) {
        errorCode = "CONFLICT";
        statusCode = 409;
      } else if (e.getMessage().contains("Insufficient permissions")) {
        errorCode = "FORBIDDEN";
        statusCode = 403;
      } else if (e.getMessage().contains("Component must have") || e.getMessage().contains("Field name cannot be")
          || e.getMessage().contains("Field type cannot be")) {
        errorCode = "VALIDATION_ERROR";
        statusCode = 400;
      }

      return Response.status(statusCode)
          .entity(ErrorEnvelope.of(errorCode, e.getMessage(),
              java.util.Map.of("projectId", projectId, "componentId", componentId), UUID.randomUUID().toString()))
          .build();
    }
  }

  @DELETE
  @Path("/{componentId}")
  @Operation(summary = "Delete component", description = "Deletes a component from a project")
  @APIResponses({@APIResponse(responseCode = "204", description = "Component deleted successfully"),
      @APIResponse(responseCode = "404", description = "Component not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class))),
      @APIResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class)))})
  public Response deleteComponent(
      @Parameter(description = "Project ID", in = ParameterIn.PATH) @PathParam("projectId") String projectId,
      @Parameter(description = "Component ID", in = ParameterIn.PATH) @PathParam("componentId") String componentId) {
    try {
      // TODO: Get user role from security context
      Role userRole = Role.BUILDER; // Placeholder - should come from security context

      componentService.deleteComponent(projectId, componentId, userRole);
      return Response.noContent().build();
    } catch (IllegalArgumentException e) {
      String errorCode = "NOT_FOUND";
      int statusCode = 404;

      if (e.getMessage().contains("Insufficient permissions")) {
        errorCode = "FORBIDDEN";
        statusCode = 403;
      }

      return Response.status(statusCode)
          .entity(ErrorEnvelope.of(errorCode, e.getMessage(),
              java.util.Map.of("projectId", projectId, "componentId", componentId), UUID.randomUUID().toString()))
          .build();
    }
  }
}
