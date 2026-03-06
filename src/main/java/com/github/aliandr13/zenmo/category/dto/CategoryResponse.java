package com.github.aliandr13.zenmo.category.dto;

import com.github.aliandr13.zenmo.category.Category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        UUID parentId,
        String color,
        Instant createdAt
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getParentId(),
                c.getColor(),
                c.getCreatedAt()
        );
    }
}
