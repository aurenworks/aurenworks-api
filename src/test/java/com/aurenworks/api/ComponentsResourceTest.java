package com.aurenworks.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ComponentsResourceTest {

  @Test
  void testGetComponent_Success() {
    given().when().get("/components/user").then().statusCode(200).contentType(ContentType.JSON)
        .body("id", equalTo("user")).body("name", equalTo("User"))
        .body("description", equalTo("User information component")).body("fields", hasSize(3))
        .body("metadata.version", equalTo("1.0")).header("ETag", notNullValue());
  }

  @Test
  void testGetComponent_NotFound() {
    given().when().get("/components/nonexistent").then().statusCode(404).contentType(ContentType.JSON)
        .body("error.code", equalTo("NOT_FOUND")).body("error.message", containsString("Component not found"))
        .body("error.details.componentId", equalTo("nonexistent"));
  }

  @Test
  void testUpdateComponent_Success() {
    // First get the current ETag
    String etag = given().when().get("/components/user").then().statusCode(200).extract().header("ETag");

    // Update the component
    String updateRequest = """
        {
          "name": "Updated User",
          "description": "Updated user information component",
          "fields": [
            {
              "name": "name",
              "type": "string",
              "required": true,
              "constraints": {"maxLength": 200}
            },
            {
              "name": "email",
              "type": "string",
              "required": true,
              "constraints": {"maxLength": 255}
            }
          ],
          "metadata": {"version": "2.0"}
        }
        """;

    given().contentType(ContentType.JSON).header("If-Match", etag).body(updateRequest).when().put("/components/user")
        .then().statusCode(200).contentType(ContentType.JSON).body("id", equalTo("user"))
        .body("name", equalTo("Updated User")).body("description", equalTo("Updated user information component"))
        .body("fields", hasSize(2)).body("metadata.version", equalTo("2.0")).header("ETag", notNullValue());
  }

  @Test
  void testUpdateComponent_ETagMismatch() {
    String updateRequest = """
        {
          "name": "Updated User",
          "description": "Updated description",
          "fields": [
            {
              "name": "name",
              "type": "string",
              "required": true,
              "constraints": {}
            }
          ],
          "metadata": {}
        }
        """;

    given().contentType(ContentType.JSON).header("If-Match", "wrong-etag").body(updateRequest).when()
        .put("/components/user").then().statusCode(409).contentType(ContentType.JSON)
        .body("error.code", equalTo("CONFLICT")).body("error.message", containsString("ETag mismatch"));
  }

  @Test
  void testUpdateComponent_ValidationError_EmptyFields() {
    String updateRequest = """
        {
          "name": "Updated User",
          "description": "Updated description",
          "fields": [],
          "metadata": {}
        }
        """;

    given().contentType(ContentType.JSON).body(updateRequest).when().put("/components/user").then().statusCode(400)
        .contentType(ContentType.JSON).body("error.code", equalTo("VALIDATION_ERROR"))
        .body("error.message", equalTo("Component must have at least one field"));
  }

  @Test
  void testUpdateComponent_ValidationError_EmptyFieldName() {
    String updateRequest = """
        {
          "name": "Updated User",
          "description": "Updated description",
          "fields": [
            {
              "name": "",
              "type": "string",
              "required": true,
              "constraints": {}
            }
          ],
          "metadata": {}
        }
        """;

    given().contentType(ContentType.JSON).body(updateRequest).when().put("/components/user").then().statusCode(400)
        .contentType(ContentType.JSON).body("error.code", equalTo("VALIDATION_ERROR"))
        .body("error.message", equalTo("Field name cannot be empty"));
  }

  @Test
  void testUpdateComponent_ValidationError_EmptyFieldType() {
    String updateRequest = """
        {
          "name": "Updated User",
          "description": "Updated description",
          "fields": [
            {
              "name": "name",
              "type": "",
              "required": true,
              "constraints": {}
            }
          ],
          "metadata": {}
        }
        """;

    given().contentType(ContentType.JSON).body(updateRequest).when().put("/components/user").then().statusCode(400)
        .contentType(ContentType.JSON).body("error.code", equalTo("VALIDATION_ERROR"))
        .body("error.message", equalTo("Field type cannot be empty for field: name"));
  }
}
