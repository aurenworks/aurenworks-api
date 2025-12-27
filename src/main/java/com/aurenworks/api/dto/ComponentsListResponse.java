package com.aurenworks.api.dto;

import java.util.List;

public record ComponentsListResponse(List<ComponentResponse> components, PaginationInfo pagination) {
  public record PaginationInfo(int page, int size, long total, int totalPages, boolean hasNext, boolean hasPrevious) {
  }
}
