package com.github.aliandr13.zenmo.user;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User domain object for authentication (plain POJO).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppUser {

    private UUID id;
    private String email;
    private String passwordHash;
    private Instant createdAt;
}
