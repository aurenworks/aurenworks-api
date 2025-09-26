package com.aurenworks.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.UUID;

@Provider
@ApplicationScoped
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

  @Override
  public Response toResponse(Throwable ex) {
    // simple requestId for now; later pull from MDC/context
    String requestId = UUID.randomUUID().toString();
    int status = (ex instanceof UnsupportedOperationException) ? 501 : 500;
    String code = (status == 501) ? "NOT_IMPLEMENTED" : "INTERNAL";
    String message = (status == 501) ? "Not implemented" : "Internal server error";

    ErrorEnvelope body = ErrorEnvelope.of(code, message, Map.of("exception", ex.getClass().getSimpleName()), requestId);
    return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(body).build();
  }
}
