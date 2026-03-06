package com.github.aliandr13.zenmo.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthFacade {
    public CurrentUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user in context");
        }
        if (auth.getPrincipal() instanceof UserPrincipal up) {
            return new CurrentUser(up.userId(), up.email());
        }
        throw new IllegalStateException("Unexpected principal type: " + auth.getPrincipal().getClass().getName());
    }
}

