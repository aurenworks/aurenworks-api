package com.aurenworks.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CorsResourceTest {

  private static final String ALLOWED_ORIGIN = "http://localhost:5173";
  private static final String BLOCKED_ORIGIN = "http://evil.com";

  @Test
  void cors_headers_present_for_allowed_origin() {
    given().header("Origin", ALLOWED_ORIGIN).when().get("/hello").then().statusCode(200)
        .header("Access-Control-Allow-Origin", is(ALLOWED_ORIGIN))
        .header("Access-Control-Allow-Credentials", is("true"));
  }

  @Test
  void cors_headers_not_present_for_blocked_origin() {
    // Quarkus rejects CORS requests from blocked origins with 403
    given().header("Origin", BLOCKED_ORIGIN).when().get("/hello").then().statusCode(403);
  }

  @Test
  void preflight_request_returns_correct_headers() {
    given().header("Origin", ALLOWED_ORIGIN).header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "Authorization,Content-Type").when().options("/hello").then()
        .statusCode(200).header("Access-Control-Allow-Origin", is(ALLOWED_ORIGIN))
        .header("Access-Control-Allow-Methods", containsString("POST"))
        .header("Access-Control-Allow-Headers", allOf(containsString("Authorization"), containsString("Content-Type")))
        .header("Access-Control-Allow-Credentials", is("true"));
  }

  @Test
  void preflight_request_rejected_for_blocked_origin() {
    // Quarkus rejects CORS preflight requests from blocked origins with 403
    given().header("Origin", BLOCKED_ORIGIN).header("Access-Control-Request-Method", "POST").when().options("/hello")
        .then().statusCode(403);
  }

  @Test
  void cors_allows_required_methods() {
    given().header("Origin", ALLOWED_ORIGIN).when().options("/hello").then().statusCode(200)
        .header("Access-Control-Allow-Methods", allOf(containsString("GET"), containsString("POST"),
            containsString("PUT"), containsString("DELETE"), containsString("OPTIONS")));
  }

  @Test
  void cors_allows_required_headers() {
    given().header("Origin", ALLOWED_ORIGIN)
        .header("Access-Control-Request-Headers", "Authorization,Content-Type,Accept").when().options("/hello").then()
        .statusCode(200).header("Access-Control-Allow-Headers",
            allOf(containsString("Authorization"), containsString("Content-Type"), containsString("Accept")));
  }

  @Test
  void actual_request_with_authorization_header_allowed() {
    // Test that CORS headers are present even when auth fails (CORS is checked before auth)
    // The endpoint may return 401, but CORS headers should still be present
    given().header("Origin", ALLOWED_ORIGIN).header("Authorization", "Bearer test-token").when().get("/hello").then()
        .header("Access-Control-Allow-Origin", is(ALLOWED_ORIGIN))
        .header("Access-Control-Allow-Credentials", is("true"));
  }
}
