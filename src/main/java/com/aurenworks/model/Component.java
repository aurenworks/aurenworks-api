package com.aurenworks.model;

import java.util.List;
import java.util.Map;

public record Component(String id, String name, String description, List<ComponentField> fields,
    Map<String, Object> metadata) {
  public record ComponentField(String name, String type, boolean required, Map<String, Object> constraints) {
  }
}
