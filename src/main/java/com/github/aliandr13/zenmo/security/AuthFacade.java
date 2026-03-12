package com.github.aliandr13.zenmo.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthFacade {
    public CurrentUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user in context");
        }
        if (auth.getPrincipal() instanceof UserPrincipal up) {
            return new CurrentUser(up.userId(), up.email());
        }
        // e.g. Basic auth sets principal to String (username); /me requires JWT UserPrincipal
        throw new AuthenticationCredentialsNotFoundException("Not authenticated with a valid token");
    }
}

