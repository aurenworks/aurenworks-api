package com.aurenworks.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AuthResourceTest {

  @Test
  void login_is_stubbed_501() {
    given().contentType("application/json").body("{\"username\":\"u\",\"password\":\"p\"}").when().post("/auth/login")
        .then().statusCode(501).body("error.code", is("NOT_IMPLEMENTED"));
  }

  @Test
  void me_is_stubbed_501() {
    given().when().get("/auth/me").then().statusCode(501).body("error.code", is("NOT_IMPLEMENTED"));
  }

  @Test
  void logout_returns_204() {
    given().when().post("/auth/logout").then().statusCode(204);
  }
}
