package com.deundeun.challenge.dto.response;

import com.deundeun.challenge.domain.Category;

public record CategoryResponse(Long id, String name) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}