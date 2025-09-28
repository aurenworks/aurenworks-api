package com.aurenworks.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class RecordsResourceTest {

  @Test
  void testCreateRecord() {
    Map<String, Object> recordData = Map.of("name", "John Doe", "age", 30, "active", true);

    given().contentType("application/json")
        .body(Map.of("componentId", "user", "values", recordData, "metadata", Map.of("source", "test"))).when()
        .post("/records").then().statusCode(201).body("id", notNullValue()).body("componentId", is("user"))
        .body("values.name", is("John Doe")).body("values.age", is(30)).body("values.active", is(true))
        .body("createdAt", notNullValue()).body("updatedAt", notNullValue()).body("createdBy", is("system"));
  }

  @Test
  void testCreateRecordWithInvalidComponent() {
    given().contentType("application/json").body(Map.of("componentId", "nonexistent", "values", Map.of("name", "Test")))
        .when().post("/records").then().statusCode(400).body("error.code", is("VALIDATION_ERROR"))
        .body("error.message", is("Component not found: nonexistent"));
  }

  @Test
  void testCreateRecordWithMissingRequiredField() {
    given().contentType("application/json").body(Map.of("componentId", "user", "values", Map.of("age", 30) // Missing
                                                                                                           // required
                                                                                                           // 'name'
                                                                                                           // field
    )).when().post("/records").then().statusCode(400).body("error.code", is("VALIDATION_ERROR")).body("error.message",
        is("Required field missing: name"));
  }

  @Test
  void testCreateRecordWithInvalidFieldType() {
    given().contentType("application/json")
        .body(Map.of("componentId", "user", "values", Map.of("name", "John Doe", "age", "not-a-number" // Should be
                                                                                                       // number
        ))).when().post("/records").then().statusCode(400).body("error.code", is("VALIDATION_ERROR"))
        .body("error.message", is("Field age must be a number"));
  }

  @Test
  void testGetRecords() {
    // First create a record
    given().contentType("application/json")
        .body(Map.of("componentId", "user", "values", Map.of("name", "Test User", "age", 25))).when().post("/records");

    given().when().get("/records").then().statusCode(200).body("records", notNullValue())
        .body("pagination", notNullValue()).body("pagination.page", is(0)).body("pagination.size", is(20))
        .body("pagination.total", notNullValue());
  }

  @Test
  void testGetRecordsWithComponentIdFilter() {
    given().when().get("/records?componentId=user").then().statusCode(200).body("records", notNullValue())
        .body("pagination", notNullValue());
  }

  @Test
  void testGetRecordsWithPagination() {
    given().when().get("/records?page=0&size=5").then().statusCode(200).body("pagination.page", is(0))
        .body("pagination.size", is(5));
  }

  @Test
  void testGetRecordsWithInvalidPagination() {
    given().when().get("/records?page=-1").then().statusCode(400).body("error.code", is("VALIDATION_ERROR"))
        .body("error.message", is("Page must be non-negative"));
  }

  @Test
  void testGetRecordsWithInvalidSize() {
    given().when().get("/records?size=101").then().statusCode(400).body("error.code", is("VALIDATION_ERROR"))
        .body("error.message", is("Size must be between 1 and 100"));
  }

  @Test
  void testGetRecordById() {
    // First create a record
    String recordId = given().contentType("application/json")
        .body(Map.of("componentId", "user", "values", Map.of("name", "Test User", "age", 25))).when().post("/records")
        .then().statusCode(201).extract().path("id");

    given().when().get("/records/" + recordId).then().statusCode(200).body("id", is(recordId))
        .body("componentId", is("user")).body("values.name", is("Test User")).body("values.age", is(25));
  }

  @Test
  void testGetRecordByIdNotFound() {
    given().when().get("/records/nonexistent-id").then().statusCode(404).body("error.code", is("NOT_FOUND"))
        .body("error.message", is("Record not found: nonexistent-id"));
  }
}
