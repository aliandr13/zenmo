package com.github.aliandr13.zenmo.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CategoryRequest(
        @NotBlank @Size(max = 200) String name,
        UUID parentId,
        @Size(max = 50) String color
) {
}
