package com.aurenworks.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class HealthzResourceTest {

  @Test
  void healthz_returns_ok() {
    given().when().get("/healthz").then().statusCode(200).body("status", is("ok")).body("uptime",
        greaterThanOrEqualTo(0f));
  }
}
