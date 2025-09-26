package com.aurenworks.api;

import java.util.Map;

import com.aurenworks.api.dto.LoginRequest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

  @POST
  @Path("/login")
  public Response login(LoginRequest req) {
    // scaffold only
    throw new UnsupportedOperationException("login stub");
  }

  @POST
  @Path("/refresh")
  public Response refresh(Map<String, String> body) {
    // scaffold only
    throw new UnsupportedOperationException("refresh stub");
  }

  @POST
  @Path("/logout")
  @Consumes(MediaType.WILDCARD)
  public Response logout() {
    // for scaffolding we can return 204 to show endpoint exists
    return Response.noContent().build();
  }

  @GET
  @Path("/me")
  public Response me() {
    // scaffold only
    throw new UnsupportedOperationException("me stub");
  }
}
