package com.example.implementacionparcial.common.security;

import com.example.implementacionparcial.common.exceptions.BadRequestException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUser {

    public UUID id() {
        return UUID.fromString(jwt().getSubject());
    }

    public String username() {
        return jwt().getClaimAsString("preferred_username");
    }

    private Jwt jwt() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new BadRequestException("The current request is not authenticated with a JWT");
        }
        return jwt;
    }
}
