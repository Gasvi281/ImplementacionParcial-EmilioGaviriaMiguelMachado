package com.example.implementacionparcial.common.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resourceName, Object id) {
        return new ResourceNotFoundException("%s with id '%s' was not found".formatted(resourceName, id));
    }
}
