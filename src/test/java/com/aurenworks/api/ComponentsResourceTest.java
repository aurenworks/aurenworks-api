package com.aurenworks.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ComponentsResourceTest {

  private static final String PROJECT_ID = "default-project";

  @Test
  void testListComponents_Success() {
    given().when().get("/projects/" + PROJECT_ID + "/components").then().statusCode(200).contentType(ContentType.JSON)
        .body("components", notNullValue()).body("pagination", notNullValue()).body("pagination.page", equalTo(0))
        .body("pagination.size", equalTo(20)).body("pagination.total", greaterThanOrEqualTo(0));
  }

  @Test
  void testListComponents_WithPagination() {
    given().queryParam("page", 0).queryParam("size", 10).when().get("/projects/" + PROJECT_ID + "/components").then()
        .statusCode(200).contentType(ContentType.JSON).body("pagination.page", equalTo(0))
        .body("pagination.size", equalTo(10));
  }

  @Test
  void testListComponents_InvalidPage() {
    given().queryParam("page", -1).when().get("/projects/" + PROJECT_ID + "/components").then().statusCode(400)
        .contentType(ContentType.JSON).body("error.code", equalTo("VALIDATION_ERROR"));
  }

  @Test
  void testListComponents_InvalidSize() {
    given().queryParam("size", 101).when().get("/projects/" + PROJECT_ID + "/components").then().statusCode(400)
        .contentType(ContentType.JSON).body("error.code", equalTo("VALIDATION_ERROR"));
  }

  @Test
  void testListComponents_PageOutOfBounds() {
    // Request a page that's way out of bounds
    given().queryParam("page", 999).queryParam("size", 10).when().get("/projects/" + PROJECT_ID + "/components").then()
        .statusCode(400).contentType(ContentType.JSON).body("error.code", equalTo("VALIDATION_ERROR"))
        .body("error.message", containsString("out of bounds"));
  }

  @Test
  void testCreateComponent_Success() {
    String createRequest = """
        {
          "name": "Test Component",
          "description": "Test component description",
          "fields": [
            {
              "name": "testField",
              "type": "string",
              "required": true,
              "constraints": {"maxLength": 100}
            }
          ],
          "metadata": {"version": "1.0"}
        }
        """;

    given().contentType(ContentType.JSON).body(createRequest).when().post("/projects/" + PROJECT_ID + "/components")
        .then().statusCode(201).contentType(ContentType.JSON).body("name", equalTo("Test Component"))
        .body("description", equalTo("Test component description")).body("fields", hasSize(1))
        .body("metadata.version", equalTo("1.0")).header("ETag", notNullValue());
  }

  @Test
  void testCreateComponent_ValidationError_EmptyFields() {
    String createRequest = """
        {
          "name": "Test Component",
          "description": "Test component description",
          "fields": [],
          "metadata": {}
        }
        """;

    given().contentType(ContentType.JSON).body(createRequest).when().post("/projects/" + PROJECT_ID + "/components")
        .then().statusCode(400).contentType(ContentType.JSON).body("error.code", equalTo("VALIDATION_ERROR"))
        .body("error.message", equalTo("Component must have at least one field"));
  }

  @Test
  void testGetComponent_Success() {
    given().when().get("/projects/" + PROJECT_ID + "/components/user").then().statusCode(200)
        .contentType(ContentType.JSON).body("id", equalTo("user")).body("name", equalTo("User"))
        .body("description", equalTo("User information component")).body("fields", hasSize(3))
        .body("metadata.version", equalTo("1.0")).header("ETag", notNullValue());
  }

  @Test
  void testGetComponent_NotFound() {
    given().when().get("/projects/" + PROJECT_ID + "/components/nonexistent").then().statusCode(404)
        .contentType(ContentType.JSON).body("error.code", equalTo("NOT_FOUND"))
        .body("error.message", containsString("Component not found"))
        .body("error.details.componentId", equalTo("nonexistent"));
  }

  @Test
  void testUpdateComponent_Success() {
    // First get the current ETag
    String etag = given().when().get("/projects/" + PROJECT_ID + "/components/user").then().statusCode(200).extract()
        .header("ETag");

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

    given().contentType(ContentType.JSON).header("If-Match", etag).body(updateRequest).when()
        .put("/projects/" + PROJECT_ID + "/components/user").then().statusCode(200).contentType(ContentType.JSON)
        .body("id", equalTo("user")).body("name", equalTo("Updated User"))
        .body("description", equalTo("Updated user information component")).body("fields", hasSize(2))
        .body("metadata.version", equalTo("2.0")).header("ETag", notNullValue());
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
        .put("/projects/" + PROJECT_ID + "/components/user").then().statusCode(409).contentType(ContentType.JSON)
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

    given().contentType(ContentType.JSON).body(updateRequest).when().put("/projects/" + PROJECT_ID + "/components/user")
        .then().statusCode(400).contentType(ContentType.JSON).body("error.code", equalTo("VALIDATION_ERROR"))
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

    given().contentType(ContentType.JSON).body(updateRequest).when().put("/projects/" + PROJECT_ID + "/components/user")
        .then().statusCode(400).contentType(ContentType.JSON).body("error.code", equalTo("VALIDATION_ERROR"))
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

    given().contentType(ContentType.JSON).body(updateRequest).when().put("/projects/" + PROJECT_ID + "/components/user")
        .then().statusCode(400).contentType(ContentType.JSON).body("error.code", equalTo("VALIDATION_ERROR"))
        .body("error.message", equalTo("Field type cannot be empty for field: name"));
  }

  @Test
  void testDeleteComponent_Success() {
    // First create a component to delete
    String createRequest = """
        {
          "name": "To Delete",
          "description": "Component to be deleted",
          "fields": [
            {
              "name": "testField",
              "type": "string",
              "required": true,
              "constraints": {}
            }
          ],
          "metadata": {}
        }
        """;

    String componentId = given().contentType(ContentType.JSON).body(createRequest).when()
        .post("/projects/" + PROJECT_ID + "/components").then().statusCode(201).extract().path("id");

    // Delete the component
    given().when().delete("/projects/" + PROJECT_ID + "/components/" + componentId).then().statusCode(204);

    // Verify it's deleted
    given().when().get("/projects/" + PROJECT_ID + "/components/" + componentId).then().statusCode(404);
  }

  @Test
  void testDeleteComponent_NotFound() {
    given().when().delete("/projects/" + PROJECT_ID + "/components/nonexistent").then().statusCode(404)
        .contentType(ContentType.JSON).body("error.code", equalTo("NOT_FOUND"));
  }
}
