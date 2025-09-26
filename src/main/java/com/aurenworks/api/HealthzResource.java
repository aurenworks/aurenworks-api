package com.aurenworks.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Path("/healthz")
public class HealthzResource {

  private static final Instant START = Instant.now();
  private static final String COMMIT_SHA = System.getenv("COMMIT_SHA");

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> health() {
    Map<String, Object> m = new HashMap<>();
    m.put("status", "ok");
    m.put("uptime", (double) (Instant.now().getEpochSecond() - START.getEpochSecond()));
    m.put("commitSha", COMMIT_SHA == null ? null : COMMIT_SHA);
    return m;
  }
}
