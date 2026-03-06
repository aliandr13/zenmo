package com.github.aliandr13.zenmo.security;

import java.util.UUID;

public record CurrentUser(UUID userId, String email) {
}

