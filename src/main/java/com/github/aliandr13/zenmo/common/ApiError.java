package com.github.aliandr13.zenmo.common;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API error response body.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}

