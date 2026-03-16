package com.github.aliandr13.zenmo.category;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category domain object (plain POJO).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    private UUID id;
    private UUID userId;
    private String name;
    private UUID parentId;
    private String color;
    private Instant createdAt;
}
