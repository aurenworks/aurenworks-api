package com.aurenworks.api;

import java.util.UUID;

import com.aurenworks.api.dto.CreateRecordRequest;
import com.aurenworks.api.dto.RecordResponse;
import com.aurenworks.api.dto.RecordsListResponse;
import com.aurenworks.service.RecordService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/records")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Records", description = "Operations for managing records")
public class RecordsResource {

  @Inject
  RecordService recordService;

  @POST
  @Operation(summary = "Create a new record", description = "Creates a new record with the specified component schema and values")
  @RequestBody(description = "Record creation request", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateRecordRequest.class)))
  @APIResponses({
      @APIResponse(responseCode = "201", description = "Record created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RecordResponse.class))),
      @APIResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class)))})
  public Response createRecord(CreateRecordRequest request) {
    try {
      RecordResponse record = recordService.createRecord(request);
      return Response.status(201).entity(record).build();
    } catch (IllegalArgumentException e) {
      return Response.status(400).entity(ErrorEnvelope.of("VALIDATION_ERROR", e.getMessage(),
          java.util.Map.of("field", "componentId"), UUID.randomUUID().toString())).build();
    }
  }

  @GET
  @Operation(summary = "Get records", description = "Retrieves a paginated list of records with optional filtering and sorting")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Records retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RecordsListResponse.class))),
      @APIResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class)))})
  public Response getRecords(
      @Parameter(description = "Filter by component ID") @QueryParam("componentId") String componentId,
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

    RecordsListResponse response = recordService.getRecords(componentId, page, size, sortBy, sortOrder);
    return Response.ok(response).build();
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Get record by ID", description = "Retrieves a specific record by its unique identifier")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Record retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RecordResponse.class))),
      @APIResponse(responseCode = "404", description = "Record not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEnvelope.class)))})
  public Response getRecord(@Parameter(description = "Record ID") @PathParam("id") String id) {
    try {
      RecordResponse record = recordService.getRecord(id);
      return Response.ok(record).build();
    } catch (IllegalArgumentException e) {
      return Response.status(404).entity(
          ErrorEnvelope.of("NOT_FOUND", e.getMessage(), java.util.Map.of("recordId", id), UUID.randomUUID().toString()))
          .build();
    }
  }
}
